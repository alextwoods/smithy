/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

public final class DataTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("aws.api#data");

    public DataTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public static final class Provider extends StringTrait.Provider<DataTrait> {
        public Provider() {
            super(ID, DataTrait::new);
        }
    }
}
