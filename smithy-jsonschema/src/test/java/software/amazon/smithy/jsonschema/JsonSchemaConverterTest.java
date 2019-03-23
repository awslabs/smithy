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

package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumConstantBody;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;

public class JsonSchemaConverterTest {
    @Test
    public void dealsWithRecursion() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("recursive.json"))
                .assemble()
                .unwrap();
        var document = JsonSchemaConverter.create().convert(model.getShapeIndex());

        assertThat(document.getDefinitions().keySet(), not(empty()));
    }

    @Test
    public void integrationTest() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-service.json"))
                .assemble()
                .unwrap();
        SchemaDocument document = JsonSchemaConverter.create().convert(model.getShapeIndex());

        assertThat(document.getDefinitions().keySet(), not(empty()));
    }

    @Test
    public void hasCopyConstructor() {
        Predicate<Shape> predicate = shape -> false;
        var config = Node.objectNodeBuilder().withMember("foo", "bar").build();
        PropertyNamingStrategy propertyStrategy = (container, member, conf) -> "a";
        RefStrategy refStrategy = (member, conf) -> "#/foo/bar";

        JsonSchemaConverter.create()
                .config(config)
                .propertyNamingStrategy(propertyStrategy)
                .refStrategy(refStrategy)
                .shapePredicate(predicate)
                .copy();
    }

    @Test
    public void canFilterShapesWithCustomPredicate() {
        Predicate<Shape> predicate = shape -> !shape.getId().getName().equals("Foo");
        var index = ShapeIndex.builder()
                .addShape(StringShape.builder().id("smithy.example#Foo").build())
                .addShape(StringShape.builder().id("smithy.example#Baz").build())
                .build();
        var doc = JsonSchemaConverter.create().shapePredicate(predicate).convert(index);

        assertTrue(doc.getDefinition("#/definitions/SmithyExampleFoo").isEmpty());
        assertTrue(doc.getDefinition("#/definitions/SmithyExampleBaz").isPresent());
    }

    @Test
    public void canUseCustomPropertyNamingStrategy() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        MemberShape member = MemberShape.builder().id("smithy.example#Foo$bar").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMember(member)
                .build();
        var index = ShapeIndex.builder().addShapes(struct, member, string).build();
        PropertyNamingStrategy strategy = (container, memberShape, conf) -> {
            return memberShape.getMemberName().toUpperCase(Locale.US);
        };
        var doc = JsonSchemaConverter.create().propertyNamingStrategy(strategy).convert(index);

        assertThat(doc.getDefinition("#/definitions/SmithyExampleFoo").get().getProperties().keySet(),
                   contains("BAR"));
    }

    @Test
    public void canUseCustomRefStrategy() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        MemberShape member = MemberShape.builder().id("smithy.example#Foo$bar").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMember(member)
                .build();
        var index = ShapeIndex.builder().addShapes(struct, member, string).build();
        RefStrategy strategy = (id, conf) -> "#/foo/" + id;
        var doc = JsonSchemaConverter.create().refStrategy(strategy).convert(index);

        assertThat(doc.getDefinitions().keySet(), containsInAnyOrder(
                "#/foo/smithy.example#Foo",
                "#/foo/smithy.example#Foo$bar",
                "#/foo/smithy.example#String"));
    }

    @Test
    public void canAddCustomSchemaMapper() {
        var index = ShapeIndex.builder().addShape(StringShape.builder().id("smithy.example#Foo").build()).build();
        SchemaBuilderMapper mapper = (shape, builder, conf) -> builder.putExtension("Hi", Node.from("There"));
        var doc = JsonSchemaConverter.create().addSchemaMapper(mapper).convert(index);

        assertTrue(doc.getDefinition("#/definitions/SmithyExampleFoo").isPresent());
        assertTrue(doc.getDefinition("#/definitions/SmithyExampleFoo").get().getExtension("Hi").isPresent());
    }

    @Test
    public void excludesServiceShapes() {
        var index = ShapeIndex.builder()
                .addShape(ServiceShape.builder().id("smithy.example#Service").version("X").build())
                .build();
        var doc = JsonSchemaConverter.create().convert(index);

        assertThat(doc.getDefinitions().keySet(), empty());
    }

    @Test
    public void excludesPrivateShapes() {
        var index = ShapeIndex.builder()
                .addShape(StringShape.builder().id("smithy.example#String").addTrait(new PrivateTrait()).build())
                .build();
        var doc = JsonSchemaConverter.create().convert(index);

        assertThat(doc.getDefinitions().keySet(), empty());
    }

    @Test
    public void excludesMembersOfPrivateShapes() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        MemberShape member = MemberShape.builder().id("smithy.example#Foo$bar").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMember(member)
                .addTrait(new PrivateTrait())
                .build();
        var index = ShapeIndex.builder().addShapes(struct, member, string).build();
        var doc = JsonSchemaConverter.create().convert(index);

        assertThat(doc.getDefinitions().keySet(), contains("#/definitions/SmithyExampleString"));
    }

    @Test
    public void canIncludePrivateShapesWithFlag() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        MemberShape member = MemberShape.builder().id("smithy.example#Foo$bar").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMember(member)
                .addTrait(new PrivateTrait())
                .build();
        var index = ShapeIndex.builder().addShapes(struct, member, string).build();
        var config = Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.SMITHY_INCLUDE_PRIVATE_SHAPES, true)
                .build();
        var doc = JsonSchemaConverter.create().config(config).convert(index);

        assertThat(doc.getDefinitions().keySet(), not(empty()));
        assertThat(doc.getDefinitions().keySet(), containsInAnyOrder(
                "#/definitions/SmithyExampleFoo",
                "#/definitions/SmithyExampleFooBarMember",
                "#/definitions/SmithyExampleString"));
    }

    @Test
    public void addsExtensionsFromConfig() {
        var index = ShapeIndex.builder().build();
        var config = Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.SCHEMA_DOCUMENT_EXTENSIONS, Node.objectNode()
                        .withMember("foo", Node.from("bar")))
                .build();
        var doc = JsonSchemaConverter.create().config(config).convert(index);

        assertThat(doc.getDefinitions().keySet(), empty());
        assertThat(doc.getExtension("foo").get(), equalTo(Node.from("bar")));
    }

    @Test
    public void convertsRootSchemas() {
        var shape = StringShape.builder().id("smithy.example#String").build();
        var index = ShapeIndex.builder().addShape(shape).build();
        var document = JsonSchemaConverter.create().convert(index, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("string"));
    }

    @Test
    public void convertsBlobToString() {
        var shape = BlobShape.builder().id("smithy.example#Blob").build();
        var index = ShapeIndex.builder().addShape(shape).build();
        var document = JsonSchemaConverter.create().convert(index, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("string"));
    }

    @Test
    public void convertsNumbersToNumber() {
        List<Shape> shapes = List.of(
                ByteShape.builder().id("smithy.example#Number").build(),
                ShortShape.builder().id("smithy.example#Number").build(),
                IntegerShape.builder().id("smithy.example#Number").build(),
                LongShape.builder().id("smithy.example#Number").build(),
                FloatShape.builder().id("smithy.example#Number").build(),
                DoubleShape.builder().id("smithy.example#Number").build(),
                BigIntegerShape.builder().id("smithy.example#Number").build(),
                BigDecimalShape.builder().id("smithy.example#Number").build());

        for (var shape : shapes) {
            var index = ShapeIndex.builder().addShape(shape).build();
            var document = JsonSchemaConverter.create().convert(index, shape);

            assertThat(document.getRootSchema().getType().get(), equalTo("number"));
        }
    }

    @Test
    public void supportsRangeTrait() {
        var shape = IntegerShape.builder()
                .id("smithy.example#Number")
                .addTrait(RangeTrait.builder().min(BigDecimal.valueOf(10)).max(BigDecimal.valueOf(100)).build())
                .build();
        var index = ShapeIndex.builder().addShape(shape).build();
        var document = JsonSchemaConverter.create().convert(index, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("number"));
        assertThat(document.getRootSchema().getMinimum().get(), equalTo(BigDecimal.valueOf(10)));
        assertThat(document.getRootSchema().getMaximum().get(), equalTo(BigDecimal.valueOf(100)));
    }

    @Test
    public void convertsBooleanToBoolean() {
        var shape = BooleanShape.builder().id("smithy.example#Boolean").build();
        var index = ShapeIndex.builder().addShape(shape).build();
        var document = JsonSchemaConverter.create().convert(index, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("boolean"));
    }

    @Test
    public void convertsListShapes() {
        var string = StringShape.builder().id("smithy.api#String").build();
        var member = MemberShape.builder().id("smithy.example#Collection$member").target("smithy.api#String").build();

        var shapes = List.of(
                ListShape.builder().id("smithy.example#Collection").addMember(member).build(),
                SetShape.builder().id("smithy.example#Collection").addMember(member).build());

        for (var shape : shapes) {
            var index = ShapeIndex.builder().addShapes(string, shape, member).build();
            var document = JsonSchemaConverter.create().convert(index, shape);

            assertThat(document.getRootSchema().getType().get(), equalTo("array"));
            assertThat(document.getRootSchema().getItems().get().getRef().get(),
                       equalTo("#/definitions/SmithyExampleCollectionMember"));
            var memberDef = document.getDefinition("#/definitions/SmithyExampleCollectionMember").get();
            assertThat(memberDef.getType().get(), equalTo("string"));
        }
    }

    @Test
    public void supportsStringLengthTrait() {
        var shape = StringShape.builder()
                .id("smithy.example#String")
                .addTrait(LengthTrait.builder().min(10L).max(100L).build())
                .build();
        var index = ShapeIndex.builder().addShape(shape).build();
        var document = JsonSchemaConverter.create().convert(index, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("string"));
        assertThat(document.getRootSchema().getMinLength().get(), equalTo(10L));
        assertThat(document.getRootSchema().getMaxLength().get(), equalTo(100L));
    }

    @Test
    public void supportsListAndSetLengthTrait() {
        var string = StringShape.builder().id("smithy.api#String").build();
        var member = MemberShape.builder()
                .id("smithy.example#Collection$member")
                .target("smithy.api#String")
                .addTrait(LengthTrait.builder().min(10L).max(100L).build())
                .build();

        var shapes = List.of(
                ListShape.builder().id("smithy.example#Collection").addMember(member).build(),
                SetShape.builder().id("smithy.example#Collection").addMember(member).build());

        for (var shape : shapes) {
            var index = ShapeIndex.builder().addShapes(string, shape, member).build();
            var document = JsonSchemaConverter.create().convert(index, shape);
            var memberDef = document.getDefinition("#/definitions/SmithyExampleCollectionMember").get();

            assertThat(memberDef.getMinLength().get(), equalTo(10L));
            assertThat(memberDef.getMaxLength().get(), equalTo(100L));
        }
    }

    @Test
    public void supportsMapLengthTrait() {
        var string = StringShape.builder().id("smithy.api#String").build();
        var key = MemberShape.builder().id("smithy.example#Map$key").target("smithy.api#String").build();
        var value = MemberShape.builder().id("smithy.example#Map$value").target("smithy.api#String").build();
        var shape = MapShape.builder()
                .id("smithy.example#Map")
                .key(key)
                .value(value)
                .addTrait(LengthTrait.builder().min(10L).max(100L).build())
                .build();
        var index = ShapeIndex.builder().addShapes(string, shape, key, value).build();
        var document = JsonSchemaConverter.create().convert(index, shape);
        var schema = document.getRootSchema();

        assertThat(schema.getMinProperties().get(), equalTo(10));
        assertThat(schema.getMaxProperties().get(), equalTo(100));
    }

    @Test
    public void supportsUniqueItemsOnLists() {
        var string = StringShape.builder().id("smithy.api#String").build();
        var member = MemberShape.builder().id("smithy.example#List$member").target("smithy.api#String").build();
        var shape = ListShape.builder()
                .id("smithy.example#List")
                .addMember(member)
                .addTrait(new UniqueItemsTrait())
                .build();

        var index = ShapeIndex.builder().addShapes(string, shape, member).build();
        var document = JsonSchemaConverter.create().convert(index, shape);

        assertTrue(document.getRootSchema().getUniqueItems());
    }

    @Test
    public void supportsPatternTrait() {
        var pattern = "^[A-Z]+$";
        var string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(new PatternTrait(pattern))
                .build();
        var index = ShapeIndex.builder().addShapes(string).build();
        var document = JsonSchemaConverter.create().convert(index, string);
        var schema = document.getRootSchema();

        assertThat(schema.getPattern().get(), equalTo(pattern));
    }

    @Test
    public void supportsMediaType() {
        var mediaType = "application/json";
        var string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(new MediaTypeTrait(mediaType))
                .build();
        var index = ShapeIndex.builder().addShapes(string).build();
        var document = JsonSchemaConverter.create().convert(index, string);
        var schema = document.getRootSchema();

        assertThat(schema.getContentMediaType().get(), equalTo(mediaType));
    }

    @Test
    public void supportsTitle() {
        var title = "Hello";
        var string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(new TitleTrait(title))
                .build();
        var index = ShapeIndex.builder().addShapes(string).build();
        var document = JsonSchemaConverter.create().convert(index, string);
        var schema = document.getRootSchema();

        assertThat(schema.getTitle().get(), equalTo(title));
    }

    @Test
    public void supportsDocumentation() {
        var docs = "Hello";
        var string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(new DocumentationTrait(docs))
                .build();
        var index = ShapeIndex.builder().addShapes(string).build();
        var document = JsonSchemaConverter.create().convert(index, string);
        var schema = document.getRootSchema();

        assertThat(schema.getDescription().get(), equalTo(docs));
    }

    @Test
    public void supportsEnum() {
        var string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(EnumTrait.builder().addEnum("foo", EnumConstantBody.builder().build()).build())
                .build();
        var index = ShapeIndex.builder().addShapes(string).build();
        var document = JsonSchemaConverter.create().convert(index, string);
        var schema = document.getRootSchema();

        assertThat(schema.getEnumValues().get(), contains("foo"));
    }

    @Test
    public void supportsUnionOneOf() {
        var string = StringShape.builder().id("smithy.api#String").build();
        var member = MemberShape.builder().id("a.b#Union$foo").target("smithy.api#String").build();
        var union = UnionShape.builder().id("a.b#Union").addMember(member).build();
        var index = ShapeIndex.builder().addShapes(union, member, string).build();
        var document = JsonSchemaConverter.create().convert(index, union);
        var schema = document.getRootSchema();

        assertThat(schema.getOneOf(), hasSize(1));
        assertThat(schema.getOneOf().get(0).getRequired(), contains("foo"));
        assertThat(schema.getOneOf().get(0).getProperties().keySet(), contains("foo"));
    }

    @Test
    public void supportsUnionObject() {
        var string = StringShape.builder().id("smithy.api#String").build();
        var member = MemberShape.builder().id("a.b#Union$foo").target("smithy.api#String").build();
        var union = UnionShape.builder().id("a.b#Union").addMember(member).build();
        var index = ShapeIndex.builder().addShapes(union, member, string).build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder()
                                .withMember(JsonSchemaConstants.SMITHY_UNION_STRATEGY, "object")
                                .build())
                .convert(index, union);
        var schema = document.getRootSchema();

        assertThat(schema.getOneOf(), empty());
        assertThat(schema.getType().get(), equalTo("object"));
        assertThat(schema.getProperties().keySet(), empty());
    }

    @Test
    public void supportsUnionStructure() {
        var string = StringShape.builder().id("smithy.api#String").build();
        var member = MemberShape.builder().id("a.b#Union$foo").target("smithy.api#String").build();
        var union = UnionShape.builder().id("a.b#Union").addMember(member).build();
        var index = ShapeIndex.builder().addShapes(union, member, string).build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder()
                                .withMember(JsonSchemaConstants.SMITHY_UNION_STRATEGY, "structure")
                                .build())
                .convert(index, union);
        var schema = document.getRootSchema();

        assertThat(schema.getOneOf(), empty());
        assertThat(schema.getType().get(), equalTo("object"));
        assertThat(schema.getProperties().keySet(), contains("foo"));
    }

    @Test
    public void throwsForUnsupportUnionSetting() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            var string = StringShape.builder().id("smithy.api#String").build();
            var member = MemberShape.builder().id("a.b#Union$foo").target("smithy.api#String").build();
            var union = UnionShape.builder().id("a.b#Union").addMember(member).build();
            var index = ShapeIndex.builder().addShapes(union, member, string).build();
            JsonSchemaConverter.create()
                    .config(Node.objectNodeBuilder()
                                    .withMember(JsonSchemaConstants.SMITHY_UNION_STRATEGY, "not-valid")
                                    .build())
                    .convert(index, union);
        });
    }
}
