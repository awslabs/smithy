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

package software.amazon.smithy.model.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

public class ValidationEventTest {

    @Test
    public void requiresMessage() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ValidationEvent.builder()
                    .severity(Severity.ERROR)
                    .eventId("foo")
                    .build();
        });
    }

    @Test
    public void requiresSeverity() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ValidationEvent.builder()
                    .message("test")
                    .eventId("foo")
                    .build();
        });
    }

    @Test
    public void requiresEventId() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ValidationEvent.builder()
                    .severity(Severity.ERROR)
                    .message("test")
                    .build();
        });
    }

    @Test
    public void suppressionIsOnlyValidWithSuppress() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ValidationEvent.builder()
                    .severity(Severity.ERROR)
                    .message("test")
                    .eventId("foo")
                    .suppressionReason("Some reason")
                    .build();
        });
    }

    @Test
    public void hasGetters() {
        ShapeId id = ShapeId.from("ns.foo#baz");
        ValidationEvent event = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(id)
                .eventId("abc.foo")
                .suppressionReason("my reason")
                .build();

        assertThat(event.getSeverity(), equalTo(Severity.SUPPRESSED));
        assertThat(event.getMessage(), equalTo("The message"));
        assertThat(event.getEventId(), equalTo("abc.foo"));
        assertThat(event.getSuppressionReason().get(), equalTo("my reason"));
        assertThat(event.getShapeId().get(), is(id));
        assertThat(event.getSeverity(), is(Severity.SUPPRESSED));
    }

    @Test
    public void usesShapeSourceWhenPresent() {
        StringShape stringShape = StringShape.builder().id("ns.foo#bar").source("file", 1, 2).build();
        ValidationEvent event = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.ERROR)
                .shape(stringShape)
                .eventId("abc.foo")
                .build();

        assertThat(event.getSourceLocation(), is(stringShape.getSourceLocation()));
    }

    @Test
    public void usesEmptyLocationWhenNoneSet() {
        ShapeId id = ShapeId.from("ns.foo#baz");
        ValidationEvent event = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(id)
                .eventId("abc.foo")
                .suppressionReason("my reason")
                .build();

        assertThat(event.getSourceLocation(), is(SourceLocation.none()));
    }

    @Test
    public void createsEventBuilderFromEvent() {
        ValidationEvent event = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#baz"))
                .eventId("abc.foo")
                .suppressionReason("my reason")
                .build();
        ValidationEvent other = event.toBuilder().build();

        assertThat(event, equalTo(other));
    }

    @Test
    public void sameInstanceIsEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .eventId("abc.foo")
                .build();

        assertEquals(a, a);
    }

    @Test
    public void differentTypesAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .eventId("abc.foo")
                .build();

        assertNotEquals(a, "test");
    }

    @Test
    public void differentMessagesAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .eventId("abc.foo")
                .build();
        ValidationEvent b = a.toBuilder().message("other message").build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentSeveritiesAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .eventId("abc.foo")
                .build();
        ValidationEvent b = a.toBuilder().severity(Severity.ERROR).build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentSourceLocationsAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .eventId("abc.foo")
                .suppressionReason("my reason")
                .sourceLocation(SourceLocation.none())
                .build();
        ValidationEvent b = a.toBuilder().sourceLocation(new SourceLocation("foo", 10, 0)).build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentShapeIdAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .eventId("abc.foo")
                .suppressionReason("my reason")
                .sourceLocation(SourceLocation.none())
                .build();
        ValidationEvent b = a.toBuilder().shapeId(ShapeId.from("ns.foo#qux")).build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentEventIdAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .eventId("abc.foo")
                .suppressionReason("my reason")
                .sourceLocation(SourceLocation.none())
                .build();
        ValidationEvent b = a.toBuilder().eventId("other.id").build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentSuppressionReasonAreNotEqual() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .shapeId(ShapeId.from("ns.foo#bar"))
                .eventId("abc.foo")
                .suppressionReason("my reason")
                .sourceLocation(SourceLocation.none())
                .build();
        ValidationEvent b = a.toBuilder().suppressionReason("other reason").build();

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void toStringContainsSeverityAndEventId() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .eventId("abc.foo")
                .build();

        assertEquals(a.toString(), "[SUPPRESSED] (abc.foo) - N/A [0, 0]: The message");
    }

    @Test
    public void toStringContainsShapeId() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .eventId("abc.foo")
                .shapeId(ShapeId.from("ns.foo#baz"))
                .build();

        assertEquals(a.toString(), "[SUPPRESSED] (abc.foo) ns.foo#baz N/A [0, 0]: The message");
    }

    @Test
    public void toStringContainsSourceLocation() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .eventId("abc.foo")
                .shapeId(ShapeId.from("ns.foo#baz"))
                .sourceLocation(new SourceLocation("file", 1, 2))
                .build();

        assertEquals(a.toString(), "[SUPPRESSED] (abc.foo) ns.foo#baz file [1, 2]: The message");
    }

    @Test
    public void convertsToNode() {
        ValidationEvent a = ValidationEvent.builder()
                .message("The message")
                .severity(Severity.SUPPRESSED)
                .eventId("abc.foo")
                .shapeId(ShapeId.from("ns.foo#baz"))
                .sourceLocation(new SourceLocation("file", 1, 2))
                .build();

        ObjectNode result = a.toNode().expectObjectNode();
        assertEquals(result.getMember("id").get().asStringNode().get().getValue(), "abc.foo");
        assertEquals(result.getMember("shapeId").get().asStringNode().get().getValue(), "ns.foo#baz");
        assertEquals(result.getMember("filename").get().asStringNode().get().getValue(), "file");
        assertEquals(result.getMember("message").get().asStringNode().get().getValue(), "The message");
    }
}
