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

package software.amazon.smithy.cli.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.SmithyBuildConfig;
import software.amazon.smithy.build.SmithyBuildResult;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Colors;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.Parser;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.ValidatedResult;

public final class BuildCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(BuildCommand.class.getName());

    @Override
    public String getName() {
        return "build";
    }

    @Override
    public String getSummary() {
        return "Builds Smithy models and creates plugin artifacts for each projection";
    }

    @Override
    public Parser getParser() {
        return Parser.builder()
                .repeatedParameter("--config", "-c",
                     "Path to smithy-build.json configuration. Defaults to 'smithy-build.json'.")
                .parameter("--output", "-o", "Where to write artifacts. Defaults to 'build/smithy'.")
                .option("--discover", "-d", "Enables model discovery, merging in models found inside of jars")
                .positional("<MODELS>", "Path to Smithy models or directories")
                .build();
    }

    @Override
    public void execute(Arguments arguments, ClassLoader classLoader) {
        List<String> config = arguments.repeatedParameter("--config", null);
        String output = arguments.parameter("--output", null);
        List<String> models = arguments.positionalArguments();

        LOGGER.info(String.format("Building Smithy models: [%s]", String.join(" ", models)));
        SmithyBuildConfig.Builder configBuilder = SmithyBuildConfig.builder();

        // Try to find a smithy-build.json file.
        if (config == null && Files.exists(Paths.get("smithy-build.json"))) {
            config = Collections.singletonList("smithy-build.json");
        }

        if (config != null) {
            LOGGER.info(String.format("Loading Smithy configs: [%s]", String.join(" ", config)));
            config.forEach(file -> configBuilder.load(Paths.get(file)));
        }

        if (output != null) {
            configBuilder.outputDirectory(output);
            try {
                Files.createDirectories(Paths.get(output));
                LOGGER.fine(String.format("Output directory set to: %s", output));
            } catch (IOException e) {
                throw new CliError("Unable to create Smithy output directory: " + e.getMessage());
            }
        }

        SmithyBuildConfig smithyBuildConfig = configBuilder.build();

        // Build the model and fail if there are errors.
        ValidatedResult<Model> sourceResult = buildModel(classLoader, models, arguments.has(SmithyCli.DISCOVER));
        Model model = sourceResult.unwrap();

        SmithyBuildResult smithyBuildResult = SmithyBuild.create(classLoader)
                .config(smithyBuildConfig)
                .model(model)
                .build();

        // Fail if any projections failed to build, but build all projections.
        if (smithyBuildResult.anyBroken()) {
            throw new CliError("One or more projections contained ERROR or unsuppressed DANGER events");
        }

        Colors.out(Colors.BRIGHT_GREEN, "Smithy build successfully generated the following artifacts");
        smithyBuildResult.allArtifacts().map(Path::toString).sorted().forEach(System.out::println);
    }

    private ValidatedResult<Model> buildModel(ClassLoader classLoader, List<String> models, boolean discoverModels) {
        ModelAssembler assembler = Model.assembler(classLoader);

        if (discoverModels) {
            LOGGER.fine("Enabling model discovery");
            assembler.discoverModels(classLoader);
        }

        models.forEach(assembler::addImport);
        ValidatedResult<Model> result = assembler.assemble();
        Validator.validate(result, true);
        return result;
    }
}
