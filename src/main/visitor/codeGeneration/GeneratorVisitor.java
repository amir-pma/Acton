package main.visitor.codeGeneration;

import main.ast.node.Main;
import main.ast.node.Program;
import main.ast.node.declaration.ActorDeclaration;
import main.ast.node.declaration.ActorInstantiation;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.node.statement.*;

public interface GeneratorVisitor {
    void visit (Program program);

    //Declarations
    void visit (ActorDeclaration actorDeclaration);
    void visit (HandlerDeclaration handlerDeclaration);
    void visit (VarDeclaration varDeclaration);

    //main
    void visit(Main mainActors);
    void visit(ActorInstantiation actorInstantiation);

    //Expressions
    String visit(UnaryExpression unaryExpression);
    String visit(BinaryExpression binaryExpression);
    String visit(ArrayCall arrayCall);
    String visit(ActorVarAccess actorVarAccess);
    String visit(Identifier identifier);
    String visit(Self self);
    String visit(Sender sender);
    String visit(BooleanValue value);
    String visit(IntValue value);
    String visit(StringValue value);

    //Statements
    void visit(Block block);
    void visit(Conditional conditional);
    void visit(For loop);
    void visit(Break breakLoop);
    void visit(Continue continueLoop);
    void visit(MsgHandlerCall msgHandlerCall);
    void visit(Print print);
    void visit(Assign assign);

}
