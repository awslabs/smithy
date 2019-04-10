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

package software.amazon.smithy.model.traits;

import java.util.Objects;
import java.util.function.BiFunction;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Abstract trait class for traits that contain only a string value.
 */
public abstract class StringTrait extends AbstractTrait {
    private final String value;

    /**
     * @param name The name of the trait being created.
     * @param value The string value of the trait.
     * @param sourceLocation Where the trait was defined.
     */
    public StringTrait(String name, String value, FromSourceLocation sourceLocation) {
        super(name, sourceLocation);
        this.value = Objects.requireNonNull(value, "Trait values must not be null");
    }

    /**
     * @return Get the trait value.
     */
    public String getValue() {
        return value;
    }

    @Override
    protected final Node createNode() {
        return new StringNode(value, getSourceLocation());
    }

    /**
     * Trait provider that expects a string value.
     */
    public static class Provider<T extends StringTrait> extends AbstractTrait.Provider {
        private final BiFunction<String, SourceLocation, T> traitFactory;

        /**
         * @param name The name of the trait being created.
         * @param traitFactory The factory used to create the trait.
         */
        public Provider(String name, BiFunction<String, SourceLocation, T> traitFactory) {
            super(name);
            this.traitFactory = traitFactory;
        }

        @Override
        public T createTrait(ShapeId id, Node value) {
            return traitFactory.apply(value.expectStringNode().getValue(), value.getSourceLocation());
        }
    }
}
