======================
Type refinement traits
======================

Type refinement traits are traits that significantly refine, or change,
the type of a shape.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


.. _box-trait:

-------------
``box`` trait
-------------

Summary
    Indicates that a shape is boxed. When a :ref:`member <member>` is marked
    with this trait or the shape targeted by a member is marked with this
    trait, the member may or may not contain a value, and the member has no
    :ref:`default value <default-values>`.

    Boolean, byte, short, integer, long, float, and double shapes are only
    considered boxed if they are marked with the ``box`` trait. All other
    shapes are always considered boxed.
Trait selector
    .. code-block:: none

        :test(boolean, byte, short, integer, long, float, double,
              member > :test(boolean, byte, short, integer, long, float, double))

    *A boolean, byte, short, integer, long, float, double shape or a member that targets one of these shapes*
Value type
    Annotation trait.

The ``box`` trait is primarily used to influence code generation. For example,
in Java, this might mean the value provided as the member of an aggregate
shape can be set to null. In a language like Rust, this might mean the value
is wrapped in an `Option type`_.

.. tabs::

    .. code-tab:: smithy

        @box
        integer BoxedInteger

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#BoxedInteger": {
                    "type": "integer",
                    "traits": {
                        "smithy.api#box": {}
                    }
                }
            }
        }

The :ref:`prelude <prelude>` contains predefined simple shapes that can be
used in all Smithy models, including boxed and unboxed shapes.


.. _error-trait:

---------------
``error`` trait
---------------

Summary
    Indicates that a structure shape represents an error. All shapes
    referenced by the :ref:`errors list of an operation <operation-errors>`
    MUST be targeted with this trait.
Trait selector
    ``structure``
Value type
    ``string`` that MUST be set to "client" or "server" to indicate if the
    client or server is at fault for the error.
Conflicts with
    :ref:`trait <trait-shapes>`

The following structure defines a throttling error.

.. tabs::

    .. code-tab:: smithy

        @error("client")
        structure ThrottlingError {}

Note that this structure is lacking the ``retryable`` trait that generically
lets clients know that the error is retryable.

.. tabs::

    .. code-tab:: smithy

        @error("client")
        @retryable
        structure ThrottlingError {}

When using an HTTP-based protocol, it is recommended to add an
:ref:`httpError-trait` to use an appropriate HTTP status code with
the error.

.. tabs::

    .. code-tab:: smithy

        @error("client")
        @retryable
        @httpError(429)
        structure ThrottlingError {}

The ``message`` member of an error structure is special-cased. It contains
the human-readable message that describes the error. If the ``message`` member
is not defined in the structure, code generated for the error may not provide
an idiomatic way to access the error message (e.g., an exception message
in Java).

.. tabs::

    .. code-tab:: smithy

        @error("client")
        @retryable
        @httpError(429)
        structure ThrottlingError {
            @required
            message: String,
        }


.. _exactDocument-trait:

-----------------------
``exactDocument`` trait
-----------------------

Summary
    Indicates that a ``blob`` or ``string`` shape contains a nested
    :ref:`document <document-type>` that can be lazily parsed and accessed
    in a structured way. The targeted shape MUST be marked with the
    :ref:`mediaType-trait` to define the serialization format of the document.
Trait selector
    ``:is(blob, string)[trait|mediaType]``

    *A blob or string shape marked with the mediaType trait*
Value type
    Annotation trait.
Conflicts with
    :ref:`streaming <streaming-trait>`: Document types are expected to be
    loaded into memory, and as such, MUST NOT be marked as ``streaming``.

The following example defines an exact document that contains binary
`Amazon Ion`_ data:

.. code-block:: smithy

    @exactDocument
    @mediaType("application/x-amzn-ion")
    blob IonDocument

The following example defines an exact document that contains Amazon Ion
text data:

.. code-block:: smithy

    @exactDocument
    @mediaType("text/x-amzn-ion; charset=utf-8")
    string IonText


Comparisons to inline documents
===============================

An exact document is serialized in a way that is isolated from its
surroundings. The term "exact" is used to indicate that the serialization
format of the document is an important part of its contract. In contrast,
:ref:`inline document types <document-type>` are serialized in a
protocol-agnostic way and can only express data types as granular as the
JSON-type system. Exact documents are preferred over inline documents when
the exact bytes of the document are required for an application. For
example, an exact document is required in order to use advanced features
of Amazon Ion like `typed nulls`_, user-defined `symbol tables`_, or
`Ion hashing`_.

Implementations need to be able to parse the media type of the exact
document in order to access its contents. However, implementations do not
need to have prior knowledge of the deserialized contents of an exact
document.

.. rubric:: Inline and exact document examples

Given the following Smithy model:

.. code-block:: smithy

    structure Example {
        foo: Document
    }

    document Document

The following non-normative example demonstrates how an inline document in
a JSON-based protocol could be serialized in the member named "foo":

.. code-block:: json

    {
        "foo": {
            "hello": "hi"
        }
    }

Given the following Smithy model:

.. code-block:: smithy

    structure Example {
        foo: JsonDocument
    }

    @exactDocument
    @mediaType("application/json")
    string JsonDocument

The following non-normative example demonstrates how that same document could
be serialized in an exact document that contains ``application/json`` data in
a JSON-based protocol:

.. code-block:: json

    {
        "foo": "{\"hello\": \"hi\"}"
    }


Code generation and implementation
==================================

Code generators SHOULD generate exact document types as a type that lazily
parses and returns an abstraction that is the same as or a specialization of
the abstraction it uses for :ref:`inline documents <document-type>`.
Implementations SHOULD make parsing documents something that is only done
lazily because not every use case involving exact documents will require the
contents to be parsed. Because the serialization format of an exact document
is a stable part of its contract, implementations MUST provide access to the
underlying contents of the ``blob`` or ``string`` shape. Providing the
contents of exact documents is useful because it provides developers that
want more granular control over the document the ability to load the document
into any kind of type they want (for example, an Amazon Ion DOM value).

Implementations SHOULD expose exact ``blob`` and ``string`` documents as
separate types in order to make it easier to utilize the underlying UTF-8
string or binary data. For example, the following non-normative example
shows how exact document types could be defined in Java (this example
builds on the example defined in :ref:`inline documents <document-type>`):

.. code-block:: java

    /**
     * An exact blob document.
     */
    interface BlobDocument {
        /**
         * Attempts to parse the blob and return a Document.
         *
         * @return Returns the parsed document on success.
         * @throws RuntimeException if the media type cannot be parsed.
         */
        Document toDocument();

        /**
         * Gets the media type of the document.
         *
         * @return Returns the document's media type.
         */
        String getMediaType();

        /**
         * Gets the underlying bytes of the document.
         *
         * @return Returns the raw bytes of the document.
         */
        byte[] getContents();
    }

    /**
     * An exact string document.
     */
    interface StringDocument {
        /**
         * Attempts to parse the string and return a Document.
         *
         * @return Returns the parsed document on success.
         * @throws RuntimeException if the media type cannot be parsed.
         */
        Document toDocument();

        /**
         * Gets the media type of the document.
         *
         * @return Returns the document's media type.
         */
        String getMediaType();

        /**
         * Gets the underlying contents of the document.
         *
         * @return Returns the raw content of the document.
         */
        String getContents();
    }


Backward compatibility
======================

It is a backward incompatible change to add or remove the ``@exactDocument``
trait from a shape after it is initially shipped as part of a service.

Implementations SHOULD provide support for exact documents in a way that
allows the implementation to add support for parsing new media types without
breaking previously generated code. For example, if a service uses a media type
that is not yet supported in a code generator for a particular programming
language, that service SHOULD be able to ship in that language using an exact
document abstraction that exposes the contents of the document but is unable to
provide structured access to the document. If and when support for that
media type is added to the code generator, the only observable difference to
end users of the generated code SHOULD be that the exact document abstraction
is able to now provide structured access to the document.


JSON support in exact documents
===============================

All Smithy implementations SHOULD, at a minimum, support parsing exact JSON
documents. An exact JSON document is identified as having a ``@mediaType`` trait
that is set to ``application/json`` or that contains ``+json``.


.. _Option type: https://doc.rust-lang.org/std/option/enum.Option.html
.. _Amazon Ion: http://amzn.github.io/ion-docs/docs/spec.html
.. _typed nulls: http://amzn.github.io/ion-docs/docs/spec.html#null
.. _Ion hashing: https://amzn.github.io/ion-hash/docs/spec.html
.. _symbol tables: http://amzn.github.io/ion-docs/docs/symbols.html
