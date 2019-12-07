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

package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that the "id" property of {@code smithy.test#httpRequestTests}
 * and {@code smithy.test#httpResponseTests} are unique across all test
 * cases.
 */
public class UniqueProtocolTestCaseIdValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        Map<String, List<Shape>> requestIdsToTraits = new TreeMap<>();
        Map<String, List<Shape>> responseIdsToTraits = new TreeMap<>();

        Stream.concat(model.shapes(OperationShape.class), model.shapes(StructureShape.class)).forEach(shape -> {
            shape.getTrait(HttpRequestTestsTrait.class)
                    .ifPresent(trait -> addTestCaseIdsToMap(shape, trait.getTestCases(), requestIdsToTraits));
            shape.getTrait(HttpResponseTestsTrait.class)
                    .ifPresent(trait -> addTestCaseIdsToMap(shape, trait.getTestCases(), responseIdsToTraits));
        });

        removeEntriesWithSingleValue(requestIdsToTraits);
        removeEntriesWithSingleValue(responseIdsToTraits);

        return collectEvents(requestIdsToTraits, responseIdsToTraits);
    }

    private void addTestCaseIdsToMap(
            Shape shape,
            List<? extends HttpMessageTestCase> testCases,
            Map<String, List<Shape>> map
    ) {
        for (HttpMessageTestCase testCase : testCases) {
            map.computeIfAbsent(testCase.getId(), id -> new ArrayList<>()).add(shape);
        }
    }

    private void removeEntriesWithSingleValue(Map<String, List<Shape>> map) {
        map.keySet().removeIf(key -> map.get(key).size() == 1);
    }

    private List<ValidationEvent> collectEvents(
            Map<String, List<Shape>> requestIdsToTraits,
            Map<String, List<Shape>> responseIdsToTraits
    ) {
        if (requestIdsToTraits.isEmpty() && responseIdsToTraits.isEmpty()) {
            return Collections.emptyList();
        }

        List<ValidationEvent> mutableEvents = new ArrayList<>();
        addValidationEvents(requestIdsToTraits, mutableEvents, HttpRequestTestsTrait.ID);
        addValidationEvents(responseIdsToTraits, mutableEvents, HttpResponseTestsTrait.ID);
        return mutableEvents;
    }

    private void addValidationEvents(
            Map<String, List<Shape>> conflicts,
            List<ValidationEvent> mutableEvents,
            ShapeId trait
    ) {
        for (Map.Entry<String, List<Shape>> entry : conflicts.entrySet()) {
            for (Shape shape : entry.getValue()) {
                mutableEvents.add(error(shape, String.format(
                        "Conflicting `%s` test case IDs found for ID `%s`: %s",
                        trait, entry.getKey(),
                        ValidationUtils.tickedList(entry.getValue().stream().map(Shape::getId)))));
            }
        }
    }
}
