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

import java.util.logging.Logger;
import software.amazon.smithy.aws.traits.apigateway.AuthorizersTrait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SmithyOpenApiPlugin;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * Adds API Gateway authorizers to their corresponding security schemes.
 *
 * <p>The {@link AuthorizersTrait} is applied to a service shape to define
 * a custom API Gateway authorizer. This trait is a map of authentication
 * scheme name to the authorizer definition. This plugin finds each
 * authorizer and applies the API Gateway specific OpenAPI extension
 * {@code x-amazon-apigateway-authorizer} to define the authorizer in the
 * OpenAPI security scheme that corresponds to the referenced authentication
 * scheme.
 */
public final class AddAuthorizers implements SmithyOpenApiPlugin {
    private static final String EXTENSION_NAME = "x-amazon-apigateway-authorizer";
    private static final String CLIENT_EXTENSION_NAME = "x-amazon-apigateway-authtype";
    private static final Logger LOGGER = Logger.getLogger(AddApiKeySource.class.getName());

    @Override
    public SecurityScheme updateSecurityScheme(
            Context context,
            String authName,
            String securitySchemeName,
            SecurityScheme securityScheme
    ) {
        return context.getService().getTrait(AuthorizersTrait.class)
                .map(trait -> addAuthorizers(authName, securitySchemeName, securityScheme, trait))
                .orElse(securityScheme);
    }

    private SecurityScheme addAuthorizers(
            String authName,
            String securitySchemeName,
            SecurityScheme securityScheme,
            AuthorizersTrait trait
    ) {
        if (!trait.getAllAuthorizers().containsKey(authName)) {
            return securityScheme;
        }

        var authorizer = trait.getAllAuthorizers().get(authName);
        LOGGER.fine(() -> String.format(
                "Adding the `%s` OpenAPI extension to the `%s` security scheme based on the `%s` trait for the "
                + "`%s` authentication scheme.",
                EXTENSION_NAME, securitySchemeName, trait.getName(), authName));

        var builder = securityScheme.toBuilder();
        authorizer.getClientType().ifPresent(type -> builder.putExtension(CLIENT_EXTENSION_NAME, type));
        var authorizerNode = authorizer.toNode().expectObjectNode().withoutMember("clientType");
        builder.putExtension(EXTENSION_NAME, authorizerNode);
        return builder.build();
    }
}
