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

package software.amazon.smithy.aws.apigateway.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class AddRequestValidatorsTest {
    @Test
    public void addsRequestValidators() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("request-validators.json"))
                .assemble()
                .unwrap();

        var result = OpenApiConverter.create()
                .classLoader(getClass().getClassLoader())
                .convert(model, ShapeId.from("smithy.example#Service"));

        assertThat(result.getExtension("x-amazon-apigateway-request-validator").get(), equalTo(Node.from("full")));
        var validators = result.getExtension("x-amazon-apigateway-request-validators").get().expectObjectNode();
        assertTrue(validators.containsMember("body-only"));
        assertTrue(validators.containsMember("full"));
        assertFalse(validators.containsMember("params-only"));

        var operation1Val = result.getPaths().get("/1").getPut().get()
                .getExtension("x-amazon-apigateway-request-validator");
        assertTrue(operation1Val.isEmpty());

        var operation2Val = result.getPaths().get("/2").getPut().get()
                .getExtension("x-amazon-apigateway-request-validator");
        assertThat(operation2Val.get(), equalTo(Node.from("body-only")));
    }
}
