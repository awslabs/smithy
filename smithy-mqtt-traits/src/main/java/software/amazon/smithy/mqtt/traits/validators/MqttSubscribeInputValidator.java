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

package software.amazon.smithy.mqtt.traits.validators;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.mqtt.traits.SubscribeTrait;
import software.amazon.smithy.mqtt.traits.TopicLabelTrait;

/**
 * Validates {@code mqttSubscribe} operation input.
 *
 * <ul>
 *     <li>Subscribe operation input members must all have mqttLabel trait.</li>
 * </ul>
 */
public final class MqttSubscribeInputValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex index = model.getShapeIndex();
        return index.shapes(OperationShape.class)
                .filter(shape -> shape.hasTrait(SubscribeTrait.class))
                .flatMap(shape -> validateOperation(index, shape))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateOperation(ShapeIndex index, OperationShape operation) {
        return operation.getInput()
                .flatMap(index::getShape)
                .flatMap(Shape::asStructureShape).stream().flatMap(input -> input.getAllMembers().values().stream()
                        .filter(member -> !member.hasTrait(TopicLabelTrait.class))
                        .map(member -> error(member, String.format(
                                "All input members of an operation marked with the `mqttSubscribe` trait "
                                + "must be marked with the `mqttTopicLabel` trait, and this member is used "
                                + "as part of the input of the `%s` operation.", operation.getId()))));
    }
}
