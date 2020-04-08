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

package software.amazon.smithy.openapi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.jsonschema.JsonSchemaConfig;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol;
import software.amazon.smithy.openapi.fromsmithy.Smithy2OpenApiExtension;
import software.amazon.smithy.utils.ListUtils;

/**
 * "openapi" smithy-build plugin configuration settings.
 */
public class OpenApiConfig extends JsonSchemaConfig {

    /** The supported version of OpenAPI. */
    public static final String VERSION = "3.0.2";

    /** Specifies what to do when the httpPrefixHeaders trait is found in a model. */
    public enum HttpPrefixHeadersStrategy {
        /** The default setting that causes the build to fail. */
        FAIL,

        /** The header is omitted from the OpenAPI model and a warning is logged. */
        WARN
    }

    /** The JSON pointer to where OpenAPI schema components should be written. */
    private static final String SCHEMA_COMPONENTS_POINTER = "#/components/schemas";

    private ShapeId service;
    private ShapeId protocol;
    private boolean tags;
    private List<String> supportedTags = Collections.emptyList();
    private String defaultBlobFormat = "byte";
    private boolean keepUnusedComponents;
    private String jsonContentType = "application/json";
    private boolean forbidGreedyLabels;
    private HttpPrefixHeadersStrategy onHttpPrefixHeaders = HttpPrefixHeadersStrategy.FAIL;
    private boolean ignoreUnsupportedTraits;
    private Map<String, Node> substitutions = Collections.emptyMap();
    private Map<String, Node> jsonAdd = Collections.emptyMap();
    private List<String> externalDocs = ListUtils.of(
            "Homepage", "API Reference", "User Guide", "Developer Guide", "Reference", "Guide");

    public OpenApiConfig() {
        super();
        setDefinitionPointer(SCHEMA_COMPONENTS_POINTER);
    }

    public ShapeId getService() {
        return service;
    }

    /**
     * Sets the service shape ID to convert.
     *
     * <p>For example, smithy.example#Weather.
     *
     * @param service the Smithy service shape ID to convert.
     */
    public void setService(ShapeId service) {
        this.service = service;
    }

    public ShapeId getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol shape ID to use when converting Smithy to OpenAPI.
     *
     * <p>For example, aws.protocols#restJson1.
     *
     * <p>Smithy will try to match the provided protocol name with an
     * implementation of {@link OpenApiProtocol} registered with a
     * service provider implementation of {@link Smithy2OpenApiExtension}.
     *
     * <p>This property is required if a service supports multiple protocols.
     *
     * @param protocol The protocol shape ID to use.
     */
    public void setProtocol(ShapeId protocol) {
        this.protocol = protocol;
    }

    public String getDefaultBlobFormat() {
        return defaultBlobFormat;
    }

    /**
     * Sets the default OpenAPI format property used when converting blob
     * shapes in Smithy to strings in OpenAPI.
     *
     * <p>Defaults to "byte", meaning Base64 encoded.
     *
     * @param defaultBlobFormat Default blob OpenAPI format to use.
     */
    public void setDefaultBlobFormat(String defaultBlobFormat) {
        this.defaultBlobFormat = Objects.requireNonNull(defaultBlobFormat);
    }

    public boolean getTags() {
        return tags;
    }

    /**
     * Sets whether or not to include Smithy tags in the result as
     * OpenAPI tags.
     *
     * @param tags Set to true to enable tags.
     */
    public void setTags(boolean tags) {
        this.tags = tags;
    }

    public List<String> getSupportedTags() {
        return supportedTags;
    }

    /**
     * Limits the exported {@code tags} to a specific set of tags.
     *
     * <p>The value must be a list of strings. This property requires that
     * {@link #getTags()} is set to true in order to have an effect.
     *
     * @param supportedTags The set of tags to export.
     */
    public void setSupportedTags(List<String> supportedTags) {
        this.supportedTags = Objects.requireNonNull(supportedTags);
    }

    public boolean getKeepUnusedComponents() {
        return keepUnusedComponents;
    }

    /**
     * Set to true to prevent unused OpenAPI components from being
     * removed from the created specification.
     *
     * @param keepUnusedComponents Set to true to keep unused components.
     */
    public void setKeepUnusedComponents(boolean keepUnusedComponents) {
        this.keepUnusedComponents = keepUnusedComponents;
    }

    public String getJsonContentType() {
        return jsonContentType;
    }

    /**
     * Sets a custom media-type to associate with the JSON payload of
     * JSON-based protocols.
     *
     * @param jsonContentType Content-Type to use for JSON protocols by default.
     */
    public void setJsonContentType(String jsonContentType) {
        this.jsonContentType = Objects.requireNonNull(jsonContentType);
    }

    public boolean getForbidGreedyLabels() {
        return forbidGreedyLabels;
    }

    /**
     * Set to true to forbid greedy URI labels.
     *
     * <p>By default, greedy labels will appear as-is in the path generated
     * for an operation. For example, {@code "/{foo+}"}.
     *
     * @param forbidGreedyLabels Set to true to forbid greedy labels.
     */
    public void setForbidGreedyLabels(boolean forbidGreedyLabels) {
        this.forbidGreedyLabels = forbidGreedyLabels;
    }

    public HttpPrefixHeadersStrategy getOnHttpPrefixHeaders() {
        return onHttpPrefixHeaders;
    }

    /**
     * Specifies what to do when the {@code }httpPrefixHeaders} trait is
     * found in a model.
     *
     * <p>OpenAPI does not support httpPrefixHeaders. By default, the
     * conversion will fail when this trait is encountered, but this
     * behavior can be customized. By default, the conversion fails when
     * prefix headers are encountered.
     *
     * @param onHttpPrefixHeaders Strategy to use for prefix headers.
     */
    public void setOnHttpPrefixHeaders(HttpPrefixHeadersStrategy onHttpPrefixHeaders) {
        this.onHttpPrefixHeaders = Objects.requireNonNull(onHttpPrefixHeaders);
    }

    public boolean getIgnoreUnsupportedTraits() {
        return ignoreUnsupportedTraits;
    }

    /**
     * Set to true to emit warnings rather than failing when unsupported
     * traits like {@code endpoint} and {@code hostLabel} are encountered.
     *
     * @param ignoreUnsupportedTraits True to ignore unsupported traits.
     */
    public void setIgnoreUnsupportedTraits(boolean ignoreUnsupportedTraits) {
        this.ignoreUnsupportedTraits = ignoreUnsupportedTraits;
    }

    public Map<String, Node> getSubstitutions() {
        return substitutions;
    }

    /**
     * Defines a map of strings to any JSON value to find and replace in the
     * generated OpenAPI model.
     *
     * <p>String values are replaced if the string in its entirety matches one
     * of the keys provided in the {@code substitutions} map. The
     * corresponding value is then substituted for the string; this could
     * even result in a string changing into an object, array, etc.
     *
     * @param substitutions Map of substitutions.
     */
    public void setSubstitutions(Map<String, Node> substitutions) {
        this.substitutions = Objects.requireNonNull(substitutions);
    }

    public Map<String, Node> getJsonAdd() {
        return jsonAdd;
    }

    /**
     * Adds or replaces the JSON value in the generated OpenAPI document
     * at the given JSON pointer locations with a different JSON value.
     *
     * <p>The value must be a map where each key is a valid JSON pointer
     * string as defined in RFC 6901. Each value in the map is the JSON
     * value to add or replace at the given target.
     *
     * <p>Values are added using similar semantics of the "add" operation
     * of JSON Patch, as specified in RFC 6902, with the exception that
     * adding properties to an undefined object will create nested
     * objects in the result as needed.
     *
     * @param jsonAdd Map of JSON path to values to patch in.
     */
    public void setJsonAdd(Map<String, Node> jsonAdd) {
        this.jsonAdd = Objects.requireNonNull(jsonAdd);
    }

    public List<String> getExternalDocs() {
        return externalDocs;
    }

    /**
     * Limits the source of converted "externalDocs" fields to the specified
     * priority ordered list of names in an externalDocumentation trait.
     *
     * <p>This list is case insensitive. By default, this is a list of the
     * following values: "Homepage", "API Reference", "User Guide",
     * "Developer Guide", "Reference", and "Guide".
     *
     * @param externalDocs External docs to look for and convert, in order.
     */
    public void setExternalDocs(List<String> externalDocs) {
        this.externalDocs = Objects.requireNonNull(externalDocs);
    }
}
