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

package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that the put lifecycle operation of a resource
 * can only be used to create a resource and cannot replace
 * an existing resource.
 */
public final class NoReplaceTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#noReplace");

    public NoReplaceTrait(SourceLocation sourceLocation) {
        super(ID, sourceLocation);
    }

    public NoReplaceTrait() {
        this(SourceLocation.NONE);
    }

    public static final class Provider extends AnnotationTrait.Provider<NoReplaceTrait> {
        public Provider() {
            super(ID, NoReplaceTrait::new);
        }
    }
}
