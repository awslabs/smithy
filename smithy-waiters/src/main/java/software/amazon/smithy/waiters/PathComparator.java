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

import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;

/**
 * Defines a comparison to perform in a ListPathMatcher.
 */
public enum PathComparator implements ToNode {

    /** Matches if all values in the list matches the expected string. */
    ALL_STRING_EQUALS("allStringEquals"),

    /** Matches if any value in the list matches the expected string. */
    ANY_STRING_EQUALS("anyStringEquals"),

    /** Matches if the return value is a string that is equal to the expected string. */
    STRING_EQUALS("stringEquals"),

    /** Matches if the return value is a boolean that is equal to the string literal 'true' or 'false'. */
    BOOLEAN_EQUALS("booleanEquals");

    private final String asString;

    PathComparator(String asString) {
        this.asString = asString;
    }

    /**
     * Creates a {@code ListPathComparator} from a {@link Node}.
     * @param node Node to create the {@code ListPathComparator} from.
     * @return Returns the created {@code ListPathComparator}.
     * @throws ExpectationNotMetException if the given {@code node} is invalid.
     */
    public static PathComparator fromNode(Node node) {
        String value = node.expectStringNode().getValue();
        for (PathComparator comparator : values()) {
            if (comparator.toString().equals(value)) {
                return comparator;
            }
        }

        throw new ExpectationNotMetException("Expected valid path comparator, but found " + value, node);
    }

    @Override
    public String toString() {
        return asString;
    }

    @Override
    public Node toNode() {
        return Node.from(toString());
    }
}
