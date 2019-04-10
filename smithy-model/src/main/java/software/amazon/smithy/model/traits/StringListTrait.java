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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Contains abstract functionality to build traits that contain a list
 * of strings.
 */
public abstract class StringListTrait extends AbstractTrait {
    private final List<String> values;

    /**
     * @param name The name of the trait being created.
     * @param values The string values of the trait.
     * @param sourceLocation Where the trait was defined.
     */
    public StringListTrait(String name, List<String> values, FromSourceLocation sourceLocation) {
        super(name, sourceLocation);
        this.values = Objects.requireNonNull(values, "values must not be null");
    }

    @Override
    protected final Node createNode() {
        return ArrayNode.fromStrings(values);
    }

    /**
     * Factory method to create a StringList provider.
     *
     * @param <T> Type of StringList trait to create.
     */
    @FunctionalInterface
    public interface StringListTraitConstructor<T extends StringListTrait> {
        /**
         * Wraps the constructor of a StringList trait.
         *
         * @param values Values to pass to the trait.
         * @param sourceLocation The location in which the trait was defined.
         * @return Returns the created StringList trait.
         */
        T create(List<String> values, FromSourceLocation sourceLocation);
    }

    /**
     * @return Gets the trait values.
     */
    public final List<String> getValues() {
        return values;
    }

    /**
     * Abstract builder to build a StringList trait.
     */
    public abstract static class Builder<TRAIT extends StringListTrait, BUILDER extends Builder>
            extends AbstractTraitBuilder<TRAIT, BUILDER> {

        private final List<String> values = new ArrayList<>();

        /**
         * Gets the values set in the builder.
         *
         * @return Returns the set values.
         */
        public List<String> getValues() {
            return Collections.unmodifiableList(values);
        }

        /**
         * Adds a value to the builder.
         *
         * @param value Value to add.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public BUILDER addValue(String value) {
            values.add(Objects.requireNonNull(value));
            return (BUILDER) this;
        }

        /**
         * Replaces all of the values in the builder with the given values.
         *
         * @param values Value to replace into the builder.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public BUILDER values(Collection<String> values) {
            clearValues();
            this.values.addAll(values);
            return (BUILDER) this;
        }

        /**
         * Removes a value from the builder.
         *
         * @param value Value to remove.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public BUILDER removeValue(String value) {
            this.values.remove(value);
            return (BUILDER) this;
        }

        /**
         * Clears all values out of the builder.
         *
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public BUILDER clearValues() {
            values.clear();
            return (BUILDER) this;
        }
    }

    /**
     * Trait provider that expects a list of string values.
     */
    public static class Provider<T extends StringListTrait> extends AbstractTrait.Provider {
        private final BiFunction<List<String>, SourceLocation, T> traitFactory;

        /**
         * @param name The name of the trait being created.
         * @param traitFactory The factory used to create the trait.
         */
        public Provider(String name, BiFunction<List<String>, SourceLocation, T> traitFactory) {
            super(name);
            this.traitFactory = traitFactory;
        }

        @Override
        public T createTrait(ShapeId id, Node value) {
            List<String> values = Node.loadArrayOfString(getTraitName(), value);
            return traitFactory.apply(values, value.getSourceLocation());
        }
    }
}
