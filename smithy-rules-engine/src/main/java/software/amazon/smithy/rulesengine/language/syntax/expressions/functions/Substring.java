/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for getting the substring of a string value.
 */
@SmithyUnstableApi
public final class Substring extends LibraryFunction {
    public static final String ID = "substring";
    private static final Definition DEFINITION = new Definition();

    private Substring(FunctionNode functionNode) {
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
     * Creates a {@link Substring} function from the given expressions.
     *
     * @param expression the string to extract from.
     * @param startIndex the starting index.
     * @param stopIndex  the ending index.
     * @param reverse    the reverse order argument.
     * @return The resulting {@link Substring} function.
     */
    public static Substring ofExpressions(
            ToExpression expression,
            ToExpression startIndex,
            ToExpression stopIndex,
            ToExpression reverse
    ) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, expression, startIndex, stopIndex, reverse));
    }

    /**
     * Creates a {@link Substring} function from the given expressions.
     *
     * @param expression the string to extract from.
     * @param startIndex the starting index.
     * @param stopIndex  the ending index.
     * @param reverse    the reverse order argument.
     * @return The resulting {@link Substring} function.
     */
    public static Substring ofExpressions(ToExpression expression, int startIndex, int stopIndex, boolean reverse) {
        return ofExpressions(expression, Expression.of(startIndex), Expression.of(stopIndex), Expression.of(reverse));
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    /**
     * A {@link FunctionDefinition} for the {@link Substring} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Arrays.asList(Type.stringType(), Type.integerType(), Type.integerType(), Type.booleanType());
        }

        @Override
        public Type getReturnType() {
            return Type.optionalType(Type.stringType());
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            String str = arguments.get(0).expectStringValue().getValue();
            int startIndex = arguments.get(1).expectIntegerValue().getValue();
            int stopIndex = arguments.get(2).expectIntegerValue().getValue();
            boolean reverse = arguments.get(3).expectBooleanValue().getValue();
            String result = getSubstring(str, startIndex, stopIndex, reverse);
            return result == null ? Value.emptyValue() : Value.stringValue(result);
        }

        @Override
        public Substring createFunction(FunctionNode functionNode) {
            return new Substring(functionNode);
        }
    }

    /**
     * Get the substring of {@code value} using the same logic as the substring function.
     *
     * @param value Value to get the substring of.
     * @param startIndex Start index.
     * @param stopIndex Stop index.
     * @param reverse True if the slice is from the end.
     * @return the substring value or null.
     */
    public static String getSubstring(String value, int startIndex, int stopIndex, boolean reverse) {
        for (int i = 0; i < value.length(); i++) {
            if (!(value.charAt(i) <= 127)) {
                return null;
            }
        }

        if (startIndex >= stopIndex || value.length() < stopIndex) {
            return null;
        }

        if (!reverse) {
            return value.substring(startIndex, stopIndex);
        } else {
            int revStart = value.length() - stopIndex;
            int revStop = value.length() - startIndex;
            return value.substring(revStart, revStop);
        }
    }
}
