/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.ArrayValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function that returns true if Any elements of an array are true/false.
 */
@SmithyUnstableApi
public final class Any extends LibraryFunction {
    public static final String ID = "any";
    private static final Definition DEFINITION = new Definition();

    private Any(FunctionNode functionNode) {
        super(DEFINITION, functionNode);
    }

    /**
     * Gets the {@link FunctionDefinition} implementation.
     *
     * @return the function definition.
     */
    public static Definition getDefinition() {
        return DEFINITION;
    }

    /**
     * Creates a {@link Any} function from the given expressions.
     *
     * @param arg1 the array to test.
     * @param arg2 the value to test against.
     * @return The resulting {@link Any} function.
     */
    public static Any ofExpressions(ToExpression arg1, ToExpression arg2) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1, arg2));
    }

    /**
     * Creates a {@link Any} function from the given expressions.
     *
     * @param arg1 the array to test.
     * @param arg2 the value to test against.
     * @return The resulting {@link Any} function.
     */
    public static Any ofExpressions(ToExpression arg1, Boolean arg2) {
        return ofExpressions(arg1, Expression.of(arg2));
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    /**
     * A {@link FunctionDefinition} for the {@link Any} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Arrays.asList(
                    Type.arrayType(Type.booleanType()),
                    Type.booleanType()
            );
        }

        @Override
        public Type getReturnType() {
            return Type.booleanType();
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            ArrayValue values = arguments.get(0).expectArrayValue();
            boolean expected = arguments.get(1).expectBooleanValue().getValue();
            return Value.booleanValue(
                    values.getValues().stream()
                            .anyMatch(v -> v.expectBooleanValue().getValue() == expected)
            );
        }

        @Override
        public Any createFunction(FunctionNode functionNode) {
            return new Any(functionNode);
        }
    }
}
