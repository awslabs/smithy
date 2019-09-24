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

package software.amazon.smithy.model.knowledge;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.EventStreamTrait;

/**
 * Index of operation shapes to event stream information.
 *
 * <p>This knowledge index provides information about event streams in
 * operations, including the input/output member that contains an
 * event stream, the shape targeted by this member, and any additional
 * input/output members that form the initial message.
 */
public final class EventStreamIndex implements KnowledgeIndex {
    private static final Logger LOGGER = Logger.getLogger(EventStreamIndex.class.getName());

    private final Map<ShapeId, EventStreamInfo> inputInfo = new HashMap<>();
    private final Map<ShapeId, EventStreamInfo> outputInfo = new HashMap<>();

    public EventStreamIndex(Model model) {
        ShapeIndex index = model.getShapeIndex();
        OperationIndex operationIndex = model.getKnowledge(OperationIndex.class);

        model.getShapeIndex().shapes(OperationShape.class).forEach(operation -> {
            operationIndex.getInput(operation).ifPresent(input -> {
                computeEvents(index, operation, input, inputInfo);
            });
            operationIndex.getOutput(operation).ifPresent(output -> {
                computeEvents(index, operation, output, outputInfo);
            });
        });
    }

    private void computeEvents(
            ShapeIndex index,
            OperationShape operation,
            StructureShape shape,
            Map<ShapeId, EventStreamInfo> infoMap
    ) {
        for (MemberShape member : shape.getAllMembers().values()) {
            if (member.hasTrait(EventStreamTrait.class)) {
                createEventStreamInfo(index, operation, shape, member).ifPresent(info -> {
                    infoMap.put(operation.getId(), info);
                });
            }
        }
    }

    /**
     * Get event stream information for the input of an operation.
     *
     * <p>No result is returned if the provided operation shape does not
     * exist, if the targeted shape is not an operation, if the operation
     * does not contain an input structure or if the input structure does
     * not contain an input event stream.
     *
     * @param operationShape Operation or shape ID to retrieve information for.
     * @return Returns the optionally found input event stream information.
     */
    public Optional<EventStreamInfo> getInputInfo(ToShapeId operationShape) {
        return Optional.ofNullable(inputInfo.get(operationShape.toShapeId()));
    }

    /**
     * Get event stream information for the output of an operation.
     *
     * <p>No result is returned if the provided operation shape does not
     * exist, if the targeted shape is not an operation, if the operation
     * does not contain an output structure or if the output structure does
     * not contain an output event stream.
     *
     * @param operationShape Operation or shape ID to retrieve information for.
     * @return Returns the optionally found output event stream information.
     */
    public Optional<EventStreamInfo> getOutputInfo(ToShapeId operationShape) {
        return Optional.ofNullable(outputInfo.get(operationShape.toShapeId()));
    }

    private Optional<EventStreamInfo> createEventStreamInfo(
            ShapeIndex index,
            OperationShape operation,
            StructureShape structure,
            MemberShape member
    ) {
        EventStreamTrait trait = member.getTrait(EventStreamTrait.class).get();

        Shape eventStreamTarget = index.getShape(member.getTarget()).orElse(null);
        if (eventStreamTarget == null) {
            LOGGER.severe(String.format(
                    "Skipping event stream info for %s because the %s member target %s does not exist",
                    operation.getId(), member.getMemberName(), member.getTarget()));
            return Optional.empty();
        }

        // Compute the events of the event stream.
        Map<String, StructureShape> events = new HashMap<>();
        if (eventStreamTarget.asUnionShape().isPresent()) {
            for (MemberShape unionMember : eventStreamTarget.asUnionShape().get().getAllMembers().values()) {
                index.getShape(unionMember.getTarget()).flatMap(Shape::asStructureShape).ifPresent(struct -> {
                    events.put(unionMember.getMemberName(), struct);
                });
            }
        } else if (eventStreamTarget.asStructureShape().isPresent()) {
            events.put(member.getMemberName(), eventStreamTarget.asStructureShape().get());
        } else {
            // If the event target is an invalid type, then we can't create the indexed result.
            LOGGER.severe(() -> String.format(
                    "Skipping event stream info for %s because the %s member target %s is not a structure or union",
                    operation.getId(), member.getMemberName(), member.getTarget()));
            return Optional.empty();
        }

        Map<String, MemberShape> initialMembers = new HashMap<>();
        Map<String, Shape> initialTargets = new HashMap<>();

        for (MemberShape structureMember : structure.getAllMembers().values()) {
            if (!structureMember.getMemberName().equals(member.getMemberName())) {
                index.getShape(structureMember.getTarget()).ifPresent(shapeTarget -> {
                    initialMembers.put(structureMember.getMemberName(), structureMember);
                    initialTargets.put(structureMember.getMemberName(), shapeTarget);
                });
            }
        }

        return Optional.of(new EventStreamInfo(
                operation, trait, structure,
                member, eventStreamTarget,
                initialMembers, initialTargets,
                events));
    }
}
