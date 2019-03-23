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

package software.amazon.smithy.model.validation.nodevalidation;

import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.Trait;

abstract class MemberAndShapeTraitPlugin<S extends Shape, N extends Node, T extends Trait>
        implements NodeValidatorPlugin {

    private final Class<S> targetShapeClass;
    private final Class<N> nodeClass;
    private final Class<T> traitClass;

    MemberAndShapeTraitPlugin(Class<S> targetShapeClass, Class<N> nodeClass, Class<T> traitClass) {
        this.targetShapeClass = targetShapeClass;
        this.nodeClass = nodeClass;
        this.traitClass = traitClass;
    }

    @SuppressWarnings("unchecked")
    public final List<String> apply(Shape shape, Node value, ShapeIndex index) {
        if (nodeClass.isInstance(value)
                && shape.getTrait(traitClass).isPresent()
                && isMatchingShape(shape, index)) {
            return check(shape, shape.getTrait(traitClass).get(), (N) value, index);
        }

        return List.of();
    }

    private boolean isMatchingShape(Shape shape, ShapeIndex index) {
        // Is the shape the expected shape type?
        if (targetShapeClass.isInstance(shape)) {
            return true;
        }

        // Is the targeted member an instance of the expected shape?
        return shape.asMemberShape()
                .flatMap(member -> index.getShape(member.getTarget()))
                .filter(targetShapeClass::isInstance)
                .isPresent();
    }

    protected abstract List<String> check(Shape shape, T trait, N value, ShapeIndex index);
}
