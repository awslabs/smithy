/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * When the `httpQueryParams` trait is used, this validator emits a NOTE when another member of the container shape
 * applies the `httpQuery` trait which may result in a conflict within the query string.
 */
public final class HttpQueryParamsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(HttpQueryParamsTrait.class)) {
            return Collections.emptyList();
        } else {
            return validateQueryTraitUsage(model);
        }
    }

    private List<ValidationEvent> validateQueryTraitUsage(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Shape shape : model.getShapesWithTrait(HttpQueryParamsTrait.class)) {
            shape.asMemberShape().flatMap(member -> model.getShape(member.getContainer())
                .flatMap(Shape::asStructureShape))
                .ifPresent(structure -> {
                    // Gather the names of member shapes, as strings, that apply HttpQuery traits
                    List<String> queryShapes = getMembersWithTrait(structure, HttpQueryTrait.class);
                    if (queryShapes.size() > 0) {
                        events.add(createNote(structure, shape.toShapeId().getMember().get(), queryShapes));
                    }
                });
        }

        return events;
    }

    private List<String> getMembersWithTrait(StructureShape structure, Class<? extends Trait> trait) {
        List<String> members = new ArrayList<>();
        for (MemberShape member : structure.members()) {
            if (member.hasTrait(trait)) {
                members.add(member.getMemberName());
            }
        }
        return members;
    }

    private ValidationEvent createNote(Shape target, String queryParamsShape, List<String> queryShapes) {
        return note(target, String.format("Operation input `%s` has an `httpQueryParams` trait applied to the `%s` "
                + "member and `httpQuery` traits applied to the following members: %s. This can cause confusion when "
                + "keys from the `httpQueryParams` trait conflict with those defined directly by `httpQuery` traits",
                target.toShapeId(), queryParamsShape, ValidationUtils.tickedList(queryShapes)));
    }
}
