namespace smithy.example

use smithy.test#httpRequestTests

@trait
@protocolDefinition
structure testProtocol {}

@trait
@authDefinition
structure testScheme {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo",
        documentation: "Testing...",
        protocol: testProtocol,
        authScheme: testScheme,
        method: "POST",
        uri: "/",
        queryParams: ["foo=baz"],
        forbidQueryParams: ["Nope"],
        requireQueryParams: ["Yap"],
        headers: {"X-Foo": "baz"},
        forbidHeaders: ["X-Nope"],
        requireHeaders: ["X-Yap"],
        body: "Hi",
        bodyMediaType: "text/plain",
        params: {
            body: "Hi",
            header: null,
        },
        vendorParams: {foo: "Bar"}
    }
])
operation SayHello {
    input: SayHelloInput
}

structure SayHelloInput {
    @httpPayload
    body: String,

    @httpHeader("X-OmitMe")
    header: String,
}
