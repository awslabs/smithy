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

package software.amazon.smithy.model.shapes;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.ToSmithyBuilder;

/**
 * Represents a {@code map} shape.
 */
public final class MapShape extends Shape implements ToSmithyBuilder<MapShape> {

    private final MemberShape key;
    private final MemberShape value;

    private MapShape(Builder builder) {
        super(builder, ShapeType.MAP, false);
        key = SmithyBuilder.requiredState("key", builder.key);
        value = SmithyBuilder.requiredState("value", builder.value);

        ShapeId expectedKey = getId().withMember("key");
        if (!key.getId().equals(expectedKey)) {
            throw new IllegalArgumentException(String.format(
                    "Expected the key member of `%s` to have an ID of `%s` but found `%s`",
                    getId(), expectedKey, key.getId()));
        }

        ShapeId expectedValue = getId().withMember("value");
        if (!value.getId().equals(expectedValue)) {
            throw new IllegalArgumentException(String.format(
                    "Expected the value member of `%s` to have an ID of `%s` but found `%s`",
                    getId(), expectedValue, value.getId()));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().from(this).key(key).value(value);
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.mapShape(this);
    }

    @Override
    public Optional<MapShape> asMapShape() {
        return Optional.of(this);
    }

    /**
     * Get the value member shape of the map.
     *
     * @return Returns the value member shape.
     */
    public MemberShape getValue() {
        return value;
    }

    /**
     * Get the key member shape of the map.
     *
     * @return Returns the key member shape.
     */
    public MemberShape getKey() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        } else {
            MapShape otherShape = (MapShape) other;
            return super.equals(otherShape)
                    && getKey().equals(otherShape.getKey())
                    && getValue().equals(((MapShape) other).getValue());
        }
    }

    /**
     * Builder used to create a {@link ListShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, MapShape> {

        private MemberShape key;
        private MemberShape value;

        @Override
        public MapShape build() {
            return new MapShape(this);
        }

        public Builder key(MemberShape member) {
            this.key = Objects.requireNonNull(member);
            return this;
        }

        public Builder value(MemberShape member) {
            this.value = Objects.requireNonNull(member);
            return this;
        }

        @Override
        public Builder addMember(MemberShape member) {
            if (member.getMemberName().equals("key")) {
                return key(member);
            } else if (member.getMemberName().equals("value")) {
                return value(member);
            } else {
                throw new IllegalStateException("Invalid member given to MapShape builder: " + member.getId());
            }
        }
    }
}
