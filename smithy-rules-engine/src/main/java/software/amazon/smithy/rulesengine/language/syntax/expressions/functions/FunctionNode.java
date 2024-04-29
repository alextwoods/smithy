/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Parsed but not validated function contents containing the `fn` name and `argv`.
 */
@SmithyUnstableApi
public final class FunctionNode implements FromSourceLocation, ToNode, ToSmithyBuilder<FunctionNode> {
    private static final String ARGV = "argv";
    private static final String FN = "fn";
    private static final String MAP = "map";

    private final List<Expression> arguments = new ArrayList<>();
    private final StringNode name;
    private final Optional<Expression> map;
    private final SourceLocation sourceLocation;

    FunctionNode(Builder builder) {
        for (ToExpression expression : builder.argv.get()) {
            arguments.add(expression.toExpression());
        }
        name = SmithyBuilder.requiredState("functionName", builder.function);
        map = builder.map.map((e) -> e.toExpression());
        sourceLocation = builder.getSourceLocation();
    }

    /**
     * Constructs a {@link FunctionNode} for the given function name and arguments.
     *
     * @param functionName the function name.
     * @param arguments    zero or more expressions as arguments to the function.
     * @return the {@link FunctionNode}.
     */
    public static FunctionNode ofExpressions(String functionName, ToExpression... arguments) {
        return ofExpressions(functionName, SourceLocation.none(), arguments);
    }

    /**
     * Constructs a {@link FunctionNode} for the given function name and arguments.
     *
     * @param functionName   the function name.
     * @param sourceLocation the source location for the function.
     * @param arguments      zero or more expressions as arguments to the function.
     * @return the {@link FunctionNode}.
     */
    public static FunctionNode ofExpressions(
            String functionName,
            FromSourceLocation sourceLocation,
            ToExpression... arguments
    ) {
        return builder()
                .sourceLocation(sourceLocation)
                .name(StringNode.from(functionName))
                .arguments(Arrays.asList(arguments))
                .build();
    }

    /**
     * Constructs a {@link FunctionNode} that maps the given function over an array and arguments.
     *
     * @param functionName the function name.
     * @param on           an expression that evaluates to an array to map this function on.
     * @param arguments    zero or more expressions as arguments to the function.
     * @return the {@link FunctionNode}.
     */
    public static FunctionNode mapOnExpression(String functionName, ToExpression on, ToExpression... arguments) {
        return mapOnExpression(functionName, SourceLocation.none(), on, arguments);
    }

    /**
     * Constructs a {@link FunctionNode} that maps the given function over an array and arguments.
     *
     * @param functionName   the function name.
     * @param sourceLocation the source location for the function.
     * @param on             an expression that evaluates to an array to map this function on.
     * @param arguments      zero or more expressions as arguments to the function.
     * @return the {@link FunctionNode}.
     */
    public static FunctionNode mapOnExpression(
            String functionName,
            FromSourceLocation sourceLocation,
            ToExpression on,
            ToExpression... arguments
    ) {
        return builder()
                .sourceLocation(sourceLocation)
                .name(StringNode.from(functionName))
                .map(on)
                .arguments(Arrays.asList(arguments))
                .build();
    }

    /**
     * Returns a new builder instance.
     *
     * @return the new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a {@link FunctionNode} from the provided {@link ObjectNode}.
     *
     * @param function the node describing the function.
     * @return the {@link FunctionNode}.
     */
    public static FunctionNode fromNode(ObjectNode function) {
        List<Expression> arguments = new ArrayList<>();
        for (Node node : function.expectArrayMember(ARGV).getElements()) {
            arguments.add(Expression.fromNode(node));
        }
        return builder()
                .sourceLocation(function)
                .name(function.expectStringMember(FN))
                .map(function.getMember(MAP).map((node) -> Expression.fromNode(node)))
                .arguments(arguments)
                .build();
    }

    /**
     * Returns an expression representing this function.
     *
     * @return this function as an expression.
     */
    public Expression createFunction() {
        return EndpointRuleSet.createFunctionFactory().apply(this)
                .orElseThrow(() -> new RuleError(new SourceException(
                        String.format("`%s` is not a valid function", name), name)));
    }

    /**
     * Gets the name for the function described in this node.
     *
     * @return the name for the function.
     */
    public String getName() {
        return name.getValue();
    }

    /**
     * Gets the list of argument {@link Expression}s to the function described in this node.
     *
     * @return the list of arguments.
     */
    public List<Expression> getArguments() {
        return arguments;
    }

    public boolean isMap() {
        return map.isPresent();
    }

    public Optional<Expression> getMap() {
        return map;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Returns a new builder instance for this {@link FunctionNode}.
     *
     * @return the new builder instance.
     */
    public Builder toBuilder() {
        return builder()
                .sourceLocation(sourceLocation)
                .map(map)
                .name(name)
                .arguments(arguments);
    }

    @Override
    public Node toNode() {
        ArrayNode.Builder argumentsBuilder = ArrayNode.builder();
        for (Expression argument : arguments) {
            argumentsBuilder.withValue(argument.toNode());
        }

        ObjectNode.Builder builder = ObjectNode.builder()
                .withMember(FN, name)
                .withMember(ARGV, argumentsBuilder.build());

        map.ifPresent((exp) -> builder.withMember(MAP, exp.toNode()));
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FunctionNode functionNode = (FunctionNode) o;
        return name.equals(functionNode.name)
                && arguments.equals(functionNode.arguments)
                && map.equals(functionNode.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arguments);
    }

    /**
     * A builder used to create a {@link FunctionNode} class.
     */
    public static final class Builder extends RulesComponentBuilder<Builder, FunctionNode> {
        private final BuilderRef<List<ToExpression>> argv = BuilderRef.forList();
        private StringNode function;
        private Optional<? extends ToExpression> map = Optional.empty();

        private Builder() {
            super(SourceLocation.none());
        }

        public Builder arguments(List<? extends ToExpression> argv) {
            this.argv.clear();
            this.argv.get().addAll(argv);
            return this;
        }

        public Builder name(StringNode functionName) {
            this.function = functionName;
            return this;
        }

        public Builder map(ToExpression map) {
            this.map = Optional.ofNullable(map);
            return this;
        }

        public Builder map(Optional<? extends ToExpression> map) {
            this.map = map;
            return this;
        }

        @Override
        public FunctionNode build() {
            return new FunctionNode(this);
        }
    }
}
