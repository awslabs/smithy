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

package software.amazon.smithy.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Immutable Set utilities to polyfill Java 9+ features.
 */
public final class SetUtils {
    private SetUtils() {}

    /**
     * Creates an immutable copy of the given set.
     *
     * @param values The collection to make an immutable set of.
     * @param <T> the Set's value type.
     * @return An immutable Set copy.
     */
    public static <T> Set<T> copyOf(Collection<? extends T> values) {
        return values.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(values));
    }

    /**
     * Returns an unmodifiable set containing zero entries.
     *
     * @param <T> the Set's value type.
     * @return an empty Set.
     */
    public static <T> Set<T> of() {
        return Collections.emptySet();
    }

    /**
     * Returns an unmodifiable set containing a single entry.
     *
     * @param value the Set's value.
     * @param <T> the Set's value type.
     * @return a Set containing the specified value.
     * @throws NullPointerException if the value is {@code null}.
     */
    public static <T> Set<T> of(T value) {
        return Collections.singleton(value);
    }

    /**
     * Returns an unmodifiable set containing any number of entries.
     *
     * @param values the Set's values.
     * @param <T> the Set's value type.
     * @return a Set containing the specified values.
     * @throws IllegalArgumentException if any of the values is a duplicate.
     * @throws NullPointerException if any value is {@code null}.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> Set<T> of(T... values) {
        HashSet<T> result = new HashSet<>(values.length);
        Collections.addAll(result, values);
        return Collections.unmodifiableSet(result);
    }

    /**
     * Creates a collector that collects into an unmodifiable set.
     *
     * <p>This is a polyfill equivalent of Java 10's
     * {@code Collectors#toUnmodifiableSet}.
     *
     * @param <T> the Set's value type.
     * @return a Collector that accumulates the entries into an unmodifiable Set.
     */
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet);
    }
}
