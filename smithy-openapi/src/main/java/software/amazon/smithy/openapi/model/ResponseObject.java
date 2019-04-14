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

package software.amazon.smithy.openapi.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ResponseObject extends Component implements ToSmithyBuilder<ResponseObject> {
    private final String description;
    private final Map<String, Ref<ParameterObject>> headers;
    private final Map<String, MediaTypeObject> content;
    private final Map<String, Ref<LinkObject>> links;

    private ResponseObject(Builder builder) {
        super(builder);
        description = SmithyBuilder.requiredState("description", builder.description);
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
        content = Collections.unmodifiableMap(new LinkedHashMap<>(builder.content));
        links = Collections.unmodifiableMap(new LinkedHashMap<>(builder.links));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Ref<ParameterObject>> getHeaders() {
        return headers;
    }

    public Map<String, MediaTypeObject> getContent() {
        return content;
    }

    public Map<String, Ref<LinkObject>> getLinks() {
        return links;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember("description", description);

        if (!headers.isEmpty()) {
            builder.withMember("headers", headers.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!content.isEmpty()) {
            builder.withMember("content", content.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!links.isEmpty()) {
            builder.withMember("links", links.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .description(description)
                .headers(headers)
                .content(content)
                .links(links);
    }

    public static final class Builder extends Component.Builder<Builder, ResponseObject> {
        private String description;
        private final Map<String, Ref<ParameterObject>> headers = new LinkedHashMap<>();
        private final Map<String, MediaTypeObject> content = new LinkedHashMap<>();
        private final Map<String, Ref<LinkObject>> links = new LinkedHashMap<>();

        private Builder() {}

        @Override
        public ResponseObject build() {
            return new ResponseObject(this);
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder putHeader(String name, Ref<ParameterObject> header) {
            headers.put(name, header);
            return this;
        }

        public Builder headers(Map<String, Ref<ParameterObject>> headers) {
            this.headers.clear();
            this.headers.putAll(headers);
            return this;
        }

        public Builder putContent(String name, MediaTypeObject mediaTypeObject) {
            content.put(name, mediaTypeObject);
            return this;
        }

        public Builder content(Map<String, MediaTypeObject> content) {
            this.content.clear();
            this.content.putAll(content);
            return this;
        }

        public Builder putLink(String name, Ref<LinkObject> link) {
            links.put(name, link);
            return this;
        }

        public Builder putLink(String name, LinkObject link) {
            return putLink(name, Ref.local(link));
        }

        public Builder links(Map<String, Ref<LinkObject>> links) {
            this.links.clear();
            this.links.putAll(links);
            return this;
        }
    }
}
