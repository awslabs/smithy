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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates traits that can only be applied to a single structure member.
 */
public class ExclusiveStructureMemberTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Find all traits that are exclusive by member and target.
        Set<ShapeId> exclusiveMemberTraits = new HashSet<>();
        Set<ShapeId> exclusiveTargetTraits = new HashSet<>();
        for (Map.Entry<Shape, TraitDefinition> entry : model.getTraitDefinitions().entrySet()) {
            if (entry.getValue().isStructurallyExclusiveByTarget()) {
                exclusiveTargetTraits.add(entry.getKey().getId());
            } else if (entry.getValue().isStructurallyExclusiveByMember()) {
                exclusiveMemberTraits.add(entry.getKey().getId());
            }
        }

        List<ValidationEvent> events = new ArrayList<>();
        model.shapes(StructureShape.class).forEach(shape -> {
            validateExclusiveMembers(shape, exclusiveMemberTraits, events);
            validateExclusiveTargets(model, shape, exclusiveTargetTraits, events);
        });

        return events;
    }

    private void validateExclusiveMembers(
            StructureShape shape,
            Set<ShapeId> exclusiveMemberTraits,
            List<ValidationEvent> events
    ) {
        for (ShapeId traitId : exclusiveMemberTraits) {
            List<String> matches = shape.getAllMembers().values().stream()
                    .filter(member -> member.findTrait(traitId).isPresent())
                    .map(MemberShape::getMemberName)
                    .collect(Collectors.toList());

            if (matches.size() > 1) {
                events.add(error(shape, String.format(
                        "The `%s` trait can be applied to only a single member of a structure, but it was found on "
                        + "the following members: %s",
                        Trait.getIdiomaticTraitName(traitId),
                        ValidationUtils.tickedList(matches))));
            }
        }
    }

    private List<ValidationEvent> validateExclusiveTargets(
            Model model,
            StructureShape shape,
            Set<ShapeId> exclusiveTargets,
            List<ValidationEvent> events
    ) {
        // Find all member targets that violate the exclusion rule (e.g., streaming trait).
        for (ShapeId id : exclusiveTargets) {
            List<String> matches = shape.getAllMembers().values().stream()
                    .filter(member -> memberTargetHasTrait(model, member, id))
                    .map(MemberShape::getMemberName)
                    .collect(Collectors.toList());

            if (matches.size() > 1) {
                events.add(error(shape, String.format(
                        "Only a single member of a structure can target a shape marked with the `%s` trait, "
                        + "but it was found on the following members: %s",
                        Trait.getIdiomaticTraitName(id),
                        ValidationUtils.tickedList(matches))));
            }
        }

        return events;
    }

    private boolean memberTargetHasTrait(Model model, MemberShape member, ShapeId trait) {
        return model.getShape(member.getTarget())
                .flatMap(target -> target.findTrait(trait))
                .isPresent();
    }
}
