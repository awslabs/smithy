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

package software.amazon.smithy.build.transforms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ExcludeTraitsByTagTest {
    @Test
    public void removesTraitsByTagInList() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("tree-shaking-traits.json").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("tags", Node.fromStrings("qux")))
                .build();
        Model result = new ExcludeTraitsByTag().transform(context);
        Set<ShapeId> traits = result.getTraitShapes().stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());

        assertFalse(traits.contains(ShapeId.from("ns.foo#quux")));
        assertTrue(traits.contains(ShapeId.from("ns.foo#bar")));
    }
}
