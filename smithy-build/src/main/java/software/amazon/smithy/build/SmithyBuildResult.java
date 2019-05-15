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

package software.amazon.smithy.build;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Encapsulates the result of running SmithyBuild.
 */
public final class SmithyBuildResult {
    private final List<ProjectionResult> results;

    private SmithyBuildResult(Builder builder) {
        results = ListUtils.copyOf(builder.results);
    }

    /**
     * Creates a builder used to build SmithyBuildResult.
     *
     * @return Returns the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if any projected models contain error or danger events.
     *
     * @return Returns true if any are broken.
     */
    public boolean anyBroken() {
        return results.stream().anyMatch(ProjectionResult::isBroken);
    }

    /**
     * Gets all of the artifacts written during the build.
     *
     * @return Returns a stream of paths to artifacts.
     */
    public Stream<Path> allArtifacts() {
        return allManifests().flatMap(manifest -> manifest.getFiles().stream());
    }

    /**
     * Gets all of the manifests that were used in the build.
     *
     * @return Returns a stream of the manifests.
     */
    public Stream<FileManifest> allManifests() {
        return results.stream().flatMap(result -> result.getPluginManifests().values().stream());
    }

    /**
     * Gets a projection result by name.
     *
     * @param projectionName ProjectionConfig name to get.
     * @return Returns the optionally found result.
     */
    public Optional<ProjectionResult> getProjectionResult(String projectionName) {
        return results.stream().filter(result -> result.getProjectionName().equals(projectionName)).findFirst();
    }

    /**
     * Gets all of the projection results as an unmodifiable list.
     *
     * @return Returns the projection results.
     */
    public List<ProjectionResult> getProjectionResults() {
        return results;
    }

    /**
     * Gets all of the projection results as a map of projection name to
     * {@link ProjectionResult}.
     *
     * @return Returns the projection results as a map.
     */
    public Map<String, ProjectionResult> getProjectionResultsMap() {
        return results.stream().collect(Collectors.toMap(ProjectionResult::getProjectionName, Function.identity()));
    }

    /**
     * Gets the number of projection results in the map.
     *
     * @return Returns the number of results.
     */
    public int size() {
        return results.size();
    }

    /**
     * Checks if the results is empty.
     *
     * @return Returns true if there are no results.
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * Creates a SmithyBuildResult.
     */
    public static final class Builder implements SmithyBuilder<SmithyBuildResult> {
        private final List<ProjectionResult> results = new ArrayList<>();

        private Builder() {}

        @Override
        public SmithyBuildResult build() {
            return new SmithyBuildResult(this);
        }

        /**
         * Adds a projection result to the builder.
         *
         * @param result Result to add.
         * @return Returns the builder.
         */
        public Builder addProjectionResult(ProjectionResult result) {
            results.add(result);
            return this;
        }
    }
}
