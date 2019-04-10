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

package software.amazon.smithy.aws.traits.iam;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;

/**
 * Defines condition keys used in a service.
 */
public final class DefineConditionKeysTrait extends AbstractTrait implements ToSmithyBuilder<DefineConditionKeysTrait> {
    private static final String TRAIT = "aws.iam#defineConditionKeys";

    private final Map<String, ConditionKeyDefinition> conditionKeys;

    private DefineConditionKeysTrait(Builder builder) {
        super(TRAIT, builder.getSourceLocation());
        conditionKeys = Map.copyOf(builder.conditionKeys);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(TRAIT);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder();
            for (var entry : value.expectObjectNode().getMembers().entrySet()) {
                var definition = ConditionKeyDefinition.fromNode(entry.getValue().expectObjectNode());
                builder.putConditionKey(entry.getKey().getValue(), definition);
            }
            return builder.build();
        }
    }

    /**
     * Gets all condition keys of the service.
     *
     * @return Returns the immutable map of condition key name to definition.
     */
    public Map<String, ConditionKeyDefinition> getConditionKeys() {
        return conditionKeys;
    }

    /**
     * Get a specific condition key by name.
     *
     * @param name Name of the condition key to get.
     * @return Returns the optionall found condition key.
     */
    public Optional<ConditionKeyDefinition> getConditionKey(String name) {
        return Optional.ofNullable(conditionKeys.get(name));
    }

    @Override
    protected Node createNode() {
        return conditionKeys.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleImmutableEntry<>(
                        Node.from(entry.getKey()), entry.getValue().toNode()))
                .collect(ObjectNode.collect(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        conditionKeys.forEach(builder::putConditionKey);
        return builder;
    }

    public static final class Builder extends AbstractTraitBuilder<DefineConditionKeysTrait, Builder> {
        private final Map<String, ConditionKeyDefinition> conditionKeys = new HashMap<>();

        private Builder() {}

        @Override
        public DefineConditionKeysTrait build() {
            return new DefineConditionKeysTrait(this);
        }

        public Builder putConditionKey(String name, ConditionKeyDefinition definition) {
            conditionKeys.put(name, definition);
            return this;
        }

        public Builder removeConditionKey(String name) {
            conditionKeys.remove(name);
            return this;
        }
    }
}
