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

import java.util.Locale;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An HTTP-specific authentication scheme that sends an arbitrary
 * API key in a header, cookie, or query string parameter.
 */
public final class HttpApiKeyAuthTrait extends BooleanTrait implements ToSmithyBuilder<HttpApiKeyAuthTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#httpApiKeyAuth");

    private final String name;
    private final Location in;

    private HttpApiKeyAuthTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        name = SmithyBuilder.requiredState("name", builder.name);
        in = SmithyBuilder.requiredState("in", builder.in);
    }

    public String getName() {
        return name;
    }

    public Location getIn() {
        return in;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .name(getName())
                .in(getIn());
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum Location {
        HEADER, COOKIE, QUERY;

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value.getSourceLocation());
            builder.name(objectNode.expectStringMember("name").getValue());
            builder.in(Location.valueOf(objectNode.expectStringMember("in").expectOneOf("header", "cookie", "query")));
            return builder.build();
        }
    }

    public static final class Builder extends AbstractTraitBuilder<HttpApiKeyAuthTrait, Builder> {
        private String name;
        private Location in;

        private Builder() {}

        @Override
        public HttpApiKeyAuthTrait build() {
            return new HttpApiKeyAuthTrait(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder in(Location in) {
            this.in = in;
            return this;
        }
    }
}
