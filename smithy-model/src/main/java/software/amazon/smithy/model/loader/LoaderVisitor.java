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

package software.amazon.smithy.model.loader;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Visitor used to drive the creation of a Model.
 *
 * <p>The intent of this visitor is to decouple the serialized format of a
 * Smithy model from the in-memory Model representation. This allows for
 * deserialization code to focus on just extracting data from the model
 * rather than logic around duplication detection, trait loading, etc.
 */
final class LoaderVisitor {
    private static final String[] SUPPORTED_VERSION_PARTS = Model.MODEL_VERSION.split("\\.");
    private static final Logger LOGGER = Logger.getLogger(LoaderVisitor.class.getName());

    /** Whether or not a call to {@link #onEnd()} has been made. */
    private boolean calledOnEnd;

    /** Nullable version that was defined. */
    private String smithyVersion;

    /** Nullable version that was defined parsed into parts. */
    private String[] smithyVersionParts;

    /** Where the version was first defined. */
    private SourceLocation versionSourceLocation;

    /** Factory used to create traits. */
    private final TraitFactory traitFactory;

    /** Property bag used to configure the LoaderVisitor. */
    private final Map<String, Object> properties;

    /** Validation events encountered while loading the model. */
    private final List<ValidationEvent> events = new ArrayList<>();

    /** Model metadata to assemble together. */
    private final Map<String, Node> metadata = new HashMap<>();

    /** Map of shape IDs to a list of traits to apply to the shape once they're built. */
    private final Map<ShapeId, List<PendingTrait>> pendingTraits = new HashMap<>();

    /** References that need to be resolved against a namespace or the prelude. */
    private final List<ForwardReferenceResolver> forwardReferenceResolvers = new ArrayList<>();

    /** Shapes that have yet to be built. */
    private final Map<ShapeId, AbstractShapeBuilder> pendingShapes = new HashMap<>();

    /** Built shapes to add to the Model. Keys are not allowed to conflict with pendingShapes. */
    private final Map<ShapeId, Shape> builtShapes = new HashMap<>();

    /** Trait definitions yet to be built (for example, waiting on a forward shape reference. */
    private final Map<String, TraitDefinition.Builder> pendingTraitDefinitions = new HashMap<>();

    /** Built trait definitions. Keys are not allowed to conflict with pendingTraitDefinitions. */
    private final Map<String, TraitDefinition> builtTraitDefinitions = new HashMap<>();

    /** Current namespace being parsed. */
    private String namespace;

    /** Map of shape aliases to their alias. */
    private final Map<String, ShapeId> useShapes = new HashMap<>();

    /** Map of trait aliases to their alias. */
    private final Map<String, ShapeId> useTraits = new HashMap<>();

    private static final class PendingTrait {
        final String relativeNamespace;
        final String name;
        final Node value;

        PendingTrait(String relativeNamespace, String name, Node value) {
            this.relativeNamespace = relativeNamespace;
            this.name = name;
            this.value = value;
        }
    }

    private static final class ForwardReferenceResolver {
        final ShapeId expectedId;
        final Consumer<ShapeId> consumer;

        ForwardReferenceResolver(ShapeId expectedId, Consumer<ShapeId> consumer) {
            this.expectedId = expectedId;
            this.consumer = consumer;
        }
    }

    /**
     * @param traitFactory Trait factory to use when resolving traits.
     */
    LoaderVisitor(TraitFactory traitFactory) {
        this(traitFactory, Collections.emptyMap());
    }

    /**
     * @param traitFactory Trait factory to use when resolving traits.
     * @param properties Map of loader visitor properties.
     */
    LoaderVisitor(TraitFactory traitFactory, Map<String, Object> properties) {
        this.traitFactory = traitFactory;
        this.properties = properties;
    }

    /**
     * Checks if the LoaderVisitor has defined a specific shape.
     *
     * @param id Shape ID to check.
     * @return Returns true if the shape has been defined.
     */
    public boolean hasDefinedShape(ShapeId id) {
        return builtShapes.containsKey(id) || pendingShapes.containsKey(id);
    }

    /**
     * Invoked each time a new file is loaded, causing any file-specific
     * caches and metadata to be flushed.
     *
     * <p>More specifically, this method clears out the previously used
     * trait and shape name aliases so that they do not affect the next
     * file that is loaded.
     *
     * @param filename The name of the file being opened.
     */
    public void onOpenFile(String filename) {
        LOGGER.fine(() -> "Beginning to parse " + filename);
        namespace = null;
        useShapes.clear();
        useTraits.clear();
    }

    /**
     * Gets the namespace that is currently being loaded.
     *
     * @return Returns the namespace or null if none is set.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets and validates the current namespace of the visitor.
     *
     * <p>Relative shape and trait definitions will use this
     * namespace by default.
     *
     * @param namespace Namespace being entered.
     * @param source The source location of where the namespace is defined.
     */
    public void onNamespace(String namespace, FromSourceLocation source) {
        if (!ShapeId.VALID_NAMESPACE.matcher(namespace).find()) {
            String msg = String.format("Invalid namespace name `%s`. Namespaces must match the following pattern: %s",
                                       namespace, ShapeId.VALID_NAMESPACE);
            throw new ModelSyntaxException(msg, source);
        }

        this.namespace = namespace;
    }

    /**
     * Resolve shape targets and tracks forward references.
     *
     * <p>Smithy models allow for forward references to shapes that have not
     * yet been defined. Only after all shapes are loaded is the entire set
     * of possible shape IDs known. This normally isn't a concern, but Smithy
     * allows for public shapes defined in the prelude to be referenced by
     * targets like members and resource identifiers without an absolute
     * shape ID (for example, {@code String}). However, a relative prelude
     * shape ID is only resolved for such a target if a shape of the same
     * name was not defined in the same namespace in which the target
     * was defined.
     *
     * <p>If a shape in the same namespace as the target has already been
     * defined or if the target is absolute and cannot resolve to a prelude
     * shape, the provided {@code resolver} is invoked immediately. Otherwise,
     * the {@code resolver} is invoked inside of the {@link #onEnd} method
     * only after all shapes have been declared.
     *
     * @param target Shape that is targeted.
     * @param resolver The consumer to invoke once the shape ID is resolved.
     */
    public void onShapeTarget(String target, Consumer<ShapeId> resolver) {
        assertNamespaceIsPresent(SourceLocation.none());

        ShapeId expectedId = ShapeId.fromOptionalNamespace(namespace, target);
        if (hasDefinedShape(expectedId) || target.contains("#")) {
            // Account for previously seen shapes in this namespace and absolute shapes.
            resolver.accept(expectedId);
        } else if (useShapes.containsKey(target)) {
            // Account for aliased shapes.
            resolver.accept(useShapes.get(target));
        } else {
            forwardReferenceResolvers.add(new ForwardReferenceResolver(expectedId, resolver));
        }
    }

    /**
     * Checks if a specific property is set.
     *
     * @param property Name of the property to check.
     * @return Returns true if the property is set.
     */
    public boolean hasProperty(String property) {
        return properties.containsKey(property);
    }

    /**
     * Gets a property from the loader visitor.
     *
     * @param property Name of the property to get.
     * @return Returns the optionally found property.
     */
    public Optional<Object> getProperty(String property) {
        return Optional.ofNullable(properties.get(property));
    }

    /**
     * Gets a property from the loader visitor of a specific type.
     *
     * @param property Name of the property to get.
     * @param <T> Type to convert the property to if found.
     * @param type Type to convert the property to if found.
     * @return Returns the optionally found property.
     * @throws ClassCastException if a found property is not of the expected type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String property, Class<T> type) {
        return getProperty(property).map(value -> {
            if (!type.isInstance(value)) {
                throw new ClassCastException(String.format(
                        "Expected `%s` property of the LoaderVisitor to be a `%s`, but found a `%s`",
                        property, type.getName(), value.getClass().getName()));
            }
            return (T) value;
        });
    }

    /**
     * Called when visiting the Smithy model version.
     *
     * @param sourceLocation Location of the version number.
     * @param smithyVersion Version to set.
     */
    public void onVersion(SourceLocation sourceLocation, String smithyVersion) {
        // Optimized case of an exact match.
        if (this.smithyVersion != null && this.smithyVersion.equals(smithyVersion)) {
            return;
        }

        String[] versionParts = smithyVersion.split("\\.");
        validateVersionNumber(sourceLocation, versionParts);

        if (this.smithyVersion != null && !areVersionsCompatible(smithyVersionParts, versionParts)) {
            throw new SourceException(format(
                    "Cannot set Smithy version to `%s` because it was previously set to an incompatible version "
                    + "`%s` in %s", smithyVersion, this.smithyVersion, versionSourceLocation), sourceLocation);
        }

        if (!isSupportedVersion(versionParts)) {
            throw new SourceException(
                    format("Invalid Smithy version provided: `%s`. Expected a version compatible with the tooling "
                           + "version of `%s`. Perhaps you need to update your version of Smithy?",
                           String.join(".", versionParts), Model.MODEL_VERSION),
                    sourceLocation);
        }

        this.smithyVersion = smithyVersion;
        this.smithyVersionParts = versionParts;
        this.versionSourceLocation = sourceLocation;

        validateState(sourceLocation);
    }

    private static void validateVersionNumber(SourceLocation sourceLocation, String[] versionParts) {
        // We require at least 2 but a maximum of 3 segments (e.g., "1.0", and "1.0.0" are equal).
        if (versionParts.length < 2 || versionParts.length > 3) {
            throw new SourceException(
                    "Smithy version number should have 2 or 3 parts: "
                    + String.join(".", versionParts), sourceLocation);
        }

        for (String part : versionParts) {
            if (part.isEmpty()) {
                throw new SourceException("Invalid Smithy version number: "
                                          + String.join(".", versionParts), sourceLocation);
            }
            for (int i = 0; i < part.length(); i++) {
                if (!Character.isDigit(part.charAt(i))) {
                    throw new SourceException(
                            "Invalid Smithy version number: " + String.join(".", versionParts), sourceLocation);
                }
            }
        }
    }

    private static boolean areVersionsCompatible(String[] left, String[] right) {
        if (!left[0].equals(right[0])) {
            return false;
        }

        // A major version of "1" or higher are equal at this point.
        // A major version of "0" must also check the minor version.
        return !left[0].equals("0") || left[1].equals(right[1]);
    }

    private static boolean isSupportedVersion(String[] version) {
        // First ensure that the version is in the same range.
        if (!areVersionsCompatible(SUPPORTED_VERSION_PARTS, version)) {
            return false;
        }

        // Next ensure that the version does not exceed the latest known
        // minor version of the model.
        return version[1].equals(SUPPORTED_VERSION_PARTS[1])
               || Integer.parseInt(version[1]) < Integer.parseInt(SUPPORTED_VERSION_PARTS[1]);
    }

    private void validateState(FromSourceLocation sourceLocation) {
        if (calledOnEnd) {
            throw new IllegalStateException("Cannot call visitor method because visitor has called onEnd");
        }

        if (smithyVersion == null) {
            // Assume latest supported version.
            LOGGER.warning(format("No Smithy version explicitly specified in %s, so assuming version of %s",
                                  sourceLocation.getSourceLocation().getFilename(), Model.MODEL_VERSION));
            onVersion(sourceLocation.getSourceLocation(), Model.MODEL_VERSION);
        }
    }

    /**
     * Adds an error to the loader.
     *
     * @param event Validation event to add.
     */
    public void onError(ValidationEvent event) {
        events.add(Objects.requireNonNull(event));
    }

    /**
     * Invoked when a shape definition name is defined, validates the name,
     * and returns the resolved shape ID of the name.
     *
     * <p>This method ensures a namespace has been set, the syntax is valid,
     * and that the name does not conflict with any previously defined use
     * statements.
     *
     * @param name Name being defined.
     * @param source The location of where it is defined.
     * @return Returns the parsed and loaded shape ID.
     */
    public ShapeId onShapeDefName(String name, FromSourceLocation source) {
        return shapeId(name, "shape", source, useShapes);
    }

    /**
     * Invoked when a trait definition name is defined, validates the name,
     * and returns the resolved ID of the name.
     *
     * <p>This method ensures a namespace has been set, the syntax is valid,
     * and that the name does not conflict with any previously defined use
     * statements.
     *
     * @param name Name being defined.
     * @param source The location of where it is defined.
     * @return Returns the parsed and loaded shape ID.
     */
    public ShapeId onTraitDefName(String name, FromSourceLocation source) {
        return shapeId(name, "trait", source, useTraits);
    }

    private ShapeId shapeId(String name, String descriptor, FromSourceLocation source, Map<String, ShapeId> map) {
        validateState(source);
        assertNamespaceIsPresent(source);

        if (map.containsKey(name)) {
            String msg = String.format("%s name `%s` conflicts with imported shape `%s`",
                                       descriptor, name, map.get(name));
            throw new UseException(msg, source);
        }

        try {
            return ShapeId.fromRelative(namespace, name);
        } catch (ShapeIdSyntaxException e) {
            throw new ModelSyntaxException("Invalid " + descriptor + " name: " + name, source);
        }
    }

    private void assertNamespaceIsPresent(FromSourceLocation source) {
        if (namespace == null) {
            throw new ModelSyntaxException("A namespace must be set before shapes or traits can be defined", source);
        }
    }

    /**
     * Adds a shape to the loader.
     *
     * @param shapeBuilder Shape builder to add.
     */
    public void onShape(AbstractShapeBuilder shapeBuilder) {
        validateState(shapeBuilder);
        ShapeId id = SmithyBuilder.requiredState("id", shapeBuilder.getId());
        if (validateOnShape(id, shapeBuilder)) {
            pendingShapes.put(id, shapeBuilder);
        }
    }

    /**
     * Adds a shape to the loader.
     *
     * @param shape Built shape to add to the loader visitor.
     */
    public void onShape(Shape shape) {
        validateState(shape);
        if (validateOnShape(shape.getId(), shape)) {
            builtShapes.put(shape.getId(), shape);
        }
    }

    private boolean validateOnShape(ShapeId id, FromSourceLocation source) {
        if (!hasDefinedShape(id)) {
            return true;
        } else if (Prelude.isPreludeShape(id)) {
            // Ignore prelude shape conflicts since it's such a common case of
            // passing an already built shape index into a ModelAssembler.
        } else {
            // The shape has been duplicated, so get the previously defined pending shape or built shape.
            SourceLocation previous = Optional.<FromSourceLocation>ofNullable(pendingShapes.get(id))
                    .orElseGet(() -> builtShapes.get(id)).getSourceLocation();
            // Ignore duplicate shapes defined in the same file.
            if (previous != SourceLocation.NONE && previous.equals(source.getSourceLocation())) {
                LOGGER.warning(() -> "Ignoring duplicate shape definition defined in the same file: "
                                     + id + " defined at " + source.getSourceLocation());
            } else {
                onError(ValidationEvent.builder()
                                .eventId(Validator.MODEL_ERROR)
                                .severity(Severity.ERROR)
                                .sourceLocation(source)
                                .message(String.format(
                                        "Duplicate shape definition for `%s` found at `%s` and `%s`",
                                        id, previous.getSourceLocation(), source.getSourceLocation()))
                                .build());
            }
        }

        return false;
    }

    /**
     * Adds a trait to a shape.
     *
     * <p>Resolving the trait against a trait definition is deferred until
     * the entire model is loaded. A namespace is required to have been set
     * if the trait name is not absolute.
     *
     * @param target Shape to add the trait to.
     * @param traitName Fully-qualified trait name to add.
     * @param traitValue Trait value as a Node object.
     */
    public void onTrait(ShapeId target, String traitName, Node traitValue) {
        validateState(traitValue);

        if (!traitName.contains("#")) {
            assertNamespaceIsPresent(traitValue);
        }

        // Account for trait aliases.
        if (useTraits.containsKey(traitName)) {
            traitName = useTraits.get(traitName).toString();
        }

        PendingTrait pendingTrait = new PendingTrait(namespace, traitName, traitValue);
        pendingTraits.computeIfAbsent(target, id -> new ArrayList<>()).add(pendingTrait);
    }

    /**
     * Adds a trait definition to the loader.
     *
     * @param definition Trait definition to add.
     */
    public void onTraitDef(TraitDefinition.Builder definition) {
        if (validateOnTraitDef(definition.getFullyQualifiedName(), definition)) {
            pendingTraitDefinitions.put(definition.getFullyQualifiedName(), definition);
        }
    }

    /**
     * Adds a trait definition to the loader.
     *
     * @param definition Trait definition to add.
     */
    public void onTraitDef(TraitDefinition definition) {
        if (validateOnTraitDef(definition.getFullyQualifiedName(), definition)) {
            builtTraitDefinitions.put(definition.getFullyQualifiedName(), definition);
        }
    }

    private boolean validateOnTraitDef(String name, FromSourceLocation source) {
        validateState(source);
        if (!pendingTraitDefinitions.containsKey(name) && !builtTraitDefinitions.containsKey(name)) {
            return true;
        } else if (Prelude.isPreludeTraitDefinition(name)) {
            // Duplicate trait defined in the prelude are ignored.
        } else {
            // The trait definition has been duplicated.
            SourceLocation previous = Optional.<FromSourceLocation>ofNullable(pendingTraitDefinitions.get(name))
                    .orElseGet(() -> builtTraitDefinitions.get(name)).getSourceLocation();
            // Ignore duplicate trait defs from the same file location.
            if (previous != SourceLocation.NONE && previous.equals(source.getSourceLocation())) {
                LOGGER.warning(() -> "Ignoring duplicate trait definition defined in the same file: "
                                     + name + " defined at " + source.getSourceLocation());
            } else {
                onError(ValidationEvent.builder()
                                .eventId(Validator.MODEL_ERROR)
                                .severity(Severity.ERROR)
                                .sourceLocation(source)
                                .message(String.format(
                                        "Duplicate trait definitions for `%s` found at `%s` and `%s`",
                                        name, previous.getSourceLocation(), source))
                                .build());
            }
        }

        return false;
    }

    /**
     * Adds metadata to the loader.
     *
     * @param key Metadata key to add.
     * @param value Metadata value to add.
     */
    public void onMetadata(String key, Node value) {
        validateState(value);

        if (!metadata.containsKey(key)) {
            metadata.put(key, value);
        } else if (metadata.get(key).isArrayNode() && value.isArrayNode()) {
            ArrayNode previous = metadata.get(key).expectArrayNode();
            List<Node> merged = new ArrayList<>(previous.getElements());
            merged.addAll(value.expectArrayNode().getElements());
            ArrayNode mergedArray = new ArrayNode(merged, value.getSourceLocation());
            metadata.put(key, mergedArray);
        } else if (!metadata.get(key).equals(value)) {
            onError(ValidationEvent.builder()
                    .eventId(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .sourceLocation(value)
                    .message(format(
                            "Metadata conflict for key `%s`. Defined in both `%s` and `%s`",
                            key, value.getSourceLocation(), metadata.get(key).getSourceLocation()))
                    .build());
        } else {
            LOGGER.fine(() -> "Ignoring duplicate metadata definition of " + key);
        }
    }

    /**
     * Registers a fully-qualified trait name as an alias.
     *
     * <p>These aliases are cleared when {@link #onOpenFile} is called.
     *
     * @param id Shape ID to use.
     * @param location The location of where it is registered.
     */
    void onUseTrait(ShapeId id, FromSourceLocation location) {
        addUseAlias("use trait", id, useTraits, location);
    }

    /**
     * Registers an absolute shape ID as an alias.
     *
     * <p>These aliases are cleared when {@link #onOpenFile} is called.
     *
     * @param id Fully-qualified shape ID to register.
     * @param location The location of where it is registered.
     */
    void onUseShape(ShapeId id, FromSourceLocation location) {
        addUseAlias("use shape", id, useShapes, location);
    }

    private void addUseAlias(String key, ShapeId id, Map<String, ShapeId> map, FromSourceLocation location) {
        validateState(location);
        ShapeId previous = map.put(id.getName(), id);
        if (previous != null) {
            throw new UseException(String.format(
                    "Cannot `%s` name `%s` because it conflicts with `%s`", key, id, previous), location);
        }
    }

    /**
     * Called when the visitor has completed.
     *
     * @return Returns the validated model result.
     */
    public ValidatedResult<Model> onEnd() {
        validateState(SourceLocation.NONE);
        calledOnEnd = true;
        Model.Builder modelBuilder = Model.builder().smithyVersion(smithyVersion).metadata(metadata);
        ShapeIndex.Builder shapeIndexBuilder = ShapeIndex.builder();

        finalizeShapeTargets();
        finalizePendingTraitDefinitions();
        finalizePendingTraits();
        modelBuilder.addTraitDefinitions(builtTraitDefinitions.values());

        // Ensure that shape builders are created for the container shapes of
        // each modified member (builders were already created if a shape has
        // a pending trait). This needs to be done before iterating over the
        // pending shapes because a collection can't be modified while
        // iterating.
        List<ShapeId> needsConversion = new ArrayList<>();
        for (AbstractShapeBuilder builder : pendingShapes.values()) {
            if (builder instanceof MemberShape.Builder) {
                needsConversion.add(builder.getId().withoutMember());
            }
        }
        needsConversion.forEach(this::resolveShapeBuilder);

        // Build members and add them to their containing shape builders.
        for (AbstractShapeBuilder shape : pendingShapes.values()) {
            if (shape.getClass() == MemberShape.Builder.class) {
                MemberShape member = (MemberShape) buildShape(shapeIndexBuilder, shape);
                if (member != null) {
                    AbstractShapeBuilder container = pendingShapes.get(shape.getId().withoutMember());
                    if (container == null) {
                        events.add(ValidationEvent.builder()
                                .shape(member)
                                .eventId(Validator.MODEL_ERROR)
                                .severity(Severity.ERROR)
                                .message(format("Member shape `%s` added to non-existent shape", member.getId()))
                                .build());
                    } else {
                        container.addMember(member);
                    }
                }
            }
        }

        // Now that members were built, build all non-members.
        for (AbstractShapeBuilder shape : pendingShapes.values()) {
            if (shape.getClass() != MemberShape.Builder.class) {
                buildShape(shapeIndexBuilder, shape);
            }
        }

        // Add any remaining built shapes.
        shapeIndexBuilder.addShapes(builtShapes.values());
        modelBuilder.shapeIndex(shapeIndexBuilder.build());
        return new ValidatedResult<>(modelBuilder.build(), events);
    }

    private void finalizeShapeTargets() {
        // Run any finalizers used for things like forward reference resolution.
        for (ForwardReferenceResolver resolver : forwardReferenceResolvers) {
            // First, resolve to a shape in the current namespace if one exists.
            if (!hasDefinedShape(resolver.expectedId)) {
                // Next resolve to a prelude shape if one exists and is public.
                ShapeId preludeId = ShapeId.fromParts(Prelude.NAMESPACE, resolver.expectedId.asRelativeReference());
                if (Prelude.isPublicPreludeShape(preludeId)) {
                    resolver.consumer.accept(preludeId);
                    continue;
                }
                // Finally, just default back to original namespace using a broken target.
            }
            resolver.consumer.accept(resolver.expectedId);
        }

        forwardReferenceResolvers.clear();
    }

    private void finalizePendingTraitDefinitions() {
        for (Map.Entry<String, TraitDefinition.Builder> definition : pendingTraitDefinitions.entrySet()) {
            builtTraitDefinitions.put(definition.getKey(), definition.getValue().build());
        }
    }

    private void finalizePendingTraits() {
        // Build trait nodes and add them to their shape builders.
        for (Map.Entry<ShapeId, List<PendingTrait>> entry : pendingTraits.entrySet()) {
            ShapeId target = entry.getKey();
            List<PendingTrait> pendingTraits = entry.getValue();
            AbstractShapeBuilder builder = resolveShapeBuilder(target);
            if (builder == null) {
                // The shape was not found, so emit a validation event for every trait applied to it.
                emitErrorsForEachInvalidTraitTarget(target, pendingTraits);
            } else {
                for (Map.Entry<String, Node> computedEntry : computeTraits(builder, pendingTraits).entrySet()) {
                    createAndApplyTraitToShape(builder, computedEntry.getKey(), computedEntry.getValue());
                }
            }
        }
    }

    private AbstractShapeBuilder resolveShapeBuilder(ShapeId id) {
        if (pendingShapes.containsKey(id)) {
            return pendingShapes.get(id);
        } else if (builtShapes.containsKey(id)) {
            // If the shape is not a builder but rather a built shape, then convert into a builder.
            // Once converted, the shape is removed from builtShapes and added into pendingShapes.
            AbstractShapeBuilder builder = (AbstractShapeBuilder) Shape.shapeToBuilder(builtShapes.remove(id));
            pendingShapes.put(id, builder);
            return builder;
        } else {
            return null;
        }
    }

    private void emitErrorsForEachInvalidTraitTarget(ShapeId target, List<PendingTrait> pendingTraits) {
        for (PendingTrait pendingTrait : pendingTraits) {
            onError(ValidationEvent.builder()
                    .eventId(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .sourceLocation(pendingTrait.value.getSourceLocation())
                    .message(format(
                            "Trait `%s` applied to unknown shape `%s`",
                            pendingTrait.name, target))
                    .build());
        }
    }

    private Shape buildShape(ShapeIndex.Builder shapeIndexBuilder, AbstractShapeBuilder shapeBuilder) {
        try {
            Shape result = (Shape) shapeBuilder.build();
            shapeIndexBuilder.addShape(result);
            return result;
        } catch (SourceException e) {
            onError(ValidationEvent.fromSourceException(e).toBuilder().shapeId(shapeBuilder.getId()).build());
            return null;
        }
    }

    /**
     * This method resolves the fully-qualified names of each pending trait
     * (checking the namespace that contains the trait and the prelude), merges
     * them if necessary, and returns the merged map of trait names to trait
     * node values.
     *
     * Traits are added to a flat list of pending traits for a shape. We can
     * only actually determine the resolved trait definition to apply once the
     * entire model is loaded an all trait definitions are known. As such,
     * the logic for merging traits and detecting duplicates is deferred until
     * the end of the model is detected.
     *
     * @param shapeBuilder Shape that is being resolved.
     * @param pending The list of pending traits to resolve.
     * @return Returns the resolved map of pending traits.
     */
    private Map<String, Node> computeTraits(AbstractShapeBuilder shapeBuilder, List<PendingTrait> pending) {
        Map<String, Node> traits = new HashMap<>();
        for (PendingTrait trait : pending) {
            TraitDefinition definition = resolveTraitDefinition(trait);

            if (definition == null) {
                onUnresolvedTraitName(shapeBuilder, trait);
                continue;
            }

            String traitName = definition.getFullyQualifiedName();
            Node value = coerceTraitValue(trait.value, definition);
            Node previous = traits.get(traitName);

            if (previous == null) {
                traits.put(traitName, value);
            } else if (previous.isArrayNode() && value.isArrayNode()) {
                // You can merge trait arrays.
                traits.put(traitName, value.asArrayNode().get().merge(previous.asArrayNode().get()));
            } else if (previous.equals(value)) {
                LOGGER.fine(() -> String.format(
                        "Ignoring duplicate %s trait value on %s", traitName, shapeBuilder.getId()));
            } else {
                onDuplicateTrait(shapeBuilder.getId(), traitName, previous, value);
            }
        }

        return traits;
    }

    /**
     * Null values provided for traits are coerced in some cases to fit the
     * type referenced by the shape. This is used in the .smithy format to
     * make is so that you can write "@foo" rather than "@foo(true)",
     * "@foo()", or "@foo([])".
     *
     * 1. Boolean traits are converted to `true`.
     * 2. Structure and map traits are converted to an empty object.
     * 3. List and set traits are converted to an empty array.
     *
     * Boolean values can be coerced from `true` to an empty object if the
     * shape targeted by the trait is a structure. This allows traits to
     * evolve over time from being an annotation trait to a structured
     * trait (with optional members only to make it backward-compatible).
     *
     * @param value Value to coerce.
     * @param definition Trait definition to base the coercion on.
     * @return Returns the coerced value.
     */
    private Node coerceTraitValue(Node value, TraitDefinition definition) {
        if (value.isNullNode()) {
            ShapeType targetType = determineTraitDefinitionType(definition);
            if (targetType == null) {
                return new BooleanNode(true, value.getSourceLocation());
            } else if (targetType == ShapeType.STRUCTURE || targetType == ShapeType.MAP) {
                return new ObjectNode(Collections.emptyMap(), value.getSourceLocation());
            } else if (targetType == ShapeType.LIST || targetType == ShapeType.SET) {
                return new ArrayNode(Collections.emptyList(), value.getSourceLocation());
            } else {
                return value;
            }
        } else if (value.isBooleanNode()
                   && value.asBooleanNode().get().getValue()
                   && determineTraitDefinitionType(definition) == ShapeType.STRUCTURE) {
            return new ObjectNode(Collections.emptyMap(), value.getSourceLocation());
        }

        return value;
    }

    private ShapeType determineTraitDefinitionType(TraitDefinition definition) {
        ShapeId target = definition.getShape().orElse(null);
        if (pendingShapes.containsKey(target)) {
            return pendingShapes.get(target).getShapeType();
        } else if (builtShapes.containsKey(target)) {
            return builtShapes.get(target).getType();
        } else {
            // The trait is an annotation trait.
            return null;
        }
    }

    /**
     * Resolves the given pending trait name against all known trait
     * definitions.
     *
     * 1. If the trait uses an absolute ID, then use that.
     * 2. If there is a trait definition by the relative name in the same
     *    namespace the trait was defined, then use that.
     * 3. Check to see if one of the prelude traits match the given name.
     *
     * @param trait Trait name to resolve.
     * @return Returns the resolved definition or null if not found.
     */
    private TraitDefinition resolveTraitDefinition(PendingTrait trait) {
        String absoluteName = trait.name.contains("#") ? trait.name : trait.relativeNamespace + "#" + trait.name;
        return builtTraitDefinitions.containsKey(absoluteName)
               ? builtTraitDefinitions.get(absoluteName)
               : builtTraitDefinitions.get(Prelude.NAMESPACE + "#" + trait.name);
    }

    private void onDuplicateTrait(ShapeId target, String traitName, FromSourceLocation previous, Node duplicate) {
        onError(ValidationEvent.builder()
                .eventId(Validator.MODEL_ERROR)
                .severity(Severity.ERROR)
                .sourceLocation(duplicate.getSourceLocation())
                .shapeId(target)
                .message(format(
                        "Conflicting `%s` trait found on shape `%s`. The previous trait was defined at `%s`, "
                        + "and a conflicting trait was defined at `%s`.",
                        traitName, target, previous.getSourceLocation(), duplicate.getSourceLocation()))
                .build());
    }

    private void onUnresolvedTraitName(AbstractShapeBuilder shapeBuilder, PendingTrait trait) {
        Severity severity = getProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, Boolean.class).orElse(false)
                            ? Severity.WARNING : Severity.ERROR;

        // Fail if the trait cannot be resolved.
        onError(ValidationEvent.builder()
                .eventId(Validator.MODEL_ERROR)
                .severity(severity)
                .sourceLocation(trait.value.getSourceLocation())
                .shapeId(shapeBuilder.getId())
                .message(format(
                        "Unable to resolve trait `%s` (%s value defined in the `%s` namespace). If this is a "
                        + "custom trait, then it must be defined before it can be used in a model.",
                        trait.name, trait.value.getType(), trait.relativeNamespace))
                .build());
    }

    /**
     * Applies a trait to a shape. If a concrete class for the trait cannot
     * be found, then a {@link DynamicTrait} is created. If the trait throws
     * an exception while being created, it is caught and the validation
     * event is logged.
     *
     * @param shapeBuilder Shape builder to update.
     * @param traitName Name of the fully-qualified trait to add.
     * @param traitValue The trait value to set.
     */
    private void createAndApplyTraitToShape(AbstractShapeBuilder shapeBuilder, String traitName, Node traitValue) {
        try {
            // Create the trait using a factory, or default to an un-typed modeled trait.
            Trait createdTrait = traitFactory.createTrait(traitName, shapeBuilder.getId(), traitValue)
                    .orElseGet(() -> new DynamicTrait(traitName, traitValue));
            shapeBuilder.addTrait(createdTrait);
        } catch (SourceException e) {
            events.add(ValidationEvent.fromSourceException(e, format("Error creating trait `%s`: ",
                            Trait.getIdiomaticTraitName(traitName)))
                    .toBuilder()
                    .shapeId(shapeBuilder.getId())
                    .build());
        }
    }
}
