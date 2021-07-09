package main.visitor.typeChecker;

import main.ast.node.Program;
import main.ast.node.declaration.ActorDeclaration;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.operators.BinaryOperator;
import main.ast.node.expression.operators.UnaryOperator;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.type.Type;
import main.ast.type.actorType.ActorType;
import main.ast.type.arrayType.ArrayType;
import main.ast.type.noType.NoType;
import main.ast.type.primitiveType.BooleanType;
import main.ast.type.primitiveType.IntType;
import main.ast.type.primitiveType.StringType;
import main.ast.type.senderType.SenderType;
import main.compileError.CompileErrorException;
import main.compileError.TypeCheckErrors.*;
import main.symbolTable.*;
import main.symbolTable.itemException.ItemNotFoundException;
import main.symbolTable.symbolTableVariableItem.SymbolTableVariableItem;

import java.util.ArrayList;

public class ExpressionTypeChecker implements TypeVisitor {
    private boolean isInInitial;
    private Program program;
    private ActorDeclaration actorDeclaration;
    private HandlerDeclaration handlerDeclaration;
    private ActorVarAccess currentActorVarAccess;

    public void setInInitial(boolean isInInitial_) {
        this.isInInitial = isInInitial_;
    }

    public void setProgram(Program program_) {
        this.program = program_;
    }

    public void setActorDeclaration(ActorDeclaration actorDeclaration_) {
        this.actorDeclaration = actorDeclaration_;
    }

    public void setMsgHandlerDeclaration(HandlerDeclaration handlerDeclaration_) {
        this.handlerDeclaration = handlerDeclaration_;
    }

    private void addError(CompileErrorException compileErrorException) {
        TypeChecker.errors.add(compileErrorException);
        (TypeChecker.numOfErrors)++;
    }

    public boolean isLvalue(Expression expression) {
        return ((expression instanceof ArrayCall) || (expression instanceof ActorVarAccess) || (expression instanceof Identifier));
    }

    private int getIndex(String name) {
        int i = -1;
        for(ActorDeclaration actorDeclaration : program.getActors()) {
            i++;
            if(actorDeclaration.getName().getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isSubType(String parentName, String childName) {
        if(childName.equals(parentName)) {
            return true;
        }
        ArrayList<Boolean> visited = new ArrayList<>();
        for(ActorDeclaration actorDeclaration : program.getActors()) {
            visited.add(false);
        }

        SymbolTableActorItem node = null;
        try {
            SymbolTableItem symbolTableActorItem = SymbolTable.root.get(SymbolTableActorItem.STARTKEY + childName);
            node = (SymbolTableActorItem)symbolTableActorItem;
        } catch (ItemNotFoundException e) { }

        if(node != null && node.getParentName() != null) {
            try {
                SymbolTableItem nodeTemp = SymbolTable.root.get(SymbolTableActorItem.STARTKEY + node.getParentName());
                node = (SymbolTableActorItem) nodeTemp;
            } catch (ItemNotFoundException exception) {}
        }
        else {
            return false;
        }

        while(true) {
            if((node == null) || (node.getActorDeclaration().getName().getName().equals(childName))) {
                return false;
            }
            visited.set(getIndex(node.getActorDeclaration().getName().getName()), true);
            if(node.getActorDeclaration().getName().getName().equals(parentName)) {
                return true;
            }
            else {
                if(node.getParentName() != null) {
                    try {
                        SymbolTableItem nodeTemp = SymbolTable.root.get(SymbolTableActorItem.STARTKEY + node.getParentName());
                        node = (SymbolTableActorItem) nodeTemp;
                        if(visited.get(getIndex(node.getActorDeclaration().getName().getName()))){
                            node = null;
                        }
                    } catch (ItemNotFoundException exception) {
                        node = null;
                    }
                }
                else {
                    node = null;
                }
            }
        }

    }

    public Type visitExpr( Expression expr )
    {
        if( expr == null )
            return null;
        else if( expr instanceof UnaryExpression )
            return this.visit( ( UnaryExpression ) expr );
        else if( expr instanceof BinaryExpression )
            return this.visit( ( BinaryExpression ) expr );
        else if( expr instanceof ArrayCall )
            return this.visit( ( ArrayCall ) expr );
        else if( expr instanceof ActorVarAccess )
            return this.visit( ( ActorVarAccess ) expr );
        else if( expr instanceof Identifier )
            return this.visit( ( Identifier ) expr );
        else if( expr instanceof Self )
            return this.visit( ( Self ) expr );
        else if( expr instanceof Sender )
            return this.visit( ( Sender ) expr );
        else if( expr instanceof BooleanValue )
            return this.visit( ( BooleanValue ) expr );
        else if( expr instanceof IntValue )
            return this.visit( ( IntValue ) expr );
        else if( expr instanceof StringValue )
            return this.visit( ( StringValue ) expr );
        return null;
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        Type operandType = visitExpr(unaryExpression.getOperand());
        UnaryOperator operator = unaryExpression.getUnaryOperator();
        boolean hasError = false;

        if((operator == UnaryOperator.not) && !((operandType instanceof NoType) || (operandType instanceof BooleanType))) {
            addError(new UnsupportedOperandType(unaryExpression.getLine(), operator.name()));
            hasError = true;
        }
        else if(!(operator == UnaryOperator.not) && !((operandType instanceof NoType) || (operandType instanceof IntType))) {
            addError(new UnsupportedOperandType(unaryExpression.getLine(), operator.name()));
            hasError = true;
        }

        if(!(operandType instanceof NoType) && !(operator == UnaryOperator.not) && !(operator == UnaryOperator.minus) && !(isLvalue(unaryExpression.getOperand()))) {
            addError(new IncDecOperandNotLvalue(unaryExpression.getLine(), ((operator == UnaryOperator.postdec) || (operator == UnaryOperator.predec)) ? 1 : 0));
            hasError = true;
        }

        if(hasError || (operandType instanceof NoType))
            return new NoType();
        return operandType;
    }

    @Override
    public Type visit(BinaryExpression binaryExpression) {

        Type leftType = visitExpr(binaryExpression.getLeft());
        Type rightType = visitExpr(binaryExpression.getRight());
        BinaryOperator operator = binaryExpression.getBinaryOperator();

        if((operator == BinaryOperator.eq) || (operator == BinaryOperator.neq)) {
            if (!(((leftType instanceof IntType) && (rightType instanceof IntType)) ||
                ((leftType instanceof StringType) && (rightType instanceof StringType)) ||
                ((leftType instanceof BooleanType) && (rightType instanceof BooleanType)) ||
                ((leftType instanceof ArrayType) && (rightType instanceof ArrayType) && (((ArrayType)leftType).getSize() == ((ArrayType)rightType).getSize())) ||
                ((leftType instanceof ActorType) && (rightType instanceof ActorType)) ||
                ((leftType instanceof SenderType) && (rightType instanceof ActorType)) ||
                ((leftType instanceof ActorType) && (rightType instanceof SenderType)) ||
                ((leftType instanceof SenderType) && (rightType instanceof SenderType)) ||
                ((leftType instanceof NoType) || (rightType instanceof NoType)))) {

                addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                return new NoType();
            }
            if((leftType instanceof NoType) || (rightType instanceof NoType))
                return new NoType();
            return new BooleanType();
        }

        if((operator == BinaryOperator.gt) || (operator == BinaryOperator.lt)) {
            if (!(((leftType instanceof IntType) && (rightType instanceof IntType)) ||
                    ((leftType instanceof IntType) && (rightType instanceof NoType)) ||
                    ((leftType instanceof NoType) && (rightType instanceof IntType)) ||
                    ((leftType instanceof NoType) && (rightType instanceof NoType)))) {

                addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                return new NoType();
            }
            if((leftType instanceof NoType) || (rightType instanceof NoType))
                return new NoType();
            return new BooleanType();
        }

        if((operator == BinaryOperator.add) || (operator == BinaryOperator.sub) ||
            (operator == BinaryOperator.mult) || (operator == BinaryOperator.div) || (operator == BinaryOperator.mod)) {

            if (!(((leftType instanceof IntType) && (rightType instanceof IntType)) ||
                    ((leftType instanceof IntType) && (rightType instanceof NoType)) ||
                    ((leftType instanceof NoType) && (rightType instanceof IntType)) ||
                    ((leftType instanceof NoType) && (rightType instanceof NoType)))) {

                addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                return new NoType();
            }
            if((leftType instanceof NoType) || (rightType instanceof NoType))
                return new NoType();
            return new IntType();
        }

        if((operator == BinaryOperator.or) || (operator == BinaryOperator.and)) {
            if (!(((leftType instanceof BooleanType) && (rightType instanceof BooleanType)) ||
                    ((leftType instanceof BooleanType) && (rightType instanceof NoType)) ||
                    ((leftType instanceof NoType) && (rightType instanceof BooleanType)) ||
                    ((leftType instanceof NoType) && (rightType instanceof NoType)))) {

                addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                return new NoType();
            }
            if((leftType instanceof NoType) || (rightType instanceof NoType))
                return new NoType();
            return new BooleanType();
        }

        if(operator == BinaryOperator.assign) {
            boolean hasError = false;

            if (!(((leftType instanceof IntType) && (rightType instanceof IntType)) ||
                    ((leftType instanceof StringType) && (rightType instanceof StringType)) ||
                    ((leftType instanceof BooleanType) && (rightType instanceof BooleanType)) ||
                    ((leftType instanceof ArrayType) && (rightType instanceof ArrayType)) ||
                    ((leftType instanceof ActorType) && (rightType instanceof SenderType)) ||
                    ((leftType instanceof SenderType) && (rightType instanceof ActorType)) ||
                    ((leftType instanceof SenderType) && (rightType instanceof SenderType)) ||
                    ((leftType instanceof ActorType) && (rightType instanceof ActorType) && isSubType(((ActorType)(leftType)).getName().getName(), ((ActorType)(rightType)).getName().getName())) ||
                    ((leftType instanceof NoType) || (rightType instanceof NoType)))) {

                addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                hasError = true;
            }

            if ((leftType instanceof ArrayType) && (rightType instanceof ArrayType) && (((ArrayType)leftType).getSize() != ((ArrayType)rightType).getSize())) {
                addError(new NotEqualArraySizes(binaryExpression.getLine()));
                hasError = true;
            }

            if(!isLvalue(binaryExpression.getLeft())) {
                addError(new LeftSideNotLvalue(binaryExpression.getLine()));
                hasError = true;
            }

            if(hasError || (leftType instanceof NoType) || (rightType instanceof NoType))
                return new NoType();
            return leftType;
        }

        return null;
    }

    @Override
    public Type visit(ArrayCall arrayCall) {
        Type operandType = visitExpr(arrayCall.getArrayInstance());
        Type indexType = visitExpr(arrayCall.getIndex());
        boolean hasError = false;

        if(!((operandType instanceof NoType) || (operandType instanceof ArrayType))) {
            addError(new ArrayCallOnNotArray(arrayCall.getLine()));
            hasError = true;
        }
        if(!((indexType instanceof NoType) || (indexType instanceof IntType))) {
            addError(new ArrayIndexNotInt(arrayCall.getLine()));
            hasError = true;
        }

        if(hasError || (indexType instanceof NoType) || (operandType instanceof NoType)) {
            return new NoType();
        }
        return new IntType();
    }

    @Override
    public Type visit(ActorVarAccess actorVarAccess) {
        currentActorVarAccess = actorVarAccess;
        Type selfType = visitExpr(actorVarAccess.getSelf());
        if(selfType instanceof NoType) {
            return selfType;
        }

        SymbolTableActorItem symbolTableActorItem = null;
        try {
            SymbolTableItem symbolTableItem = SymbolTable.root.get(SymbolTableActorItem.STARTKEY + ((ActorType)selfType).getName().getName());
            symbolTableActorItem = (SymbolTableActorItem) symbolTableItem;
        } catch (ItemNotFoundException e) {}

        if(symbolTableActorItem == null) return new NoType();

        Type variableType = null;
        try {
            SymbolTableItem symbolTableItem = symbolTableActorItem.getActorSymbolTable().get(SymbolTableVariableItem.STARTKEY + actorVarAccess.getVariable().getName());
            variableType = ((SymbolTableVariableItem)symbolTableItem).getType();
            return variableType;
        } catch (ItemNotFoundException e) {
            addError(new VariableNotDeclared(actorVarAccess.getLine(), actorVarAccess.getVariable().getName()));
            return new NoType();
        }
    }

    @Override
    public Type visit(Identifier identifier) {
        if(handlerDeclaration == null) {
            SymbolTable mainSymbolTable = null;
            try {
                SymbolTableItem symbolTableItem = SymbolTable.root.get(SymbolTableMainItem.STARTKEY + "main");
                mainSymbolTable = ((SymbolTableMainItem)symbolTableItem).getMainSymbolTable();
            } catch (ItemNotFoundException e) {}

            if(mainSymbolTable == null) return new NoType();

            Type type = null;
            try {
                SymbolTableItem symbolTableItem = mainSymbolTable.get(SymbolTableVariableItem.STARTKEY + identifier.getName());
                type = ((SymbolTableVariableItem)symbolTableItem).getType();
            } catch (ItemNotFoundException e) {
                addError(new VariableNotDeclared(identifier.getLine(), identifier.getName()));
                return new NoType();
            }
            return type;
        }
        else {
            SymbolTable actorSymbolTable = null;
            SymbolTableActorItem symbolTableActorItem = null;
            try {
                SymbolTableItem symbolTableItem = SymbolTable.root.get(SymbolTableActorItem.STARTKEY + actorDeclaration.getName().getName());
                symbolTableActorItem = (SymbolTableActorItem)symbolTableItem;
                actorSymbolTable = ((SymbolTableActorItem)symbolTableItem).getActorSymbolTable();
            } catch (ItemNotFoundException e) { }

            if(symbolTableActorItem == null)
                return new NoType();

            SymbolTable handlerSymbolTable = null;
            try {
                SymbolTableItem symbolTableItem = actorSymbolTable.get(SymbolTableHandlerItem.STARTKEY + handlerDeclaration.getName().getName());
                handlerSymbolTable = ((SymbolTableHandlerItem)symbolTableItem).getHandlerSymbolTable();
            } catch (ItemNotFoundException e) {}

            Type type = null;
            try {
                SymbolTableItem symbolTableItem = handlerSymbolTable.get(SymbolTableVariableItem.STARTKEY + identifier.getName());
                type = ((SymbolTableVariableItem)symbolTableItem).getType();
            } catch (ItemNotFoundException e) {
                addError(new VariableNotDeclared(identifier.getLine(), identifier.getName()));
                return new NoType();
            }
            return type;
        }
    }

    @Override
    public Type visit(Self self) {
        if(actorDeclaration == null) {
            addError(new SelfNotInMain(currentActorVarAccess.getLine()));
            return new NoType();
        }
        Type selfType = new ActorType(actorDeclaration.getName());
        ((ActorType) selfType).setActorDeclaration(actorDeclaration);
        return selfType;
    }

    @Override
    public Type visit(Sender sender) {
        if(isInInitial) {
            addError(new SenderInInitial(sender.getLine()));
        }
        if(actorDeclaration == null) {
            addError(new SenderNotInMain(sender.getLine()));
            return new NoType();
        }
        return new SenderType();
    }

    @Override
    public Type visit(BooleanValue value) {
        return new BooleanType();
    }

    @Override
    public Type visit(IntValue value) {
        return new IntType();
    }

    @Override
    public Type visit(StringValue value) {
        return new StringType();
    }
}
