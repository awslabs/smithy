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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

public class ExcludeTraitsTest {

    @Test
    public void removesTraitsInList() {
        StringShape stringShape = StringShape.builder()
                .id("ns.foo#baz")
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .addTrait(new DocumentationTrait("docs", SourceLocation.NONE))
                .build();
        Model model = Model.assembler()
                .addShape(stringShape)
                .assemble()
                .unwrap();
        Model result = new ExcludeTraits()
                .createTransformer(Collections.singletonList("documentation"))
                .apply(ModelTransformer.create(), model);

        assertThat(result.getShapeIndex().getShape(ShapeId.from("ns.foo#baz")).get().getTrait(DocumentationTrait.class),
                   is(Optional.empty()));
        assertThat(result.getShapeIndex().getShape(ShapeId.from("ns.foo#baz")).get().getTrait(SensitiveTrait.class),
                   not(Optional.empty()));
        assertFalse(result.getTraitDefinition("smithy.api#documentation").isPresent());
    }
}
