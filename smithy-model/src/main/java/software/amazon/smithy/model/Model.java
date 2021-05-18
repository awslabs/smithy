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

package software.amazon.smithy.model;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A Smithy model that contains shapes, traits, metadata, and various
 * computed information used to interpret the model.
 */
public final class Model implements ToSmithyBuilder<Model> {

    /** Specifies the highest supported version of the IDL. */
    public static final String MODEL_VERSION = "1.0";

    /** The map of metadata keys to their "node" values. */
    private final Map<String, Node> metadata;

    /** A map of shape ID to shapes that backs the shape map. */
    private final Map<ShapeId, Shape> shapeMap;

    /** A cache of shapes of a specific type. */
    private final Map<Class<? extends Shape>, Set<? extends Shape>> cachedTypes = new ConcurrentHashMap<>();

    /** Cache of computed {@link KnowledgeIndex} instances. */
    private final Map<Class<? extends KnowledgeIndex>, KnowledgeIndex> blackboard
            = Collections.synchronizedMap(new IdentityHashMap<>());

    /** Lazily computed trait mappings. */
    private volatile TraitCache traitCache;

    /** Lazily computed hashcode. */
    private int hash;

    private Model(Builder builder) {
        shapeMap = MapUtils.copyOf(builder.shapeMap);
        metadata = builder.metadata.isEmpty() ? MapUtils.of() : MapUtils.copyOf(builder.metadata);
    }

    /**
     * Builds an explicitly configured Smithy model.
     *
     * <p>Note that the builder does not validate the correctness of the
     * model. Use the {@link #assembler()} method to build <em>and</em>
     * validate a model.
     *
     * @return Returns a model builder.
     * @see #assembler()
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Assembles and validates a Smithy model from files, nodes, and other
     * disparate sources.
     *
     * @return Returns a model assembler.
     */
    public static ModelAssembler assembler() {
        return new ModelAssembler();
    }

    /**
     * Creates a {@link ModelAssembler} that is configured to discover traits,
     * validators, and built-in validators using the given
     * {@code ClassLoader}.
     *
     * @param classLoader Class loader used to discover services.
     * @return Returns a model assembler.
     */
    public static ModelAssembler assembler(ClassLoader classLoader) {
        return new ModelAssembler()
                .traitFactory(TraitFactory.createServiceFactory(classLoader))
                .validatorFactory(ValidatorFactory.createServiceFactory(classLoader));
    }

    /**
     * Gets a metadata property by namespace and name.
     *
     * @param name Name of the property to retrieve.
     * @return Returns the optional property.
     */
    public Optional<Node> getMetadataProperty(String name) {
        return Optional.ofNullable(metadata.get(name));
    }

    /**
     * @return Gets the unmodifiable metadata of the model across all
     *  namespaces.
     */
    public Map<String, Node> getMetadata() {
        return metadata;
    }

    /**
     * Gets the trait definition of a specific trait shape ID.
     *
     * @param traitId ID of the shape to get the trait definition of.
     * @return Returns the optionally found trait definition.
     */
    public Optional<TraitDefinition> getTraitDefinition(ToShapeId traitId) {
        return getShape(traitId.toShapeId()).flatMap(shape -> shape.getTrait(TraitDefinition.class));
    }

    /**
     * Gets a set of shapes in the model marked with a specific trait.
     *
     * @param trait Trait shape ID to look for on shapes.
     * @return Returns the immutable set of matching shapes.
     */
    public Set<Shape> getShapesWithTrait(ToShapeId trait) {
        Map<ShapeId, Set<Shape>> mappings = getTraitCache().traitIdsToShapes;
        return Collections.unmodifiableSet(mappings.getOrDefault(trait.toShapeId(), Collections.emptySet()));
    }

    private TraitCache getTraitCache() {
        TraitCache cache = traitCache;
        if (cache == null) {
            synchronized (this) {
                cache = traitCache;
                if (cache == null) {
                    traitCache = cache = new TraitCache(this.shapeMap.values());
                }
            }
        }
        return cache;
    }

    /**
     * Gets the immutable set of {@code ShapeId} in the model.
     *
     * @return Returns the shape IDs.
     */
    public Set<ShapeId> getShapeIds() {
        return shapeMap.keySet();
    }

    /**
     * Gets a set of shapes in the model marked with a specific trait.
     *
     * <p>The result is an exact match on trait classes and does not utilize
     * any kind of polymorphic instance of checks.
     *
     * @param trait Trait class to look for on shapes.
     * @return Returns the immutable set of matching shapes.
     */
    public Set<Shape> getShapesWithTrait(Class<? extends Trait> trait) {
        Map<Class<? extends Trait>, Set<Shape>> mappings = getTraitCache().traitsToShapes;
        return Collections.unmodifiableSet(mappings.getOrDefault(trait, Collections.emptySet()));
    }

    /**
     * Gets a set of every trait shape ID that is used in the model.
     *
     * @return Returns the shape IDs of traits used in the model.
     */
    public Set<ShapeId> getAppliedTraits() {
        return Collections.unmodifiableSet(getTraitCache().traitIdsToShapes.keySet());
    }

    /**
     * Returns true if the given trait shape ID was used in the model.
     *
     * @param trait The trait class to check.
     * @return Returns true if the trait was used in the model.
     */
    public boolean isTraitApplied(Class<? extends Trait> trait) {
        return !getShapesWithTrait(trait).isEmpty();
    }

    /**
     * Attempts to retrieve a {@link Shape} by {@link ShapeId}.
     *
     * @param id Shape to retrieve by ID.
     * @return Returns the optional shape.
     */
    public Optional<Shape> getShape(ShapeId id) {
        return Optional.ofNullable(shapeMap.get(id));
    }

    /**
     * Attempts to retrieve a {@link Shape} by {@link ShapeId} and
     * throws if not found.
     *
     * @param id Shape to retrieve by ID.
     * @return Returns the shape.
     * @throws ExpectationNotMetException if the shape is not found.
     */
    public Shape expectShape(ShapeId id) {
        return getShape(id).orElseThrow(() -> new ExpectationNotMetException(
                "Shape not found in model: " + id, SourceLocation.NONE));
    }

    /**
     * Attempts to retrieve a {@link Shape} by {@link ShapeId} and
     * throws if not found or if the shape is not of the expected type.
     *
     * @param id Shape to retrieve by ID.
     * @param type Shape type to expect and convert to.
     * @param <T> Expected shape type.
     * @return Returns the shape.
     * @throws ExpectationNotMetException if the shape is not found or is not the expected type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Shape> T expectShape(ShapeId id, Class<T> type) {
        Shape shape = expectShape(id);
        if (type.isInstance(shape)) {
            return (T) shape;
        }

        throw new ExpectationNotMetException(String.format(
                "Expected shape `%s` to be an instance of `%s`, but found `%s`",
                id, type.getSimpleName(), shape.getType()), shape);
    }

    /**
     * Gets a stream of {@link Shape}s in the index.
     *
     * @return Returns a stream of shapes.
     */
    public Stream<Shape> shapes() {
        return shapeMap.values().stream();
    }

    /**
     * Gets a stream of shapes in the index of a specific type {@code T}.
     *
     * <p>The provided shapeType class must exactly match the class of a
     * shape in the model in order to be returned from this method;
     * that is, the provided class must be a concrete subclass of
     * {@link Shape} and not an abstract class like {@link NumberShape}.
     *
     * @param shapeType Shape type {@code T} to retrieve.
     * @param <T> Shape type to stream from the index.
     * @return A stream of shapes of {@code T} matching {@code shapeType}.
     */
    public <T extends Shape> Stream<T> shapes(Class<T> shapeType) {
        return toSet(shapeType).stream();
    }

    /**
     * Gets an immutable Set of shapes of a specific type.
     *
     * @param shapeType Type of shape to get a set of.
     * @param <T> Shape type to get from the index.
     * @return Returns an unmodifiable set of shapes.
     */
    @SuppressWarnings("unchecked")
    public <T extends Shape> Set<T> toSet(Class<T> shapeType) {
        return (Set<T>) cachedTypes.computeIfAbsent(shapeType, t -> {
            Set<T> result = new HashSet<>();
            for (Shape shape : shapeMap.values()) {
                if (shape.getClass() == shapeType) {
                    result.add((T) shape);
                }
            }
            return Collections.unmodifiableSet(result);
        });
    }

    /**
     * Converts the model to an immutable Set of shapes.
     *
     * @return Returns an unmodifiable set of shapes.
     */
    public Set<Shape> toSet() {
        return new AbstractSet<Shape>() {
            @Override
            public int size() {
                return shapeMap.size();
            }

            @Override
            public boolean contains(Object o) {
                return o instanceof Shape && shapeMap.containsKey(((Shape) o).getId());
            }

            @Override
            public Iterator<Shape> iterator() {
                return shapeMap.values().iterator();
            }
        };
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Model)) {
            return false;
        } else if (other == this) {
            return true;
        }

        Model otherModel = (Model) other;
        return getMetadata().equals(otherModel.getMetadata()) && shapeMap.equals(otherModel.shapeMap);
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            result = Objects.hash(getMetadata(), shapeMap.keySet());
            hash = result;
        }
        return result;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .metadata(getMetadata())
                .addShapes(this);
    }

    /**
     * This method is deprecated. Use the {@code of} method of the
     * {@link KnowledgeIndex} you wish to create instead.
     *
     * @param type Type of knowledge index to retrieve.
     * @param <T> The type of knowledge index to retrieve.
     * @return Returns the computed knowledge index.
     */
    @Deprecated
    public <T extends KnowledgeIndex> T getKnowledge(Class<T> type) {
        return getKnowledge(type, m -> {
            try {
                return type.getConstructor(Model.class).newInstance(this);
            } catch (NoSuchMethodException e) {
                String message = String.format(
                        "KnowledgeIndex for type `%s` does not expose a public constructor that accepts a Model", type);
                throw new RuntimeException(message, e);
            } catch (ReflectiveOperationException e) {
                String message = String.format(
                        "Unable to create a KnowledgeIndex for type `%s`: %s", type, e.getMessage());
                throw new RuntimeException(message, e);
            }
        });
    }

    /**
     * Gets a computed "knowledge index" of a specific type for the model
     * and caches it for subsequent retrieval.
     *
     * <p>This method should not typically be called directly because
     * KnowledgeIndex classes should all provide a public static {@code of}
     * method that accepts a {@code Model} and returns an instance of the
     * index by invoking {@code getKnowledge}.
     *
     * <p>If a {@link KnowledgeIndex} of the given type has not yet been
     * computed, one will be created using the provided {@code constructor}
     * function that accepts a {@link Model}. Computed knowledge indexes are
     * cached and returned on subsequent retrievals.
     *
     * @param type Type of knowledge index to retrieve.
     * @param constructor The method used to create {@code type}.
     * @param <T> The type of knowledge index to retrieve.
     * @return Returns the computed knowledge index.
     */
    @SuppressWarnings("unchecked")
    public <T extends KnowledgeIndex> T getKnowledge(Class<T> type, Function<Model, T> constructor) {
        return (T) blackboard.computeIfAbsent(type, t -> constructor.apply(this));
    }

    /**
     * Builder used to create a Model.
     */
    public static final class Builder implements SmithyBuilder<Model> {
        private final Map<String, Node> metadata = new HashMap<>();
        private final Map<ShapeId, Shape> shapeMap = new HashMap<>();

        private Builder() {}

        public Builder metadata(Map<String, Node> metadata) {
            clearMetadata();
            this.metadata.putAll(metadata);
            return this;
        }

        public Builder putMetadataProperty(String key, Node value) {
            metadata.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
            return this;
        }

        public Builder clearMetadata() {
            metadata.clear();
            return this;
        }

        /**
         * Add a shape to the builder.
         *
         * <p>{@link MemberShape} shapes are not added to the model directly.
         * They must be added by adding their containing shapes (e.g., to add a
         * list member, you must add the list shape that contains it). Any member
         * shape provided to any of the methods used to add shapes to the
         * model are ignored.
         *
         * @param shape Shape to add.
         * @return Returns the builder.
         */
        public Builder addShape(Shape shape) {
            // Members must be added by their containing shapes.
            if (!shape.isMemberShape()) {
                shapeMap.put(shape.getId(), shape);
                // Automatically add members of the shape.
                for (MemberShape memberShape : shape.members()) {
                    shapeMap.put(memberShape.getId(), memberShape);
                }
            }

            return this;
        }

        /**
         * Adds the shapes of another model to the builder.
         *
         * @param model Model to add shapes from.
         * @return Returns the builder.
         */
        public Builder addShapes(Model model) {
            shapeMap.putAll(model.shapeMap);
            return this;
        }

        /**
         * Adds a collection of shapes to the builder.
         *
         * @param shapes Collection of Shapes to add.
         * @param <S> Type of shape being added.
         * @return Returns the builder.
         */
        public <S extends Shape> Builder addShapes(Collection<S> shapes) {
            for (Shape shape : shapes) {
                addShape(shape);
            }
            return this;
        }

        /**
         * Adds a variadic list of shapes.
         *
         * @param shapes Shapes to add.
         * @return Returns the builder.
         */
        public Builder addShapes(Shape... shapes) {
            for (Shape shape : shapes) {
                addShape(shape);
            }
            return this;
        }

        /**
         * Removes a shape from the builder by ID.
         *
         * <p>Members of shapes are automatically removed when their
         * containing shape is removed.
         *
         * @param shapeId Shape to remove.
         * @return Returns the builder.
         */
        public Builder removeShape(ShapeId shapeId) {
            if (shapeMap.containsKey(shapeId)) {
                Shape previous = shapeMap.get(shapeId);
                shapeMap.remove(shapeId);

                // Automatically remove any members contained in the shape.
                for (MemberShape memberShape : previous.members()) {
                    shapeMap.remove(memberShape.getId());
                }
            }

            return this;
        }

        @Override
        public Model build() {
            return new Model(this);
        }
    }

    private static final class TraitCache {
        private final Map<ShapeId, Set<Shape>> traitIdsToShapes = new HashMap<>();
        private final Map<Class<? extends Trait>, Set<Shape>> traitsToShapes = new HashMap<>();

        TraitCache(Collection<Shape> shapes) {
            for (Shape shape : shapes) {
                for (Trait trait : shape.getAllTraits().values()) {
                    traitIdsToShapes.computeIfAbsent(trait.toShapeId(), id -> new HashSet<>()).add(shape);
                    traitsToShapes.computeIfAbsent(trait.getClass(), id -> new HashSet<>()).add(shape);
                }
            }
        }
    }
}
