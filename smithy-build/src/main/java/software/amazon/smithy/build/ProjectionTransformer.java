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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import software.amazon.smithy.model.Model;

/**
 * Creates a model transformer by name.
 */
public interface ProjectionTransformer {
    /**
     * Gets the name of the transformer.
     *
     * @return Returns the name (e.g., "traits").
     */
    String getName();

    /**
     * Transforms the given model using the provided {@link TransformContext}.
     *
     * @param context Transformation context.
     * @return Returns the created transformer.
     * @throws IllegalArgumentException if the arguments are invalid.
     */
    Model transform(TransformContext context);

    /**
     * Creates a {@code ProjectionTransformer} factory function using SPI
     * and the current thread's context class loader.
     *
     * @return Returns the created factory function.
     * @see Thread#getContextClassLoader()
     */
    static Function<String, Optional<ProjectionTransformer>> createServiceFactory() {
        return createServiceFactory(ServiceLoader.load(ProjectionTransformer.class));
    }

    /**
     * Creates a {@code ProjectionTransformer} factory function using
     * the given transformers.
     *
     * @param transformers Transformers used to build a factory function.
     * @return Returns the created factory.
     */
    static Function<String, Optional<ProjectionTransformer>> createServiceFactory(
            Iterable<ProjectionTransformer> transformers
    ) {
        Map<String, ProjectionTransformer> map = new HashMap<>();
        for (ProjectionTransformer transformer : transformers) {
            map.put(transformer.getName(), transformer);
        }
        return name -> Optional.ofNullable(map.get(name));
    }

    /**
     * Creates a {@code ProjectionTransformer} factory function using SPI.
     *
     * @param classLoader Class loader to use to discover classes.
     * @return Returns the created factory function.
     */
    static Function<String, Optional<ProjectionTransformer>> createServiceFactory(ClassLoader classLoader) {
        return createServiceFactory(ServiceLoader.load(ProjectionTransformer.class, classLoader));
    }
}
