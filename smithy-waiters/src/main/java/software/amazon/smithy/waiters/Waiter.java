/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.waiters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines an individual operation waiter.
 */
public final class Waiter implements ToNode, ToSmithyBuilder<Waiter> {

    private static final String DOCUMENTATION = "documentation";
    private static final String ACCEPTORS = "acceptors";
    private static final String MIN_DELAY = "minDelay";
    private static final String MAX_DELAY = "maxDelay";
    private static final int DEFAULT_MIN_DELAY = 2;
    private static final int DEFAULT_MAX_DELAY = 120;
    private static final Set<String> KEYS = SetUtils.of(DOCUMENTATION, ACCEPTORS, MIN_DELAY, MAX_DELAY);

    private final String documentation;
    private final List<Acceptor> acceptors;
    private final int minDelay;
    private final int maxDelay;

    private Waiter(Builder builder) {
        this.documentation = builder.documentation;
        this.acceptors = ListUtils.copyOf(builder.acceptors);
        this.minDelay = builder.minDelay;
        this.maxDelay = builder.maxDelay;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SmithyBuilder<Waiter> toBuilder() {
        return builder()
                .documentation(getDocumentation().orElse(null))
                .acceptors(getAcceptors())
                .minDelay(getMinDelay())
                .maxDelay(getMaxDelay());
    }

    /**
     * Create a {@code Waiter} from a {@link Node}.
     *
     * @param node {@code Node} to create the {@code Waiter} from.
     * @return Returns the created {@code Waiter}.
     * @throws ExpectationNotMetException if the given {@code node} is invalid.
     */
    public static Waiter fromNode(Node node) {
        ObjectNode value = node.expectObjectNode().warnIfAdditionalProperties(KEYS);
        Builder builder = builder();
        value.getStringMember(DOCUMENTATION).map(StringNode::getValue).ifPresent(builder::documentation);
        for (Node entry : value.expectArrayMember(ACCEPTORS).getElements()) {
            builder.addAcceptor(Acceptor.fromNode(entry));
        }

        value.getNumberMember(MIN_DELAY).map(NumberNode::getValue).map(Number::intValue).ifPresent(builder::minDelay);
        value.getNumberMember(MAX_DELAY).map(NumberNode::getValue).map(Number::intValue).ifPresent(builder::maxDelay);

        return builder.build();
    }

    /**
     * Gets the documentation of the waiter.
     *
     * @return Return the optional documentation.
     */
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * Gets the list of {@link Acceptor}s.
     *
     * @return Returns the acceptors of the waiter.
     */
    public List<Acceptor> getAcceptors() {
        return acceptors;
    }

    /**
     * Gets the minimum amount of time to wait between retries
     * in seconds.
     *
     * @return Gets the minimum retry wait time in seconds.
     */
    public int getMinDelay() {
        return minDelay;
    }

    /**
     * Gets the maximum amount of time allowed to wait between
     * retries in seconds.
     *
     * @return Gets the maximum retry wait time in seconds.
     */
    public int getMaxDelay() {
        return maxDelay;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember(DOCUMENTATION, getDocumentation().map(Node::from))
                .withMember(ACCEPTORS, getAcceptors().stream().map(Acceptor::toNode).collect(ArrayNode.collect()));

        // Don't serialize default values for minDelay and maxDelay.
        if (minDelay != DEFAULT_MIN_DELAY) {
            builder.withMember(MIN_DELAY, minDelay);
        }
        if (maxDelay != DEFAULT_MAX_DELAY) {
            builder.withMember(MAX_DELAY, maxDelay);
        }

        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Waiter)) {
            return false;
        }

        Waiter waiter = (Waiter) o;
        return minDelay == waiter.minDelay
               && maxDelay == waiter.maxDelay
               && Objects.equals(documentation, waiter.documentation)
               && acceptors.equals(waiter.acceptors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentation, acceptors, minDelay, maxDelay);
    }

    public static final class Builder implements SmithyBuilder<Waiter> {

        private String documentation;
        private final List<Acceptor> acceptors = new ArrayList<>();
        private int minDelay = DEFAULT_MIN_DELAY;
        private int maxDelay = DEFAULT_MAX_DELAY;

        private Builder() {}

        @Override
        public Waiter build() {
            return new Waiter(this);
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder clearAcceptors() {
            this.acceptors.clear();
            return this;
        }

        public Builder acceptors(List<Acceptor> acceptors) {
            clearAcceptors();
            acceptors.forEach(this::addAcceptor);
            return this;
        }

        public Builder addAcceptor(Acceptor acceptor) {
            this.acceptors.add(Objects.requireNonNull(acceptor));
            return this;
        }

        public Builder minDelay(int minDelay) {
            this.minDelay = minDelay;
            return this;
        }

        public Builder maxDelay(int maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }
    }
}
