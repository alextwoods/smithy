/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.eval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.eval.value.EndpointValue;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.visit.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.visit.RuleValueVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A runtime implementation of a rule-set evaluation engine.
 */
@SmithyUnstableApi
public class RuleEvaluator implements ExpressionVisitor<Value> {
    private final Scope<Value> scope = new Scope<>();

    /**
     * Initializes a new {@link RuleEvaluator} instances, and evaluates the provided ruleset and parameter arguments.
     *
     * @param ruleset            The endpoint ruleset.
     * @param parameterArguments The rule-set parameter identifiers and values to evaluate the rule-set against.
     * @return The resulting value from the final matched rule.
     */
    public static Value evaluate(EndpointRuleSet ruleset, Map<Identifier, Value> parameterArguments) {
        return new RuleEvaluator().evaluateRuleSet(ruleset, parameterArguments);
    }

    /**
     * Evaluate the provided ruleset and parameter arguments.
     *
     * @param ruleset            The endpoint ruleset.
     * @param parameterArguments The rule-set parameter identifiers and values to evaluate the rule-set against.
     * @return The resulting value from the final matched rule.
     */
    public Value evaluateRuleSet(EndpointRuleSet ruleset, Map<Identifier, Value> parameterArguments) {
        return scope.inScope(() -> {
            for (Parameter parameter : ruleset.getParameters().toList()) {
                parameter.getDefault().ifPresent(value -> scope.insert(parameter.getName(), value));
            }

            parameterArguments.forEach(scope::insert);

            for (Rule rule : ruleset.getRules()) {
                Value result = handleRule(rule);
                if (!result.isEmpty()) {
                    return result;
                }
            }
            throw new RuntimeException("No rules in ruleset matched");
        });
    }

    @Override
    public Value visitLiteral(Literal literal) {
        return literal.evaluate(this);
    }

    @Override
    public Value visitRef(Reference reference) {
        return scope
                .getValue(reference.getName())
                .orElse(Value.emptyValue());
    }

    @Override
    public Value visitIsSet(Expression fn) {
        return Value.booleanValue(!fn.accept(this).isEmpty());
    }

    @Override
    public Value visitNot(Expression not) {
        return Value.booleanValue(!not.accept(this).expectBooleanValue().getValue());
    }

    @Override
    public Value visitBoolEquals(Expression left, Expression right) {
        return Value.booleanValue(left.accept(this).expectBooleanValue()
                .equals(right.accept(this).expectBooleanValue()));
    }

    @Override
    public Value visitStringEquals(Expression left, Expression right) {
        return Value.booleanValue(left.accept(this).expectStringValue()
                .equals(right.accept(this).expectStringValue()));
    }

    public Value visitGetAttr(GetAttr getAttr) {
        return getAttr.evaluate(getAttr.getTarget().accept(this));
    }

    @Override
    public Value visitLibraryFunction(FunctionDefinition definition, List<Expression> arguments) {
        List<Value> values = new ArrayList<>();
        for (Expression argument : arguments) {
            values.add(argument.accept(this));
        }
        return definition.evaluate(values);
    }

    private Value handleRule(Rule rule) {
        RuleEvaluator self = this;
        return scope.inScope(() -> {
            for (Condition condition : rule.getConditions()) {
                Value value = evaluateCondition(condition);
                if (value.isEmpty() || value.equals(Value.booleanValue(false))) {
                    return Value.emptyValue();
                }
            }
            return rule.accept(new RuleValueVisitor<Value>() {
                @Override
                public Value visitTreeRule(List<Rule> rules) {
                    for (Rule subRule : rules) {
                        Value result = handleRule(subRule);
                        if (!result.isEmpty()) {
                            return result;
                        }
                    }
                    throw new RuntimeException(
                            String.format("no rules inside of tree rule matched—invalid rules (%s)", rule));
                }

                @Override
                public Value visitErrorRule(Expression error) {
                    return error.accept(self);
                }

                @Override
                public Value visitEndpointRule(Endpoint endpoint) {
                    EndpointValue.Builder builder = EndpointValue.builder()
                            .sourceLocation(endpoint)
                            .url(endpoint.getUrl()
                                    .accept(RuleEvaluator.this)
                                    .expectStringValue()
                                    .getValue());
                    endpoint.getProperties()
                            .forEach((key, value) -> builder.putProperty(key.toString(),
                                    value.accept(RuleEvaluator.this)));
                    endpoint.getHeaders()
                            .forEach((name, expressions) -> expressions.forEach(expr -> builder.addHeader(name,
                                    expr.accept(RuleEvaluator.this).expectStringValue().getValue())));
                    return builder.build();
                }
            });
        });
    }

    public Value evaluateCondition(Condition condition) {
        Value value = condition.getFn().accept(this);
        if (!value.isEmpty()) {
            condition.getResult().ifPresent(res -> scope.insert(res, value));
        }
        return value;
    }
}
