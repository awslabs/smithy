package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class AnnotationTraitTest {
    @Test
    public void loadsAnnotationTraitObjects() {
        SourceLocation sourceLocation = new SourceLocation("/foo");
        ObjectNode objectNode = new ObjectNode(Collections.emptyMap(), sourceLocation);
        SensitiveTrait trait = new SensitiveTrait.Provider().createTrait(ShapeId.from("foo#bar"), objectNode);

        assertThat(trait.getSourceLocation(), equalTo(sourceLocation));
    }

    @Test
    public void validatesAnnotationTraits() {
        SourceLocation sourceLocation = new SourceLocation("/foo");
        ArrayNode arrayNode = new ArrayNode(Collections.emptyList(), sourceLocation);

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            new SensitiveTrait.Provider().createTrait(ShapeId.from("foo#bar"), arrayNode);
        });
    }
}
