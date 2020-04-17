package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class RecursiveNeighborSelectorTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(NeighborSelectorTest.class.getResource("neighbor-test.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    private Set<String> selectIds(String expression) {
        return Selector.parse(expression)
                .select(model)
                .stream()
                .map(Shape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toSet());
    }

    @Test
    public void findsClosure() {
        Set<String> result = selectIds("service[id=smithy.example#MyService2] ~> *");

        assertThat(result, containsInAnyOrder(
                "smithy.example#Input",
                "smithy.example#Output$foo",
                "smithy.example#Error$foo",
                "smithy.example#Operation",
                "smithy.example#Error",
                "smithy.api#String",
                "smithy.example#Input$foo",
                "smithy.example#Output"
        ));
    }

    @Test
    public void findsEmptyClosure() {
        Set<String> result = selectIds("service[id=smithy.example#MyService1] ~> *");

        assertThat(result, empty());
    }
}
