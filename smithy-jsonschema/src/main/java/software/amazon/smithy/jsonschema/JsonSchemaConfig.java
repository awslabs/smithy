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

package software.amazon.smithy.jsonschema;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * JSON Schema configuration options.
 */
public class JsonSchemaConfig {

    /**
     * Configures how Smithy union shapes are converted to JSON Schema.
     */
    public enum UnionStrategy {
        /**
         * Converts to a schema that uses "oneOf".
         *
         * <p>This is the default setting used if not configured.
         */
        ONE_OF("oneOf"),

        /**
         * Converts to an empty object "{}".
         */
        OBJECT("object"),

        /**
         * Converts to an object with properties just like a structure.
         */
        STRUCTURE("structure");

        private String stringValue;

        UnionStrategy(String stringValue) {
            this.stringValue = stringValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    private boolean alphanumericOnlyRefs;
    private boolean useJsonName;
    private TimestampFormatTrait.Format defaultTimestampFormat = TimestampFormatTrait.Format.DATE_TIME;
    private UnionStrategy unionStrategy = UnionStrategy.ONE_OF;
    private String definitionPointer = "#/definitions";
    private ObjectNode schemaDocumentExtensions = Node.objectNode();
    private ObjectNode extensions = Node.objectNode();
    private Set<String> disableFeatures = new HashSet<>();
    private final ConcurrentHashMap<Class, Object> extensionCache = new ConcurrentHashMap<>();
    private final NodeMapper nodeMapper = new NodeMapper();

    public JsonSchemaConfig() {
        nodeMapper.setWhenMissingSetter(NodeMapper.WhenMissing.INGORE);
    }

    public boolean getAlphanumericOnlyRefs() {
        return alphanumericOnlyRefs;
    }

    /**
     * Creates shape name pointers that strip out non-alphanumeric characters.
     *
     * <p>This is necessary for compatibility with some vendors like
     * Amazon API Gateway that only allow alphanumeric shape names.
     *
     * @param alphanumericOnlyRefs Set to true to strip non-alphanumeric characters.
     */
    public void setAlphanumericOnlyRefs(boolean alphanumericOnlyRefs) {
        this.alphanumericOnlyRefs = alphanumericOnlyRefs;
    }

    public boolean getUseJsonName() {
        return useJsonName;
    }

    /**
     * Uses the value of the jsonName trait when creating JSON schema
     * properties for structure and union shapes.
     *
     * <p>This property has no effect if a {@link PropertyNamingStrategy} is
     * manually configured on a {@link JsonSchemaConverter}.
     *
     * @param useJsonName Set to true to use jsonName traits when creating refs..
     */
    public void setUseJsonName(boolean useJsonName) {
        this.useJsonName = useJsonName;
    }

    public TimestampFormatTrait.Format getDefaultTimestampFormat() {
        return defaultTimestampFormat;
    }

    /**
     * Sets the assumed timestampFormat trait for timestamps with no
     * timestampFormat trait. The provided value is expected to be a string.
     *
     * <p>Defaults to "date-time" if not set. Can be set to "date-time",
     * "epoch-seconds", or "http-date".
     *
     * @param defaultTimestampFormat The default timestamp format to use when none is set.
     */
    public void setDefaultTimestampFormat(TimestampFormatTrait.Format defaultTimestampFormat) {
        this.defaultTimestampFormat = defaultTimestampFormat;
    }

    public UnionStrategy getUnionStrategy() {
        return unionStrategy;
    }

    /**
     * Configures how Smithy union shapes are converted to JSON Schema.
     *
     * @param unionStrategy The union strategy to use.
     */
    public void setUnionStrategy(UnionStrategy unionStrategy) {
        this.unionStrategy = unionStrategy;
    }

    public String getDefinitionPointer() {
        return definitionPointer;
    }

    /**
     * Configures location of where definitions are written using JSON Pointer.
     *
     * <p>The provided String value MUST start with "#/" and can use nested "/"
     * characters to place schemas in nested object properties. The provided
     * JSON Pointer does not support escaping.
     *
     * <p>Defaults to "#/definitions" if no value is specified. OpenAPI
     * artifacts will want to use "#/components/schemas".
     *
     * @param definitionPointer The root definition pointer to use.
     */
    public void setDefinitionPointer(String definitionPointer) {
        this.definitionPointer = Objects.requireNonNull(definitionPointer);
    }

    public ObjectNode getSchemaDocumentExtensions() {
        return schemaDocumentExtensions;
    }

    /**
     * Adds custom key-value pairs to the JSON Schema document generated from
     * a Smithy model.
     *
     * @param schemaDocumentExtensions Custom extensions to merge into the created schema.
     */
    public void setSchemaDocumentExtensions(ObjectNode schemaDocumentExtensions) {
        this.schemaDocumentExtensions = Objects.requireNonNull(schemaDocumentExtensions);
    }

    public Set<String> getDisableFeatures() {
        return disableFeatures;
    }

    /**
     * Disables OpenAPI features by their property name name (e.g., "allOf").
     *
     * @param disableFeatures Feature names to disable.
     */
    public void setDisableFeatures(Set<String> disableFeatures) {
        this.disableFeatures = disableFeatures;
    }

    public ObjectNode getExtensions() {
        return extensions;
    }

    /**
     * Attempts to deserialize the {@code extensions} into the targeted
     * type using a {@link NodeMapper}.
     *
     * <p>Extraneous properties are ignored and <em>not</em> warned on
     * because many different plugins could be used with different
     * configuration POJOs.
     *
     * <p>The result of calling this method is cached for each type,
     * and the cache is cleared when any mutation is made to
     * extensions.
     *
     * @param as Type to deserialize extensions into.
     * @param <T> Type to deserialize extensions into.
     * @return Returns the deserialized type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtensions(Class<T> as) {
        return (T) extensionCache.computeIfAbsent(as, t -> nodeMapper.deserialize(extensions, t));
    }

    /**
     * Sets an arbitrary map of "extensions" used by plugins that need
     * configuration.
     *
     * @param extensions Extensions to set.
     */
    public void setExtensions(ObjectNode extensions) {
        this.extensions = Objects.requireNonNull(extensions);
        extensionCache.clear();
    }

    /**
     * Add an extension to the "extensions" object node using a POJO.
     *
     * @param extensionContainer POJO to serialize and merge into extensions.
     */
    public void putExtensions(Object extensionContainer) {
        ObjectNode serialized = nodeMapper.serialize(extensionContainer).expectObjectNode();
        setExtensions(extensions.merge(serialized));
    }

    /**
     * Add an extension to the "extensions" object node.
     *
     * @param key Property name to set.
     * @param value Value to assigned.
     */
    public void putExtension(String key, Node value) {
        setExtensions(extensions.withMember(key, value));
    }

    /**
     * Add an extension to the "extensions" object node.
     *
     * @param key Property name to set.
     * @param value Value to assigned.
     */
    public void putExtension(String key, boolean value) {
        putExtension(key, Node.from(value));
    }

    /**
     * Add an extension to the "extensions" object node.
     *
     * @param key Property name to set.
     * @param value Value to assigned.
     */
    public void putExtension(String key, String value) {
        putExtension(key, Node.from(value));
    }
}
