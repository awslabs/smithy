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

package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;

public class SmithyModelLoaderTest {
    @Test
    public void loadsAppropriateSourceLocations() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/main-test.smithy"))
                .assemble()
                .unwrap();

        model.getShapeIndex().shapes().forEach(shape -> {
            if (!Prelude.isPreludeShape(shape.getId())) {
                assertThat(shape.getSourceLocation(), not(equalTo(SourceLocation.NONE)));
            }

            // Non-member shapes defined in the main-test.smithy file should
            // all have a source location column of 1. The endsWith check is
            // necessary to filter out the prelude.
            if (shape.getSourceLocation().getFilename().endsWith("main-test.smithy") && !shape.isMemberShape()) {
                assertThat(shape.getSourceLocation().getColumn(), equalTo(1));
            }
        });
    }

    @Test
    public void fallsBackToPublicPreludeShapes() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/forward-reference-resolver.smithy"))
                .assemble()
                .unwrap();

        MemberShape baz = model.getShapeIndex()
                .getShape(ShapeId.from("smithy.example#Foo$baz")).get()
                .asMemberShape().get();
        MemberShape bar = model.getShapeIndex()
                .getShape(ShapeId.from("smithy.example#Foo$bar")).get()
                .asMemberShape().get();
        ResourceShape resource = model.getShapeIndex().getShape(ShapeId.from("smithy.example#MyResource")).get()
                .asResourceShape().get();

        assertThat(baz.getTarget().toString(), equalTo("smithy.api#String"));
        assertThat(bar.getTarget().toString(), equalTo("smithy.example#Integer"));
        assertThat(resource.getIdentifiers().get("a"), equalTo(ShapeId.from("smithy.example#MyString")));
        assertThat(resource.getIdentifiers().get("b"), equalTo(ShapeId.from("smithy.example#AnotherString")));
        assertThat(resource.getIdentifiers().get("c"), equalTo(ShapeId.from("smithy.api#String")));
    }

    @Test
    public void loadsTraitDefinitions() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/trait-definitions.smithy"))
                .assemble()
                .unwrap();

        assertTrue(model.getTraitDefinition("example.namespace#customTrait").isPresent());
        assertTrue(model.getTraitDefinition("example.namespace#documentation").isPresent());
        assertTrue(model.getTraitDefinition("example.namespace#numeric").isPresent());
        assertThat(model.getTraitDefinition("example.namespace#numeric").get().getShape().get(),
                   equalTo(ShapeId.from("smithy.api#Integer")));
    }

    @Test
    public void canLoadAndAliasShapesAndTraits() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("first-namespace.smithy"))
                .addImport(getClass().getResource("second-namespace.smithy"))
                .assemble()
                .unwrap();

        System.out.println(Node.prettyPrintJson(ModelSerializer.builder().build().serialize(model)));
    }
}
