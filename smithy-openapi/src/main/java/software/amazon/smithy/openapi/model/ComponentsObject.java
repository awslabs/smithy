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

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public final class ComponentsObject extends Component implements ToSmithyBuilder<ComponentsObject> {
    private final Map<String, Schema> schemas = new LinkedHashMap<>();
    private final Map<String, ResponseObject> responses = new LinkedHashMap<>();
    private final Map<String, ParameterObject> parameters = new LinkedHashMap<>();
    private final Map<String, RequestBodyObject> requestBodies = new LinkedHashMap<>();
    private final Map<String, ParameterObject> headers = new LinkedHashMap<>();
    private final Map<String, SecurityScheme> securitySchemes = new LinkedHashMap<>();
    private final Map<String, LinkObject> links = new LinkedHashMap<>();
    private final Map<String, CallbackObject> callbacks = new LinkedHashMap<>();

    private ComponentsObject(Builder builder) {
        super(builder);
        schemas.putAll(builder.schemas);
        responses.putAll(builder.responses);
        parameters.putAll(builder.parameters);
        requestBodies.putAll(builder.requestBodies);
        headers.putAll(builder.headers);
        securitySchemes.putAll(builder.securitySchemes);
        links.putAll(builder.links);
        callbacks.putAll(builder.callbacks);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    public Map<String, ResponseObject> getResponses() {
        return responses;
    }

    public Map<String, ParameterObject> getParameters() {
        return parameters;
    }

    public Map<String, RequestBodyObject> getRequestBodies() {
        return requestBodies;
    }

    public Map<String, ParameterObject> getHeaders() {
        return headers;
    }

    public Map<String, SecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    public Map<String, LinkObject> getLinks() {
        return links;
    }

    public Map<String, CallbackObject> getCallbacks() {
        return callbacks;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        var builder = Node.objectNodeBuilder();

        if (!schemas.isEmpty()) {
            builder.withMember("schemas", schemas.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!responses.isEmpty()) {
            builder.withMember("responses", responses.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!parameters.isEmpty()) {
            builder.withMember("parameters", parameters.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!requestBodies.isEmpty()) {
            builder.withMember("requestBodies", requestBodies.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!headers.isEmpty()) {
            builder.withMember("headers", headers.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!securitySchemes.isEmpty()) {
            builder.withMember("securitySchemes", securitySchemes.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!links.isEmpty()) {
            builder.withMember("links", links.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (!callbacks.isEmpty()) {
            builder.withMember("callbacks", callbacks.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue)));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .schemas(schemas)
                .responses(responses)
                .parameters(parameters)
                .requestBodies(requestBodies)
                .headers(headers)
                .securitySchemes(securitySchemes)
                .links(links)
                .callbacks(callbacks)
                .extensions(getExtensions());
    }

    public static final class Builder extends Component.Builder<Builder, ComponentsObject> {
        private final Map<String, Schema> schemas = new LinkedHashMap<>();
        private final Map<String, ResponseObject> responses = new LinkedHashMap<>();
        private final Map<String, ParameterObject> parameters = new LinkedHashMap<>();
        private final Map<String, RequestBodyObject> requestBodies = new LinkedHashMap<>();
        private final Map<String, ParameterObject> headers = new LinkedHashMap<>();
        private final Map<String, SecurityScheme> securitySchemes = new LinkedHashMap<>();
        private final Map<String, LinkObject> links = new LinkedHashMap<>();
        private final Map<String, CallbackObject> callbacks = new LinkedHashMap<>();

        private Builder() {}

        @Override
        public ComponentsObject build() {
            return new ComponentsObject(this);
        }

        public Builder schemas(Map<String, Schema> schemas) {
            this.schemas.clear();
            this.schemas.putAll(schemas);
            return this;
        }

        public Builder putSchema(String name, Schema schema) {
            schemas.put(name, schema);
            return this;
        }

        public Builder removeSchema(String name) {
            schemas.remove(name);
            return this;
        }

        public Builder responses(Map<String, ResponseObject> responses) {
            this.responses.clear();
            this.responses.putAll(responses);
            return this;
        }

        public Builder putResponse(String name, ResponseObject response) {
            responses.put(name, response);
            return this;
        }

        public Builder parameters(Map<String, ParameterObject> parameters) {
            this.parameters.clear();
            this.parameters.putAll(parameters);
            return this;
        }

        public Builder putParameter(String name, ParameterObject parameter) {
            parameters.put(name, parameter);
            return this;
        }

        public Builder requestBodies(Map<String, RequestBodyObject> requestBodies) {
            this.requestBodies.clear();
            this.requestBodies.putAll(requestBodies);
            return this;
        }

        public Builder putRequestBodies(String name, RequestBodyObject requestBody) {
            requestBodies.put(name, requestBody);
            return this;
        }

        public Builder headers(Map<String, ParameterObject> headers) {
            this.headers.clear();
            this.headers.putAll(headers);
            return this;
        }

        public Builder putHeader(String name, ParameterObject header) {
            headers.put(name, header);
            return this;
        }

        public Builder securitySchemes(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes.clear();
            this.securitySchemes.putAll(securitySchemes);
            return this;
        }

        public Builder putSecurityScheme(String name, SecurityScheme securityScheme) {
            securitySchemes.put(name, securityScheme);
            return this;
        }

        public Builder links(Map<String, LinkObject> links) {
            this.links.clear();
            this.links.putAll(links);
            return this;
        }

        public Builder putLink(String name, LinkObject link) {
            links.put(name, link);
            return this;
        }

        public Builder callbacks(Map<String, CallbackObject> callbacks) {
            this.callbacks.clear();
            this.callbacks.putAll(callbacks);
            return this;
        }

        public Builder putCallbacks(String name, CallbackObject callback) {
            callbacks.put(name, callback);
            return this;
        }
    }
}
