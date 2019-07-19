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

package software.amazon.smithy.aws.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class ServiceTraitTest {

    @Test
    public void loadsTraitWithString() {
        Node node = Node.parse("{\"sdkServiceId\": \"Foo\"}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ServiceTrait.ID, ShapeId.from("ns.foo#Foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ServiceTrait.class));
        ServiceTrait serviceTrait = (ServiceTrait) trait.get();
        assertThat(serviceTrait.getSdkId(), equalTo("Foo"));
        assertThat(serviceTrait.getCloudFormationName(), equalTo("Foo"));
        assertThat(serviceTrait.getArnNamespace(), equalTo("foo"));
        assertThat(serviceTrait.getCloudTrailEventSource(), equalTo("foo.amazonaws.com"));
        assertThat(serviceTrait.toBuilder().build(), equalTo(serviceTrait));
    }

    @Test
    public void loadsTraitWithOptionalValues() {
        Node node = Node.parse("{\"sdkServiceId\": \"Foo\", \"arnService\": \"service\", \"productName\": \"Baz\"}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ServiceTrait.ID, ShapeId.from("ns.foo#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ServiceTrait.class));
        ServiceTrait serviceTrait = (ServiceTrait) trait.get();
        assertThat(serviceTrait.getSdkId(), equalTo("Foo"));
        assertThat(serviceTrait.getArnNamespace(), equalTo("service"));
        assertThat(serviceTrait.getCloudFormationName(), equalTo("Baz"));
    }

    @Test
    public void loadsEventSource() {
        Node node = Node.parse("{\"sdkServiceId\": \"Foo\", \"cloudTrailEventSource\": \"foo.amazonaws.com\"}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ServiceTrait.ID, ShapeId.from("ns.foo#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ServiceTrait.class));
        ServiceTrait serviceTrait = (ServiceTrait) trait.get();
        assertThat(serviceTrait.getCloudTrailEventSource(), equalTo("foo.amazonaws.com"));
    }

    @Test
    public void requiresSdkServiceId() {
        assertThrows(IllegalStateException.class, () -> ServiceTrait.builder().build());
    }

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();
        ServiceShape service = result.getShapeIndex()
                .getShape(ShapeId.from("ns.foo#SomeService")).get()
                .asServiceShape().get();
        ServiceTrait trait = service.getTrait(ServiceTrait.class).get();

        assertThat(trait.getSdkId(), equalTo("Some Value"));
        assertThat(trait.getCloudFormationName(), equalTo("SomeService"));
        assertThat(trait.getArnNamespace(), equalTo("service"));
    }
}
