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

package software.amazon.smithy.model.selector;

import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Matches a set of shapes using a selector expression.
 */
@FunctionalInterface
public interface Selector {
    /** A selector that always returns all provided values. */
    Selector IDENTITY = (visitor, shapes) -> shapes;

    /**
     * Matches a selector to a set of shapes.
     *
     * @param neighborProvider Provides neighbors for shapes.
     * @param shapes Matching context of shapes.
     * @return Returns the matching shapes.
     */
    Set<Shape> select(NeighborProvider neighborProvider, Set<Shape> shapes);

    /**
     * Matches a selector against a model using a custom neighbor visitor.
     *
     * @param neighborProvider Provides neighbors for shapes
     * @param model Model to query.
     * @return Returns the matching shapes.
     */
    default Set<Shape> select(NeighborProvider neighborProvider, Model model) {
        return select(neighborProvider, model.toSet());
    }

    /**
     * Matches a selector against a model.
     *
     * @param model Model to query.
     * @return Returns the matching shapes.
     */
    default Set<Shape> select(Model model) {
        return select(NeighborProvider.of(model), model);
    }

    /**
     * Parses a selector expression.
     *
     * @param expression Expression to parse.
     * @return Returns the parsed {@link Selector}.
     */
    static Selector parse(String expression) {
        return Parser.parse(expression);
    }
}
