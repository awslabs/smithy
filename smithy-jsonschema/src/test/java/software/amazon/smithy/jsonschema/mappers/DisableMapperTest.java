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

package software.amazon.smithy.jsonschema.mappers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.jsonschema.JsonSchemaConstants;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.StringShape;

public class DisableMapperTest {
    @Test
    public void removesDisabledKeywords() {
        var shape = StringShape.builder().id("smithy.example#String").build();
        var builder = Schema.builder().type("string").format("foo");
        var config = Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.DISABLE_FORMAT, true)
                .build();
        var schema = new DisableMapper().updateSchema(shape, builder, config).build();

        assertThat(schema.getType().get(), equalTo("string"));
        assertTrue(schema.getFormat().isEmpty());
    }
}
