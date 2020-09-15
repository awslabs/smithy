/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code removeTraitShapes} removes all trait definitions from a model,
 * but leaves if the trait definition contains any of the provided
 * {@code tags}.
 *
 * <p>This transformer will not remove prelude trait definitions.
 */
public final class RemoveTraitDefinitions extends ConfigurableProjectionTransformer<RemoveTraitDefinitions.Config> {

    /**
     * {@code removeTraitShapes} configuration settings.
     */
    public static final class Config {

        private Set<String> exportTagged = Collections.emptySet();

        /**
         * You can <em>export</em> trait definitions by applying specific tags
         * to the trait definition and adding the list of export tags as an
         * argument to the transformer.
         *
         * @param exportByTags Tags that cause trait definitions to be
         *                     exported.
         */
        public void setExportTagged(Set<String> exportByTags) {
            this.exportTagged = exportByTags;
        }

        /**
         * Gets the set of tags that are used to export trait definitions.
         *
         * @return the tags that are used to export trait definitions.
         */
        public Set<String> getExportTagged() {
            return exportTagged;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Model model = context.getModel();
        ModelTransformer transformer = context.getTransformer();
        Predicate<Shape> keepTraitDefsByTag = trait -> config.getExportTagged().stream().noneMatch(trait::hasTag);

        return transformer.getModelWithoutTraitShapes(model, keepTraitDefsByTag);
    }

    @Override
    public String getName() {
        return "removeTraitDefinitions";
    }
}
