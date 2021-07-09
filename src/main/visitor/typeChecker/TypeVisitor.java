package main.visitor.typeChecker;

import main.ast.node.expression.*;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.type.Type;

public interface TypeVisitor {
    Type visit(UnaryExpression unaryExpression);
    Type visit(BinaryExpression binaryExpression);
    Type visit(ArrayCall arrayCall);
    Type visit(ActorVarAccess actorVarAccess);
    Type visit(Identifier identifier);
    Type visit(Self self);
    Type visit(Sender sender);
    Type visit(BooleanValue value);
    Type visit(IntValue value);
    Type visit(StringValue value);
}
