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

package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Checks for prefix headers in the input or output of an operation,
 * and handles the settings for throwing an exception or warning if
 * they're found.
 *
 * <p>Custom values for this setting need to be handled in a
 * {@link OpenApiProtocol}.
 */
@SmithyInternalApi
public class CheckForPrefixHeaders implements OpenApiMapper {
    private static final Logger LOGGER = Logger.getLogger(CheckForGreedyLabels.class.getName());

    @Override
    public byte getOrder() {
        return -128;
    }

    @Override
    public void before(Context<? extends Trait> context, OpenApi.Builder builder) {
        HttpBindingIndex httpBindings = context.getModel().getKnowledge(HttpBindingIndex.class);
        context.getModel().shapes(OperationShape.class).forEach(operation -> {
            check(context, httpBindings.getRequestBindings(operation, HttpBinding.Location.PREFIX_HEADERS));
            checkForResponseHeaders(context, httpBindings, operation);
            operation.getErrors().forEach(error -> checkForResponseHeaders(context, httpBindings, error));
        });
    }

    private void checkForResponseHeaders(
            Context<? extends Trait> context,
            HttpBindingIndex bindingIndex,
            ToShapeId shapeId
    ) {
        check(context, bindingIndex.getResponseBindings(shapeId, HttpBinding.Location.PREFIX_HEADERS));
    }

    private void check(Context<? extends Trait> context, List<HttpBinding> bindings) {
        OpenApiConfig.HttpPrefixHeadersStrategy strategy = context.getConfig().getOnHttpPrefixHeaders();

        for (HttpBinding binding : bindings) {
            switch (strategy) {
                case WARN:
                    LOGGER.warning(createMessage(binding));
                    break;
                case FAIL:
                    throw new OpenApiException(createMessage(binding));
                default:
                    break;
            }
        }
    }

    private static String createMessage(HttpBinding binding) {
        MemberShape member = binding.getMember();
        return String.format(
                "The `httpPrefixHeaders` trait is not supported by OpenAPI and was found on `%s`", member.getId());
    }
}
