/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.parameters;

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.type.ArrayType;
import software.amazon.smithy.rulesengine.language.evaluation.type.BooleanType;
import software.amazon.smithy.rulesengine.language.evaluation.type.StringType;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * The types that parameters can be.
 */
@SmithyUnstableApi
public enum ParameterType {
    /**
     * A "string" parameter type.
     */
    STRING,

    /**
     * A "boolean" parameter type.
     */
    BOOLEAN,

    /**
     * An array (list) of strings parameter type.
     */
    STRING_ARRAY;

    /**
     * Creates a {@link ParameterType} of a specific type from the given Node information.
     *
     * @param node the node to deserialize.
     * @return the created ParameterType.
     * @throws RuleError when the value is not a valid ParameterType.
     */
    public static ParameterType fromNode(StringNode node) throws RuleError {
        String value = node.getValue();
        if (value.equalsIgnoreCase("String")) {
            return STRING;
        }
        if (value.equalsIgnoreCase("Boolean")) {
            return BOOLEAN;
        }
        if (value.equalsIgnoreCase("StringArray")) {
            return STRING_ARRAY;
        }
        throw new RuleError(new SourceException(
                String.format("Unexpected parameter type `%s`. Expected `String`, `Boolean`, or `StringArray`.",
                        value), node));
    }

    /**
     * Creates a {@link ParameterType} of a specific type from the given Node type.
     *
     * @param node the node to deserialize.
     * @return the created ParameterType.
     * @throws RuleError when the node would not create a valid ParameterType.
     */
    public static ParameterType fromNode(Node node) throws RuleError {
        if (node.isStringNode()) {
            return STRING;
        }
        if (node.isBooleanNode()) {
            return BOOLEAN;
        }
        if (node.isArrayNode()) {
            // TODO: confirm the member types are String
            return STRING_ARRAY;
        }
        throw new RuleError(new SourceException(
                String.format("Unexpected parameter type `%s`. Expected a string or boolean.", node.getType()), node));
    }

    /**
     * Creates a {@link ParameterType} of a specific type from the given parameter Type.
     *
     * @param type the Type to create from.
     * @return the created ParameterType.
     * @throws RuntimeException when the value would not create a valid ParameterType.
     */
    public static ParameterType fromType(Type type) {
        if (type instanceof StringType) {
            return STRING;
        }
        if (type instanceof BooleanType) {
            return BOOLEAN;
        }
        if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            if (arrayType.getMember().isA(Type.stringType()) || arrayType.getMember().isA(Type.emptyType())) {
                return STRING_ARRAY;
            }
        }
        throw new RuntimeException(
                String.format("Unexpected parameter type `%s`. Expected a string, boolean or array<string>", type));
    }

    /**
     * Creates a {@link ParameterType} of a specific type from the given ShapeType.
     *
     * @param type the ShapeType to create from.
     * @return the created ParameterType.
     * @throws RuntimeException when the value would not create a valid ParameterType.
     */
    public static ParameterType fromShapeType(ShapeType type) {
        if (type == ShapeType.STRING) {
            return STRING;
        }
        if (type == ShapeType.BOOLEAN) {
            return BOOLEAN;
        }
        if (type == ShapeType.LIST) {
            // TODO: check member type
            return STRING_ARRAY;
        }
        throw new RuntimeException(
                String.format("Unexpected parameter type `%s`. Expected string or boolean.", type));
    }

    @Override
    public String toString() {
        switch (this) {
            case STRING:
                return "String";
            case BOOLEAN:
                return "Boolean";
            case STRING_ARRAY:
                return "StringArray";
            default:
                return "Unknown Type";
        }
    }
}
