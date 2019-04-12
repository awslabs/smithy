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

package software.amazon.smithy.model.validation.builtins;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.UriPattern;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates that httpLabel traits are applied correctly for operation inputs.
 *
 * <ul>
 *     <li>Validates that if an operation has labels then it must have
 *     input.</li>
 *     <li>Validates that a corresponding input member can be found for each
 *     label in each operation.</li>
 *     <li>Validates that the correct target type is used for greedy and
 *     non-greedy labels.</li>
 *     <li>Validates that all labels in the URI of each operation that
 *     references the structure, have a corresponding member with the
 *     httpLabel trait.</li>
 * </ul>
 */
public final class HttpLabelTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Validate all operation shapes with the `http` trait.
        return model.getShapeIndex().shapes(OperationShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, HttpTrait.class))
                .flatMap(pair -> validateStructure(model.getShapeIndex(), pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateStructure(ShapeIndex index, OperationShape operation, HttpTrait http) {
        // If the operation has labels then it must also have input.
        if (operation.getInput().isEmpty() && !http.getUri().getLabels().isEmpty()) {
            return ListUtils.of(error(operation, http, String.format(
                    "`http` trait uri contains labels (%s), but operation has no input.",
                    ValidationUtils.tickedList(http.getUri().getLabels().stream()
                                       .map(UriPattern.Segment::getContent).collect(Collectors.toSet())))));
        }

        // Only continue validating if the input is a structure. Typing
        // validation of the input is handled elsewhere.
        return operation.getInput().flatMap(index::getShape).flatMap(Shape::asStructureShape)
                .map(input -> validateBindings(index, operation, http, input))
                .orElse(ListUtils.of());
    }

    private List<ValidationEvent> validateBindings(
            ShapeIndex index,
            OperationShape operation,
            HttpTrait http,
            StructureShape input
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        // Create a set of labels and remove from the set when a match is
        // found. If any labels remain after looking at all members, then
        // there are unmatched labels.
        Set<String> labels = http.getUri().getLabels().stream()
                .map(UriPattern.Segment::getContent)
                .collect(Collectors.toSet());

        input.getAllMembers().values().stream()
                // Only look at members with the `httpLabel` trait.
                .flatMap(member -> Trait.flatMapStream(member, HttpLabelTrait.class))
                .forEach(pair -> {
                    MemberShape member = pair.getLeft();
                    HttpLabelTrait trait = pair.getRight();
                    labels.remove(member.getMemberName());
                    if (member.isOptional()) {
                        events.add(error(member, trait, "Members with the `httpLabel` trait must be required."));
                    }

                    // Emit an error if the member is not a valid label.
                    if (http.getUri().getLabel(member.getMemberName()).isEmpty()) {
                        events.add(error(member, trait, format(
                                "This `%s` structure member is marked with the `httpLabel` trait, but no "
                                + "corresponding `http` URI label could be found when used as the input of "
                                + "the `%s` operation.", member.getMemberName(), operation.getId())));
                    } else if (http.getUri().getLabel(member.getMemberName()).get().isGreedyLabel()) {
                        index.getShape(member.getTarget()).ifPresent(target -> {
                            // Greedy labels must be strings.
                            if (!target.isStringShape()) {
                                events.add(error(member, trait, format(
                                        "The `%s` structure member corresponds to a greedy label when used as the "
                                        + "input of the `%s` operation. This member targets %s, but greedy labels "
                                        + "must target string shapes.",
                                        member.getMemberName(), operation.getId(), target)));
                            }
                        });
                    }
                });

        if (!labels.isEmpty()) {
            events.add(error(input, String.format(
                    "This structure is used as the input for the `%s` operation, but the following URI labels "
                    + "found in the operation's `http` trait do not have a corresponding member marked with the "
                    + "`httpLabel` trait: %s", operation.getId(), ValidationUtils.tickedList(labels))));
        }

        return events;
    }
}
