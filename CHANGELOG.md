# Smithy Changelog

## 1.3.0 (2020-10-20)

### Features

* Added several `CodegenWriter` and related abstractions to simplify creating code generators. ([#587](https://github.com/awslabs/smithy/pull/587))
* Added the `@sparse` trait to the Prelude. ([#599](https://github.com/awslabs/smithy/pull/599))
* Added the `NullableIndex` to help check if a shape can be set to null. ([#599](https://github.com/awslabs/smithy/pull/599))
* Added support for API Gateway API key usage plans. ([#603](https://github.com/awslabs/smithy/pull/603), [#605](https://github.com/awslabs/smithy/pull/605))
* Added the `sortMembers` model transform to reorder the members of structures and unions. ([#588](https://github.com/awslabs/smithy/pull/588))
* Add `description` property to operations when converting to OpenAPI. ([#589](https://github.com/awslabs/smithy/pull/589))

### Bug Fixes

* Fixed an issue where the `flattenNamespaces` build transform was not registered with the SPI. ([#593](https://github.com/awslabs/smithy/pull/593))

### Optimizations

* Optimized the reverse `NeighborProvider` for memory usage. ([#590](https://github.com/awslabs/smithy/pull/590))
* Optimized model validation event aggregation for memory usage. ([#595](https://github.com/awslabs/smithy/pull/595))

### Documentation

* Clarified that `map` keys, `set` values, and `union` members cannot be null. ([#596](https://github.com/awslabs/smithy/pull/596/))
* Clarified `enum` names and their usage. ([#601](https://github.com/awslabs/smithy/pull/601))
* Added an example dependency to OpenAPI conversion. ([#594](https://github.com/awslabs/smithy/pull/594))
* Improve and clean up formatting. ([#585](https://github.com/awslabs/smithy/pull/585), [#597](https://github.com/awslabs/smithy/pull/597),
  [#598](https://github.com/awslabs/smithy/pull/598))

### Cleanup

* Updated `service`, `resource`, and `operation` shapes to maintain the order of bound `resource` and `operation`
  shapes. ([#602](https://github.com/awslabs/smithy/pull/602))
* Updated the `sources` build plugin to create an empty manifest if there are no source models. ([#607](https://github.com/awslabs/smithy/pull/607))
* Deprecated the `BoxIndex`. ([#599](https://github.com/awslabs/smithy/pull/599))
* Added `enum` names for `httpApiKeyLocation` in the Prelude. ([#606](https://github.com/awslabs/smithy/pull/606))

## 1.2.0 (2020-09-30)

### Features

* Added information to the `ModelDiff.Result` indicating how events have changed between the diff'd models. ([#574](https://github.com/awslabs/smithy/pull/574))
* Added a media type parser and validation for the `@mediaType` trait. ([#582](https://github.com/awslabs/smithy/pull/582))
* Added additional default CORS headers and configuration for OpenAPI conversions. ([#583](https://github.com/awslabs/smithy/pull/583))
* Added the `flattenNamespaces` build transform to flatten the namespaces of shapes connected to a specified service
  in a model in to a target namespace. ([#572](https://github.com/awslabs/smithy/pull/572))
* Added `runCommand` functionality to `smithy-utils`. ([#580](https://github.com/awslabs/smithy/pull/580))
* Added a `TriConsumer` to `smithy-utils`. ([#581](https://github.com/awslabs/smithy/pull/581))
* Added support for the `@httpResponseCode` trait in the `HttpBindingIndex`. ([#571](https://github.com/awslabs/smithy/pull/571))
* Added protocol tests for the `@httpResponseCode` trait. ([#573](https://github.com/awslabs/smithy/pull/573))

### Bug Fixes

* Fixed several issues that would cause Smithy to fail when running on Windows. ([#575](https://github.com/awslabs/smithy/pull/575),
  [#576](https://github.com/awslabs/smithy/pull/576), [#577](https://github.com/awslabs/smithy/pull/577))
* Fixed a bug where a `union` shape marked as an `@httpPayload` would throw an exception when trying to resolve
  its content-type. ([#584](https://github.com/awslabs/smithy/pull/584))
* Fixed a bug in OpenAPI conversions where tags were not passed through unless set in the `supportedTags` list, even
  when the `tags` setting was enabled. ([#570](https://github.com/awslabs/smithy/pull/570))

## 1.1.0 (2020-09-16)

### Features

* Added the `removeTraitDefinitions` build transform to remove trait definitions from models but leave instances intact.
  ([#558](https://github.com/awslabs/smithy/pull/558))
* Added payload binding validation to HTTP `DELETE` operations. ([#566](https://github.com/awslabs/smithy/pull/566))
* Updated `SmithyDiff` to emit events when traits are changed. ([#561](https://github.com/awslabs/smithy/pull/561))

### Bug Fixes

* Fixed an issue where some `StringListTrait` instances could lose `SourceLocation` information. ([#564](https://github.com/awslabs/smithy/pull/564))
* Fixed some issues in protocol tests. ([#560](https://github.com/awslabs/smithy/pull/560), [#563](https://github.com/awslabs/smithy/pull/563))

### Cleanup

* Model components are now deduplicated based on location and value. ([#565](https://github.com/awslabs/smithy/pull/565))
* Normalize URL import filenames for better deduplication and reporting. ([#562](https://github.com/awslabs/smithy/pull/562))

## 1.0.11 (2020-09-10)

### Features

* Added a reverse-topological knowledge index to aid in code generation for languages that require types to be
  defined before they are referenced. ([#545](https://github.com/awslabs/smithy/pull/545), [#53](https://github.com/awslabs/smithy/pull/553))
* Added the `@httpResponseCode` trait to indicate that a structure member represents an HTTP response status code. ([#546](https://github.com/awslabs/smithy/pull/546))
* Added (unstable) support for generating a "Trace File" to link code generated artifacts back to their modeled source.
  ([#552](https://github.com/awslabs/smithy/pull/552))
* Added the `:topdown` selector that matches shapes hierarchically. ([#539](https://github.com/awslabs/smithy/pull/539))
* Added validation for the `cloudTrailEventSource` property of the `@aws.api#service` trait. ([#550](https://github.com/awslabs/smithy/pull/550))
* Updated shape builders to properly update their member ShapeIds if the ShapeId of the builder changes. ([#556](https://github.com/awslabs/smithy/pull/556))
* Added several more XML related protocol tests. ([#547](https://github.com/awslabs/smithy/pull/547))

### Bug Fixes

* Fixed a bug where the `PaginatedIndex` did not properly support resolving paths. ([#554](https://github.com/awslabs/smithy/pull/554))

### Documentation

* Clarified the documentation for the `cloudTrailEventSource` property of the `@aws.api#service` trait. ([#550](https://github.com/awslabs/smithy/pull/550))
* Clarified that the `@aws.api#arn` trait has no impact on OpenAPI conversions. ([#555](https://github.com/awslabs/smithy/pull/555))

## 1.0.10 (2020-08-26)

### Features

* Added a validation event when a syntactic shape ID is found that does not target an actual shape in the model.
  ([#542](https://github.com/awslabs/smithy/pull/542))

### Bug Fixes

* Fixed a bug where forward reference resolution would use the incorrect namespace when resolving operation and
  resource bindings. ([#543](https://github.com/awslabs/smithy/pull/543))

### Cleanup

* Deprecated the reflection-based creation pattern for `KnowledgeIndex` implementations. ([#541](https://github.com/awslabs/smithy/pull/541))

## 1.0.9 (2020-08-21)

### Features

* Allow conflicting shape definitions if the fully built shapes are equivalent. ([#520](https://github.com/awslabs/smithy/pull/520))
* Added the `@internal` trait to the prelude. ([#531](https://github.com/awslabs/smithy/pull/531))
* Added the `excludeShapesByTrait` build transform that will remove any shapes marked with one or more of the
  specified traits. ([#531](https://github.com/awslabs/smithy/pull/531))
* Improved support for newlines and indentation in `CodeWriter`. ([#529](https://github.com/awslabs/smithy/pull/529))
* Added support for configuring the expression starting character in `CodeWriter`. ([#529](https://github.com/awslabs/smithy/pull/529))
* Added `payloadFormatVersion` property for API Gateway integrations. ([#527](https://github.com/awslabs/smithy/pull/527))
* Add `deprecated` property to operations when converting to OpenAPI. ([#535](https://github.com/awslabs/smithy/pull/535))
* Added several more protocol tests. ([#528](https://github.com/awslabs/smithy/pull/528), [#536](https://github.com/awslabs/smithy/pull/536)) 

### Bug Fixes

* Fixed the selector for the `@httpQuery` trait. ([#534](https://github.com/awslabs/smithy/pull/534))
* Fixed the selector for the `@httpPrefixHeaders` trait. ([#533](https://github.com/awslabs/smithy/pull/533))
* Fixed some issues in protocol tests. ([#526](https://github.com/awslabs/smithy/pull/526))

### Cleanup

* Removed the `abbreviation` property from the `@aws.api#service` trait. ([#532](https://github.com/awslabs/smithy/pull/532))
* Simplified prelude model loading. ([#524](https://github.com/awslabs/smithy/pull/524))
* Further simplified overall model loading. ([#525](https://github.com/awslabs/smithy/pull/525))

## 1.0.8 (2020-07-31)

### Features

* Updated `Walker` to provide a stable sort for shapes. ([#511](https://github.com/awslabs/smithy/pull/511))
* Improved support for loading `ValidationEvent`s via `NodeMapper`. ([#518](https://github.com/awslabs/smithy/pull/518), [#516](https://github.com/awslabs/smithy/pull/516))
* Added the ability to `disableFromNode` via `NodeMapper`. ([#505](https://github.com/awslabs/smithy/pull/505))

### Bug Fixes

* Fixed several issues in protocol tests. ([#502](https://github.com/awslabs/smithy/pull/502), [#507](https://github.com/awslabs/smithy/pull/507))

### Cleanup

* Stopped raising validation errors and running validation with `RenameShapes` transformer. ([#512](https://github.com/awslabs/smithy/pull/512))
* Simplified conflict handling for shapes. ([#514](https://github.com/awslabs/smithy/pull/514))
* Simplified duplicate member detection and handling. ([#513](https://github.com/awslabs/smithy/pull/513))

## 1.0.7 (2020-07-16)

### Features

* Use the `@title` trait to improve generated documentation for JSON Schema unions that use `"oneOf"`. ([#485](https://github.com/awslabs/smithy/pull/485))
* Added and updated several protocol tests for `restJson1`. ([#490](https://github.com/awslabs/smithy/pull/490))
* Added and updated several protocol tests for `awsJson1_1`. ([#484](https://github.com/awslabs/smithy/pull/484), [#493](https://github.com/awslabs/smithy/pull/493))
* Added protocol tests for `awsJson1_0`. ([#496](https://github.com/awslabs/smithy/pull/496))

### Bug Fixes

* Fixed a bug where `passthroughBehavior` was misspelled as `passThroughBehavior`
  in APIGateway OpenAPI output for integrations and mock integrations. ([#495](https://github.com/awslabs/smithy/pull/495))
* Fixed a bug where only the last line in a multiline doc comment on a
  member would be successfully parsed. ([#498](https://github.com/awslabs/smithy/pull/498))
* Fixed several issues in protocol tests. ([#473](https://github.com/awslabs/smithy/pull/473), [#476](https://github.com/awslabs/smithy/pull/476),
  [#481](https://github.com/awslabs/smithy/pull/481), [#491](https://github.com/awslabs/smithy/pull/491))

### Documentation

* Refactored the specification to better explain the semantic model and its representations. ([#497](https://github.com/awslabs/smithy/pull/497), [#482](https://github.com/awslabs/smithy/pull/482))
* Clarified guidance on using `@mediaType`. ([#500](https://github.com/awslabs/smithy/pull/500))
* Removed outdated namespace guidance. ([#487](https://github.com/awslabs/smithy/pull/487))
* Fixed several minor issues. ([#494](https://github.com/awslabs/smithy/pull/494))

### Cleanup

* Disallowed problematic identifiers misusing `_`. ([#499](https://github.com/awslabs/smithy/pull/499))
* Moved validation of members with the `@httpLabel` trait being marked required to the selector. ([#480](https://github.com/awslabs/smithy/pull/480))

## 1.0.6 (2020-06-24)

### Features

* Update `structure` and `union` shapes so member order is maintained as part of the contract. ([#465](https://github.com/awslabs/smithy/pull/465))
* Add validation for `document` types in protocols. ([#474](https://github.com/awslabs/smithy/pull/474))
* Provide suggestions for invalid Shape ID targets if a close match is found. ([#466](https://github.com/awslabs/smithy/pull/466))
* Added message templates and trait binding to the `EmitEachSelector`. ([#467](https://github.com/awslabs/smithy/pull/467))
* Added ability to add traits directly to the `ModelAssembler`. ([#470](https://github.com/awslabs/smithy/pull/470))
* Convert `awsJson1_1` protocol tests to Smithy IDL. ([#472](https://github.com/awslabs/smithy/pull/472))
* Update decimal values in protocol tests. ([#471](https://github.com/awslabs/smithy/pull/471)) 

### Documentation

* Update quick start guide with more examples. ([#462](https://github.com/awslabs/smithy/pull/462))

### Bug Fixes

* Fixed issues allowing `document` types in `@httpHeader` and `@httpPrefixHeaders` traits. ([#474](https://github.com/awslabs/smithy/pull/474))

## 1.0.5 (2020-06-05)

### Bug Fixes

* Fixed a bug in loading IDL files where resolving a forward reference that needed another
  forward reference would throw an exception. ([#458](https://github.com/awslabs/smithy/pull/458))
* Fixed a bug where smithy-build imports were not resolved relative to their `smithy-build.json`. ([#457](https://github.com/awslabs/smithy/pull/457))  
* Fixed a bug where the `PREFIX_HEADERS` HTTP binding location would not default its timestamp
  format to `HTTP_DATE`. ([#456](https://github.com/awslabs/smithy/pull/456))

## 1.0.4 (2020-05-29)

### Features

* Ensure that when a property is removed from a JSON schema object, that a corresponding "required"
  entry is also removed. ([#452](https://github.com/awslabs/smithy/pull/452))
* Added the (unstable) `@httpChecksumRequired` trait to indicate an operation requires a checksum
  in its HTTP request. ([#433](https://github.com/awslabs/smithy/pull/433), [#453](https://github.com/awslabs/smithy/pull/453))

### Bug Fixes

* Fixed a bug in OpenApi conversion where removing authentication for an operation would result
  in the operation inheriting the global "security" configuration instead of having it set to none. ([#451](https://github.com/awslabs/smithy/pull/451/))

### Documentation

* Added examples of building models to various guides. ([#449](https://github.com/awslabs/smithy/pull/449))
* Fixed various documentation issues. ([#449](https://github.com/awslabs/smithy/pull/449))

## 1.0.3 (2020-05-26)

### Bug Fixes

* Prevent parsing overly deep Node values ([#442](https://github.com/awslabs/smithy/pull/442))
* Fix an issue with the OpenAPI conversion where synthesized structure inputs reference required properties that
  were removed. ([#443](https://github.com/awslabs/smithy/pull/443))

## 1.0.2 (2020-05-18)

### Bug Fixes

* Fix an issue that would squash exceptions thrown for invalid suppressions. ([#440](https://github.com/awslabs/smithy/pull/440))

## 1.0.1 (2020-05-13)

### Features

* The `smithy.api#httpPayload` trait can now target document shapes. ([#431](https://github.com/awslabs/smithy/pull/431))
* Updated the IDL grammar to include many previously enforced parsing rules. ([#434](https://github.com/awslabs/smithy/pull/434))
* Added the `select` command to the CLI to print out shapes from a model that match a selector. ([#430](https://github.com/awslabs/smithy/pull/430))
* Added the `ast` command to the CLI to convert 0 or more Smithy models into a JSON AST. ([#435](https://github.com/awslabs/smithy/pull/435))
* Added a Dockerfile for building Smithy as a Docker image. ([#427](https://github.com/awslabs/smithy/pull/427))

### Optimizations

* The Smithy IDL parser has been rewritten and optimized. ([#434](https://github.com/awslabs/smithy/pull/434))
* Generate a class data share to speed up the CLI. ([#429](https://github.com/awslabs/smithy/pull/429))

### Bug Fixes

* Fix several ambiguities and issues in the IDL grammar. ([#434](https://github.com/awslabs/smithy/pull/434))
* JSON pretty printing of the AST now uses 4 spaces for indentation. ([#435](https://github.com/awslabs/smithy/pull/435))
* Fix CLI `--help` output alignment. ([#429](https://github.com/awslabs/smithy/pull/429))

## 1.0.0 (2020-05-04)

***Note***: Changes marked with "[BC]" are breaking changes more accurately described in the
specific section. A list of further intended breaking changes have a specific section near
the end of this entry.

### Features

#### General

* The model format version has been updated to `1.0` and contains several updates: [BC] ([#357](https://github.com/awslabs/smithy/pull/357), [#381](https://github.com/awslabs/smithy/pull/381))
  * The JSON AST representation requires describing annotation traits as `{}` instead of `true`.
  * Annotation traits in the IDL are now provided as `@foo` or `@foo()`. Explicit `@foo(true)` and
  `@foo(null)` support was removed.
* Smithy models can now be serialized to the IDL. ([#284](https://github.com/awslabs/smithy/pull/284))
* Added a Node-based object mapper to simplify the process of building and using Java components
from Smithy `Node`s. ([#301](https://github.com/awslabs/smithy/pull/301))
  * Many packages have seen significant updates to use this functionality. ([#305](https://github.com/awslabs/smithy/pull/305), [#364](https://github.com/awslabs/smithy/pull/364))
* Made error messages clearer when encountering duplicate shapes. ([#324](https://github.com/awslabs/smithy/pull/324))
* Model loaders now warn on additional shape properties instead of fail. ([#374](https://github.com/awslabs/smithy/pull/374))
* Added expect* methods to the base `Shape`. ([#314](https://github.com/awslabs/smithy/pull/314))
* Added `@SmithyUnstableApi`, `@SmithyInternalApi` and `@SmithyGenerated` Java annotations. ([#297](https://github.com/awslabs/smithy/pull/297))
* `NodeValidationVisitor`s are marked as internal and/or unstable. ([#375](https://github.com/awslabs/smithy/pull/375))
* The `$version` control statement can now be set to only a major version (e.g., "1") to indicate that an
  implementation must support a version >= 1 and < 2. `$version` can now be set to `major.minor` (e.g., "1.1")
  to indicate that an implementation must support a version >= 1.1 and < 2.

#### Trait updates

* Individual protocols are now defined as individual traits that are annotated with
[the `protocolDefinition` trait.](https://awslabs.github.io/smithy/1.0/spec/core/protocol-traits.html#protocoldefinition-trait) [BC] ([#273](https://github.com/awslabs/smithy/pull/273), [#280](https://github.com/awslabs/smithy/pull/280), [#379](https://github.com/awslabs/smithy/pull/379), [#390](https://github.com/awslabs/smithy/pull/390))
  * Previously listed [AWS protocols now have trait implementations.](https://awslabs.github.io/smithy/1.0/spec/aws/index.html#aws-protocols)
* Individual authentication schemes are now defined as individual traits that are annotated with
[the `authDefinition` trait.](https://awslabs.github.io/smithy/1.0/spec/core/auth-traits.html#authdefinition-trait) [BC] ([#273](https://github.com/awslabs/smithy/pull/273), [#280](https://github.com/awslabs/smithy/pull/280))
  * Previously listed [authentication schemes now have trait implementations.](https://awslabs.github.io/smithy/1.0/spec/core/auth-traits.html)
* The `smithy.api#enum` trait is now a list of enum definitions instead of a map of string keys to
enum definitions to improve clarity and encourage adding more properties to definitions. [BC] ([#326](https://github.com/awslabs/smithy/pull/326))
* The `aws.api#streaming` trait is now applied to shapes directly instead of members. [BC] ([#340](https://github.com/awslabs/smithy/pull/340))
* The `smithy.api#eventStream` trait has been removed. Event streams are now indicated by applying
the `smithy.api#streaming` trait to unions. [BC] ([#365](https://github.com/awslabs/smithy/pull/365))
* The `smithy.api#requiresLength` trait has been split out of the `smithy.api#streaming` trait to
improve clarity around event stream modeling. [BC] ([#368](https://github.com/awslabs/smithy/pull/368))
* The `smithy.api#externalDocumentation` trait is now a map instead of a single string to allow for
multiple links per trait. [BC] ([#363](https://github.com/awslabs/smithy/pull/363))
* Added the `smithy.api#noReplace` trait to indicate a PUT lifecycle operation cannot replace the
existing resource. ([#351](https://github.com/awslabs/smithy/pull/351))
* Added the `smithy.api#unstable` trait to indicate a shape MAY change. ([#290](https://github.com/awslabs/smithy/pull/290))
* Simplified `aws.api#unsignedPayload` to be an annotation. [BC] ([#270](https://github.com/awslabs/smithy/pull/270))
* Annotation traits are now lossless when loaded with additional properties, meaning they will
contain those properties when serialized. ([#385](https://github.com/awslabs/smithy/pull/385))

#### Selector updates

Selectors have received significant updates: ([#388](https://github.com/awslabs/smithy/pull/388))

* Attribute selectors can now evaluate scoped comparisons using `@foo:` to define a scope
and `@{bar}` to define a context value. ([#391](https://github.com/awslabs/smithy/pull/391))
* And logic, via `&&`, has been added to allow multiple attribute comparisons. ([#391](https://github.com/awslabs/smithy/pull/391))
* Support for selecting nested trait properties with `|`, including list/object values and object
keys, was added.
* An opt-in `trait` relationship has been added. ([#384](https://github.com/awslabs/smithy/pull/384))
* The recursive neighbor selector, `~>`, has been added. ([#386](https://github.com/awslabs/smithy/pull/386))
* A not equal comparison, `!=`, was added.
* An exists comparison, `?=`, was added. ([#391](https://github.com/awslabs/smithy/pull/391))
* Support for numbers in attribute selectors was added.
* Numeric comparisons (`>`, `>=`, `<`, `<=`) were added.
* The `(length)` function property was added. ([#391](https://github.com/awslabs/smithy/pull/391))
* Attribute selectors now support CSV values, allowing matching on one or more target values.
* The `:each` selector is now `:is` for clarity. [BC]
* The `:of` selector is now removed. Use reverse neighbors instead (e.g., `member :test(< structure)`). [BC]
* The semantics of the `:not` selector have changed significantly. `:not(list > member > string)` now means
  "do not match list shapes that target strings", whereas this previously meant,
  "do not match string shapes targeted by lists". [BC]
* Shape IDs with members must now be quoted. [BC]
* Selector parsing and evaluating now tolerates unknown relationship types. ([#377](https://github.com/awslabs/smithy/pull/377))

#### Validation updates

* Services must now contain a closure of shapes that have case-insensitively unique names. [BC] ([#337](https://github.com/awslabs/smithy/pull/337))
* `suppressions` has been updated to now only suppress validation events that occur for an entire namespace or across
  the entire model. The `@suppress` trait was added to suppress validation events for a specific shape. [BC] ([#397](https://github.com/awslabs/smithy/pull/397)).
* The `UnreferencedShape` validator has moved to `smithy-model` and is now always run. [BC] ([#319](https://github.com/awslabs/smithy/pull/319))
* `EmitEachSelector` and `EmitNoneSelector` were moved from `smithy-linters` into `smithy-model`.

#### JSON Schema conversion

The conversion to JSON schema was significantly overhauled. [BC] ([#274](https://github.com/awslabs/smithy/pull/274))

* Configuration for the build plugin was significantly overhauled. [BC] ([#364](https://github.com/awslabs/smithy/pull/364))
* The strategy for shape inlining has been changed. [BC]
* The strategy for naming shapes and handling shape id conflicts has been changed. [BC]
* Output schema error detection was improved.
* The Java API surface has been reduced. [BC]
* Added the ability to select schemas from a document using a JSON pointer.

#### OpenAPI conversion

The conversion to OpenAPI was significantly overhauled. [BC] ([#275](https://github.com/awslabs/smithy/pull/275))

* Configuration for the build plugin was significantly overhauled.  [BC] ([#364](https://github.com/awslabs/smithy/pull/364))
* Protocol conversion was updated to utilize the new traits. ([#275](https://github.com/awslabs/smithy/pull/275), [#392](https://github.com/awslabs/smithy/pull/392))
* Schemas are now generated for requests and responses instead of being inlined. [BC]
* Fixed several issues with CORS integrations.

#### API Gateway OpenAPI conversion

The API Gateway specific OpenAPI mappers have been updated. [BC] ([#367](https://github.com/awslabs/smithy/pull/367))

*  The `ApiGatewayMapper` interface was added, allowing mappers to control which API Gateway API
type(s) they support.
* Fixed several issues with CORS integrations. ([#370](https://github.com/awslabs/smithy/pull/370))
* Added support for JSON Patch-like OpenAPI schema changes based on JSON Pointers. ([#293](https://github.com/awslabs/smithy/pull/293))
* Added support for event streams in OpenAPI conversion. ([#334](https://github.com/awslabs/smithy/pull/334))

### Bug Fixes

* Fixed an issue in JSON schema conversion where member traits were dropped in some scenarios. ([#274](https://github.com/awslabs/smithy/pull/274))
* Fixed an issue where authorization headers were not properly added to CORS configurations. ([#328](https://github.com/awslabs/smithy/pull/328))
* Fixed an issue where operation response headers were being applied to error responses in OpenAPI
conversions. ([#275](https://github.com/awslabs/smithy/pull/275))
* Fixed an issue where `apply` statements wouldn't resolve target shapes properly in some cases. ([#287](https://github.com/awslabs/smithy/pull/287))
* Fixed an issue with the selector for the `smithy.api#title` trait. ([#387](https://github.com/awslabs/smithy/pull/387))
* Fixed several issues with the `smithy.api#httpApiKeyAuth` trait and its related conversions. ([#291](https://github.com/awslabs/smithy/pull/291))
* Fixed a bug with timestamp validation in specific versions of Java. ([#316](https://github.com/awslabs/smithy/pull/316))

### Optimizations

* The `TraitTargetValidator` now performs as few checks on and selections of the entire model. ([#389](https://github.com/awslabs/smithy/pull/389))
* The dependency on `jackson-core` was replaced with a vendored version of `minimal-json` to reduce
the chances of dependency conflicts. [BC] ([#323](https://github.com/awslabs/smithy/pull/323))
* Sped up model loading time by loading JSON models before IDL models to reduce forward
reference lookups. ([#287](https://github.com/awslabs/smithy/pull/287))

### Breaking changes

All changes listed in this heading and any sub-headings are breaking changes.

* The `BooleanTrait` abstract class in `smithy-model` was renamed `AnnotationTrait`. ([#381](https://github.com/awslabs/smithy/pull/381))
* The traits in the `aws.apigateway` namespace have moved from `smithy-aws-traits` to the
`smithy-aws-apigateway-traits` package for more granular use. ([#322](https://github.com/awslabs/smithy/pull/322))
  * Tooling that referenced these traits has also been updated.
* The traits in the `aws.iam` namespace have moved from `smithy-aws-traits` to the
`smithy-aws-iam-traits` package for more granular use. ([#322](https://github.com/awslabs/smithy/pull/322))
  * Tooling that referenced these traits has also been updated.
* The `aws.api#ec2QueryName` trait has moved to `aws.protocols#ec2QueryName`. ([#286](https://github.com/awslabs/smithy/pull/286))
* The `aws.api#unsignedPayload ` trait has moved to `aws.auth#unsignedPayload `. ([#286](https://github.com/awslabs/smithy/pull/286))
* The `smithy-codegen-freemarker` package has been removed. ([#382](https://github.com/awslabs/smithy/pull/382))
* Traits can no longer be applied to public Smithy Prelude shapes. ([#317](https://github.com/awslabs/smithy/pull/317))
* Smithy's `Pattern` class is renamed to `SmithyPattern` to remove the conflict with Java's regex
`Pattern` class. ([#315](https://github.com/awslabs/smithy/pull/315))
* Removed the `Triple` class from `smithy-utils`. ([#313](https://github.com/awslabs/smithy/pull/313))
* Normalized class names for OpenAPI `SecurityScemeConverter` implementations. ([#291](https://github.com/awslabs/smithy/pull/291))
* Removed alias functionality from `software.amazon.smithy.build.SmithyBuildPlugin` and
  `software.amazon.smithy.build.ProjectionTransformer`. ([#409](https://github.com/awslabs/smithy/pull/409))
* Removed `software.amazon.smithy.model.shapes.Shape#visitor` and
  `software.amazon.smithy.model.shapes.ShapeVisitor$Builder`. Use
  `software.amazon.smithy.model.shapes.ShapeVisitor$Default` instead. ([#413](https://github.com/awslabs/smithy/pull/413))
* `software.amazon.smithy.model.Model#getTraitDefinitions` and `getTraitShapes` were removed in favor of
  `software.amazon.smithy.model.Model#getShapesWithTrait`. ([#412](https://github.com/awslabs/smithy/pull/412))

#### Deprecation cleanup

* The deprecated IDL operation syntax has been removed ([#373](https://github.com/awslabs/smithy/pull/373))
* The deprecated `NodeFactory` interface has been removed. ([#265](https://github.com/awslabs/smithy/pull/265))
* The deprecated `ShapeIndex` class and all related APIs have been removed. ([#266](https://github.com/awslabs/smithy/pull/266))
* Support for the deprecated `0.4.0` model version has been removed. ([#267](https://github.com/awslabs/smithy/pull/267))
* The `aws.api#service` trait no longer supports the deprecated
`sdkServiceId`, `arnService`, or `productName` properties. ([#268](https://github.com/awslabs/smithy/pull/268))
* The deprecated `TemplateEngine` and `DefaultDataTemplateEngine` have been removed. ([#268](https://github.com/awslabs/smithy/pull/268))
* The deprecated `smithy.validators` and `smithy.suppressions` are no longer used as aliases for
validators and suppressions. ([#268](https://github.com/awslabs/smithy/pull/268))
* The `smithy.api#references` and `smithy.api#idRef` traits no longer support relative shape IDs. ([#268](https://github.com/awslabs/smithy/pull/268))

### Documentation

A significant overhaul of the specification and guides has been completed. This includes a better
flow to the spec, more complete guides, deeper documentation of AWS specific components, and a
complete redesign. Many direct links to components of the documentation will have changed.

## 0.9.9 (2020-04-01)

### Bug Fixes

* Add security to individual operations in OpenAPI conversion ([#329](https://github.com/awslabs/smithy/pull/329))
* Fix an issue with header casing for `x-api-key` integration with API Gateway ([#330](https://github.com/awslabs/smithy/pull/330))
* Fix discrepancies in `smithy-aws-protocol-tests` ([#333](https://github.com/awslabs/smithy/pull/333), [#335](https://github.com/awslabs/smithy/pull/335),
  [#349](https://github.com/awslabs/smithy/pull/349))

## 0.9.8 (2020-03-26)

### Features

* Add `RenameShapes` model transformer ([#318](https://github.com/awslabs/smithy/pull/318))
* Build `ValidationEvents` are now sorted ([#263](https://github.com/awslabs/smithy/pull/263))
* Smithy CLI logging improvements ([#263](https://github.com/awslabs/smithy/pull/263))
* Model builds fail early when syntax errors occur ([#264](https://github.com/awslabs/smithy/pull/264))
* Warn when a deprecated trait is applied to a shape ([#279](https://github.com/awslabs/smithy/pull/279))

### Bug Fixes

* Fix behavior of `schemaDocumentExtensions` when converting to OpenAPI ([#320](https://github.com/awslabs/smithy/pull/320))
* Fix discrepancies in `smithy-aws-protocol-tests` ([#309](https://github.com/awslabs/smithy/pull/309), [#321](https://github.com/awslabs/smithy/pull/321))
* Properly format test case results ([#271](https://github.com/awslabs/smithy/pull/271))
* Fix dropping one character text block lines ([#285](https://github.com/awslabs/smithy/pull/285))

### Optimizations

* Builds run parallel projections in parallel only if there are more than one ([#263](https://github.com/awslabs/smithy/pull/263))
* Run Smithy test suites as parameterized tests ([#263](https://github.com/awslabs/smithy/pull/263))

### Cleanup

* Migrate protocol tests to new operation syntax ([#260](https://github.com/awslabs/smithy/pull/260))
* Build protocol tests with the Smithy Gradle plugin ([#263](https://github.com/awslabs/smithy/pull/263))
* Deprecate using explicitly `smithy.api` for trait removal ([#306](https://github.com/awslabs/smithy/pull/306))

## 0.9.7 (2020-01-15)

### Features

* Updated Operation syntax in the Smithy IDL ([#253](https://github.com/awslabs/smithy/pull/253))
* Updated specification for XML traits ([#242](https://github.com/awslabs/smithy/pull/242))
* Add the `@aws.api#ec2QueryName-trait` trait ([#251](https://github.com/awslabs/smithy/pull/251))
* Add AWS protocol test models ([#246](https://github.com/awslabs/smithy/pull/246), [#247](https://github.com/awslabs/smithy/pull/247),
  [#250](https://github.com/awslabs/smithy/pull/250), [#255](https://github.com/awslabs/smithy/pull/255),
  and [#258](https://github.com/awslabs/smithy/pull/258))

### Optimizations

* Use URLConnection cache setting in ModelAssembler ([#244](https://github.com/awslabs/smithy/pull/244))

### Bug Fixes

* Use list of string for queryParams in the `httpRequestTests` trait ([#240](https://github.com/awslabs/smithy/pull/240))

## 0.9.6 (2020-01-02)

### Features

* Allow XML maps to be flattened ([#205](https://github.com/awslabs/smithy/pull/205))
* Add and remove shape members to model automatically ([#206](https://github.com/awslabs/smithy/pull/206))
* Deprecate ShapeIndex in favor of Model ([#209](https://github.com/awslabs/smithy/pull/209))
* Allow the sensitive trait to be applied to all but operations, services, and resources ([#212](https://github.com/awslabs/smithy/pull/212))
* Added 0.5.0 IDL and AST format ([#213](https://github.com/awslabs/smithy/pull/213))
* Allow min to equal max in range trait ([#216](https://github.com/awslabs/smithy/pull/216))
* Added validation for length trait values ([#217](https://github.com/awslabs/smithy/pull/217))
* Limit streaming trait to top-level members ([#221](https://github.com/awslabs/smithy/pull/221))
* Added protocol compliance test traits ([#226](https://github.com/awslabs/smithy/pull/226))
* Added ability to configure timestamp validation ([#229](https://github.com/awslabs/smithy/pull/229))
* Moved `TemplateEngine` implementation into FreeMarker implementation ([#230](https://github.com/awslabs/smithy/pull/230))
* Added `BoxIndex` ([#234](https://github.com/awslabs/smithy/pull/234))
* Added more expect methods to `Shape` and `Model` ([#237](https://github.com/awslabs/smithy/pull/237))

### Optimizations

* Update smithy-build to be streaming ([#211](https://github.com/awslabs/smithy/pull/211))

### Bug Fixes

* Prevent bad list, set, and map recursion ([#204](https://github.com/awslabs/smithy/pull/204))
* Properly allow omitting endpoint discovery operation inputs ([#220](https://github.com/awslabs/smithy/pull/220))

## 0.9.5 (2019-11-11)

### Features

* Allow overriding state management in CodeWriter ([#186](https://github.com/awslabs/smithy/pull/186))
* Allow the `xmlFlattened` trait to be applied to members ([#191](https://github.com/awslabs/smithy/pull/191))
* Add helper to determine HTTP-based timestamp formats ([#193](https://github.com/awslabs/smithy/pull/193))
* Allow specifying XML namespace prefixes ([#195](https://github.com/awslabs/smithy/pull/195))
* Add `SymbolContainer`, an abstraction over `Symbol`s that enables easily
  creating and aggregating `Symbols` ([#202](https://github.com/awslabs/smithy/pull/202))

### Bug Fixes

* Escape popped state content ([#187](https://github.com/awslabs/smithy/pull/187))
* Make shape ID serialization consistent ([#196](https://github.com/awslabs/smithy/pull/196))
* Exclude private members targeted in JSON schema converters ([#199](https://github.com/awslabs/smithy/pull/199))
* Fix naming collisions in JSON schema output ([#200](https://github.com/awslabs/smithy/pull/200))
* Update `equals` to included typed bag parents ([#201](https://github.com/awslabs/smithy/pull/201))

## 0.9.4 (2019-10-09)

### Features

* Add support for AWS Client Endpoint Discovery ([#165](https://github.com/awslabs/smithy/pull/165))
* Refactor event streams to target members ([#171](https://github.com/awslabs/smithy/pull/171))
* Add support for aliasing referenced `Symbol`s ([#168](https://github.com/awslabs/smithy/pull/168))
* Add support for `Symbol`s to introduce dependencies ([#169](https://github.com/awslabs/smithy/pull/169))
* Add ability to manually escape reserved words in `ReservedWordSymbolProvider` ([#174](https://github.com/awslabs/smithy/pull/174))
* Add method to gather dependencies for `Symbol`s ([#170](https://github.com/awslabs/smithy/pull/170))
* Add a caching `SymbolProvider` ([#167](https://github.com/awslabs/smithy/pull/167))
* Improve the usability of `CodeWroter#openBlock` ([#175](https://github.com/awslabs/smithy/pull/175))
* Improve the usability of `PluginContext` ([#181](https://github.com/awslabs/smithy/pull/181))

### Optimizations

* Disable URLConnection cache in CLI ([#180](https://github.com/awslabs/smithy/pull/180))

### Bug Fixes

* Fix issue with generated authentication for CORS checks ([#179](https://github.com/awslabs/smithy/pull/179))
* Set the `defaultTimestampFormat` to `epoch-seconds` for `aws.rest-json` protocols in OpenAPI ([#184](https://github.com/awslabs/smithy/pull/184))

## 0.9.3 (2019-09-16)

### Features

* Clean up `CodeWriter` modifiers ([#143](https://github.com/awslabs/smithy/pull/143))
* Add typed `ObjectNode` member expectation functions ([#144](https://github.com/awslabs/smithy/pull/144))
* Add `expectShapeId` for fully-qualified shape ID ([#147](https://github.com/awslabs/smithy/pull/147))
* Add helper to `EnumTrait` to check if it has names ([#148](https://github.com/awslabs/smithy/pull/148))
* Add `Symbol` references ([#149](https://github.com/awslabs/smithy/pull/149))
* Add `ReservedWords` builder for simpler construction ([#150](https://github.com/awslabs/smithy/pull/150))
* Allow using path expressions in paginator outputs ([#152](https://github.com/awslabs/smithy/pull/152))
* Add method to get non-trait shapes ([#153](https://github.com/awslabs/smithy/pull/153))
* Add method to write class resource to manifest ([#157](https://github.com/awslabs/smithy/pull/157))
* Allow `authType` to be specified ([#160](https://github.com/awslabs/smithy/pull/160))


### Bug Fixes

* Fix collection and gradle doc issues ([#145](https://github.com/awslabs/smithy/pull/145))
* Make `AuthorizerDefinition` definition private ([#146](https://github.com/awslabs/smithy/pull/146))
* Fix put handling on `ResourceShape` ([#158](https://github.com/awslabs/smithy/pull/158))
* Fix parse error when `apply` is at eof ([#159](https://github.com/awslabs/smithy/pull/159))
* Prevent `list`/`set` member from targeting container ([#162](https://github.com/awslabs/smithy/pull/162))
* Allow model assembling from symlink model files / directory ([#163](https://github.com/awslabs/smithy/pull/163))
