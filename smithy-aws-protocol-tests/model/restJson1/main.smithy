$version: "1.0.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A REST JSON service that sends JSON requests and responses.
@restJson1
service RestJson {
    version: "2019-12-16",
    operations: [
        // Basic input and output tests
        NoInputAndNoOutput,
        NoInputAndOutput,
        EmptyInputAndEmptyOutput,

        // @httpHeader tests
        InputAndOutputWithHeaders,
        NullAndEmptyHeaders,
        TimestampFormatHeaders,

        // @httpLabel tests
        HttpRequestWithLabels,
        HttpRequestWithLabelsAndTimestampFormat,
        HttpRequestWithGreedyLabelInPath,

        // @httpQuery tests
        AllQueryStringTypes,
        ConstantQueryString,
        ConstantAndVariableQueryString,
        IgnoreQueryParamsInResponse,
        OmitsNullSerializesEmptyString,
        QueryIdempotencyTokenAutoFill,

        // @httpPrefixHeaders tests
        HttpPrefixHeaders,

        // @httpPayload tests
        HttpPayloadTraits,
        HttpPayloadTraitsWithMediaType,
        HttpPayloadWithStructure,

        // Errors
        GreetingWithErrors,

        // Synthesized JSON document body tests
        SimpleScalarProperties,
        JsonTimestamps,
        JsonEnums,
        RecursiveShapes,
        JsonLists,
        JsonMaps,
        JsonBlobs,
    ]
}
