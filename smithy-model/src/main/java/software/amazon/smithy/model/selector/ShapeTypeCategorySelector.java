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
import java.util.stream.Collectors;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.shapes.Shape;

final class ShapeTypeCategorySelector implements Selector {
    private final Class<? extends Shape> shapeCategory;

    ShapeTypeCategorySelector(Class<? extends Shape> shapeCategory) {
        this.shapeCategory = shapeCategory;
    }

    @Override
    public Set<Shape> select(NeighborProvider neighborProvider, Set<Shape> shapes) {
        return shapes.stream().filter(shapeCategory::isInstance).collect(Collectors.toSet());
    }
}
