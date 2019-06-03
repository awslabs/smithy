/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.build.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.build.SourcesConflictException;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelDiscovery;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Copies model sources into the sources plugin.
 *
 * <p>Model sources are model components that were defined in one of the
 * directories marked as "sources" in the original model or a model
 * component that is found in the updated model but not the original model.
 *
 * <p>When a JAR is provided as a source, the models contained within the
 * JAR are extracted into the sources directory. The JAR is not copied into
 * the sources directory.
 *
 * <p>This plugin can only run if an original model is provided.
 */
public final class SourcesPlugin implements SmithyBuildPlugin {
    private static final String NAME = "sources";
    private static final String PROJECTED_FILENAME = "model.json";
    private static final Logger LOGGER = Logger.getLogger(SourcesPlugin.class.getName());

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(PluginContext context) {
        if (!context.getOriginalModel().isPresent()) {
            LOGGER.warning("No original model was provided, so the sources plugin cannot run");
            return;
        }

        List<String> names;
        String projectionName = context.getProjectionName();

        if (projectionName.equals("source")) {
            // Copy sources directly.
            names = copySources(context);
            LOGGER.fine(() -> String.format("Copying source files to the sources of %s: %s",
                    projectionName, names));
        } else {
            // Extract source shapes, traits, and metadata from the projected model.
            LOGGER.fine(() -> String.format(
                    "Creating the `%s` sources by extracting relevant components from the original model",
                    projectionName));
            names = ListUtils.of(PROJECTED_FILENAME);
            projectSources(context);
        }

        if (names.isEmpty()) {
            LOGGER.info(String.format("Skipping `%s` manifest because no Smithy sources found", projectionName));
        } else {
            LOGGER.fine(() -> String.format("Writing `%s` manifest", projectionName));
            context.getFileManifest().writeFile("manifest", String.join("\n", names) + "\n");
        }
    }

    private static List<String> copySources(PluginContext context) {
        List<String> names = new ArrayList<>();
        context.getSources().forEach(path -> copyDirectory(names, context.getFileManifest(), path, path));
        return names;
    }

    private static void copyDirectory(List<String> names, FileManifest manifest, Path root, Path current) {
        try {
            if (Files.isDirectory(current)) {
                Files.list(current)
                        .filter(p -> !p.equals(current))
                        .filter(p -> Files.isDirectory(p) || Files.isRegularFile(p))
                        .forEach(p -> copyDirectory(names, manifest, root, p));
            } else if (Files.isRegularFile(current)) {
                if (current.toString().endsWith(".jar")) {
                    // Account for just a simple file vs recursing into directories.
                    String jarRoot = root.equals(current)
                            ? "" : (root.relativize(current).toString() + File.separator);
                    // Copy Smithy models out of the JAR.
                    copyModelsFromJar(names, manifest, jarRoot, current);
                } else {
                    // Account for just a simple file vs recursing into directories.
                    Path target = root.equals(current) ? current.getFileName() : root.relativize(current);
                    copyFile(names, manifest, target, IoUtils.readUtf8File(current));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading the contents of " + current + ": " + e.getMessage(), e);
        }
    }

    private static void copyFile(List<String> names, FileManifest manifest, Path target, String contents) {
        // Path#getFileName might return null.
        if (target == null) {
            return;
        }

        if (manifest.hasFile(target)) {
            throw new SourcesConflictException(
                    "Source file conflict found when attempting to add `" + target + "` to the `sources` plugin "
                    + "output. All sources must have unique filenames relative to the directories marked as a "
                    + "'source'. The files and directories that make up sources are flattened into a single "
                    + "directory and conflicts are not allowed. The manifest has the following files: "
                    + ValidationUtils.tickedList(manifest.getFiles()));
        }

        manifest.writeFile(target, contents);
        names.add(target.toString());
    }

    private static void projectSources(PluginContext context) {
        Model originalModel = context.getOriginalModel().get();
        Model updatedModel = context.getModel();
        Set<Path> sources = context.getSources();
        ShapeIndex oldIndex = context.getOriginalModel().get().getShapeIndex();

        // New shapes, trait definitions, and metadata are considered "sources".
        ObjectNode serialized = ModelSerializer
                .builder()
                .shapeFilter(shape -> isSource(sources, oldIndex.getShape(shape.getId()).orElse(null)))
                .traitDefinitionFilter(def -> isSource(
                        sources, originalModel.getTraitDefinition(def.getFullyQualifiedName()).orElse(null)))
                .metadataFilter(key -> isSource(sources, originalModel.getMetadata().get(key)))
                .build()
                .serialize(updatedModel);

        context.getFileManifest().writeJson(PROJECTED_FILENAME, serialized);
    }

    private static boolean isSource(Set<Path> sources, FromSourceLocation sourceLocation) {
        if (sourceLocation == null) {
            return true;
        }

        Path location = Paths.get(sourceLocation.getSourceLocation().getFilename());
        for (Path path : sources) {
            if (location.startsWith(path)) {
                return true;
            }
        }

        return false;
    }

    private static void copyModelsFromJar(List<String> names, FileManifest manifest, String jarRoot, Path jarPath)
            throws IOException {
        LOGGER.fine(() -> "Copying models from JAR " + jarPath);
        URL manifestUrl = ModelDiscovery.createSmithyJarManifestUrl(jarPath.toString());

        for (URL model : ModelDiscovery.findModels(manifestUrl)) {
            String name = ModelDiscovery.getSmithyModelPathFromJarUrl(model);
            Path target = Paths.get(jarRoot + name);
            LOGGER.finer(() -> "Copying " + name + " from JAR to " + target);
            try (InputStream is = model.openStream()) {
                copyFile(names, manifest, target, IoUtils.toUtf8String(is));
            }

        }
    }
}
