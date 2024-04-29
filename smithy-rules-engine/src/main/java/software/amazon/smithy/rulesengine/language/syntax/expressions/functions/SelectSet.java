/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for selecting only the set (non-empty) elements of an Array.
 */
@SmithyUnstableApi
public final class SelectSet extends LibraryFunction {
    public static final String ID = "selectSet";
    private static final Definition DEFINITION = new Definition();

    private SelectSet(FunctionNode functionNode) {
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
     * Creates a {@link SelectSet} function from the given expressions.
     *
     * @param arg1 the array to select set values from.
     * @return The resulting {@link IsSet} function.
     */
    public static SelectSet ofExpressions(ToExpression arg1) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1));
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitSelectSet(expectOneArgument());
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) {
        Expression arg = expectOneArgument();
        Type type = arg.typeCheck(scope);
        Type arrayOfOptional = Type.arrayType(Type.optionalType(Type.anyType()));
        if (arrayOfOptional.isA(type)) {
            throw new RuntimeException(String.format("Expected %s but found %s",
                    arrayOfOptional, type));
        }

        return Type.arrayType(type.expectArrayType().getMember().expectOptionalType().inner());
    }

    /**
     * A {@link FunctionDefinition} for the {@link SelectSet} function.
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
            return Collections.singletonList(Type.arrayType(Type.optionalType(Type.anyType())));
        }

        @Override
        public Type getReturnType() {
            return Type.arrayType(Type.anyType());
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            // Specialized in the ExpressionVisitor, so this doesn't need an implementation.
            return null;
        }

        @Override
        public SelectSet createFunction(FunctionNode functionNode) {
            return new SelectSet(functionNode);
        }
    }
}
