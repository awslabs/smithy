// This file defines test cases that serialize maps in XML payloads.

$version: "0.5.0"

namespace aws.protocols.tests.query

use aws.protocols.tests.shared#FooEnumMap
use aws.protocols.tests.shared#GreetingStruct
use smithy.test#httpResponseTests

/// The example tests basic map serialization.
operation XmlMaps() -> XmlMapsOutput

apply XmlMaps @httpResponseTests([
    {
        id: "QueryXmlMaps",
        description: "Serializes XML maps",
        protocol: "aws.query",
        code: 200,
        body: """
              <XmlMapsResponse xmlns="https://example.com/">
                  <XmlMapsResult>
                      <myMap>
                          <entry>
                              <key>foo</key>
                              <value>
                                  <hi>there</hi>
                              </value>
                          </entry>
                          <entry>
                              <key>baz</key>
                              <value>
                                  <hi>bye</hi>
                              </value>
                          </entry>
                      </myMap>
                  </XmlMapsResult>
              </XmlMapsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {
                foo: {
                    hi: "there"
                },
                baz: {
                    hi: "bye"
                }
            }
        }
    }
])

structure XmlMapsOutput {
    myMap: XmlMapsOutputMap,
}

map XmlMapsOutputMap {
    key: String,
    value: GreetingStruct
}

// This example tests maps with @xmlName on members.
operation XmlMapsXmlName() -> XmlMapsXmlNameOutput

apply XmlMapsXmlName @httpResponseTests([
    {
        id: "QueryQueryXmlMapsXmlName",
        description: "Serializes XML lists",
        protocol: "aws.query",
        code: 200,
        body: """
              <XmlMapsXmlNameResponse xmlns="https://example.com/">
                  <XmlMapsXmlNameResult>
                      <myMap>
                          <entry>
                              <Name>foo</Name>
                              <Setting>
                                  <hi>there</hi>
                              </Setting>
                          </entry>
                          <entry>
                              <Name>baz</Name>
                              <Setting>
                                  <hi>bye</hi>
                              </Setting>
                          </entry>
                      </myMap>
                  </XmlMapsXmlNameResult>
              </XmlMapsXmlNameResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {
                foo: {
                    hi: "there"
                },
                baz: {
                    hi: "bye"
                }
            }
        }
    }
])

structure XmlMapsXmlNameOutput {
    myMap: XmlMapsXmlNameOutputMap,
}

map XmlMapsXmlNameOutputMap {
    @xmlName("Attribute")
    key: String,

    @xmlName("Setting")
    value: GreetingStruct
}

/// Flattened maps
operation FlattenedXmlMap() -> FlattenedXmlMapOutput

apply FlattenedXmlMap @httpResponseTests([
    {
        id: "QueryQueryFlattenedXmlMap",
        description: "Serializes flattened XML maps in responses",
        protocol: "aws.query",
        code: 200,
        body: """
              <FlattenedXmlMapResponse xmlns="https://example.com/">
                  <FlattenedXmlMapResult>
                      <myMap>
                          <key>foo</key>
                          <value>Foo</value>
                      </myMap>
                      <myMap>
                          <key>baz</key>
                          <value>Baz</value>
                      </myMap>
                  </FlattenedXmlMapResult>
              </FlattenedXmlMapResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {
                foo: "Foo",
                baz: "Baz"
            }
        }
    }
])

structure FlattenedXmlMapOutput {
    @xmlFlattened
    myMap: FooEnumMap,
}

/// Flattened maps with @xmlName
operation FlattenedXmlMapWithXmlName() -> FlattenedXmlMapWithXmlNameOutput

apply FlattenedXmlMapWithXmlName @httpResponseTests([
    {
        id: "QueryQueryFlattenedXmlMapWithXmlName",
        description: "Serializes flattened XML maps in responses that have xmlName on members",
        protocol: "aws.query",
        code: 200,
        body: """
              <FlattenedXmlMapWithXmlNameResponse xmlns="https://example.com/">
                  <FlattenedXmlMapWithXmlNameResult>
                      <KVP>
                          <K>a</K>
                          <V>A</V>
                      </myMap>
                      <KVP>
                          <K>b</K>
                          <V>B</V>
                      </myMap>
                  </FlattenedXmlMapWithXmlNameResult>
              </FlattenedXmlMapWithXmlNameResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {
                a: "A",
                b: "B",
            }
        }
    }
])

structure FlattenedXmlMapWithXmlNameOutput {
    @xmlFlattened
    @xmlName("KVP")
    myMap: FlattenedXmlMapWithXmlNameOutputMap,
}

map FlattenedXmlMapWithXmlNameOutputMap {
    @xmlName("K")
    key: String,

    @xmlName("V")
    value: String,
}
