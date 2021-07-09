package main.visitor.typeChecker;

import main.ast.node.Main;
import main.ast.node.Program;
import main.ast.node.declaration.ActorDeclaration;
import main.ast.node.declaration.ActorInstantiation;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.declaration.handler.InitHandlerDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.operators.BinaryOperator;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.node.statement.*;
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
import main.visitor.Visitor;
import main.visitor.VisitorImpl;

import java.util.ArrayList;

public class TypeChecker implements Visitor {
    private boolean isInInitial;
    private boolean isInFor;
    private ActorDeclaration currentActorDeclaration;
    private ExpressionTypeChecker expressionTypeChecker;
    public static ArrayList<CompileErrorException> errors;
    public static int numOfErrors;

    public TypeChecker() {
        this.expressionTypeChecker = new ExpressionTypeChecker();
        errors = new ArrayList<>();
        numOfErrors = 0;
        this.isInInitial = false;
        this.isInFor = false;
        expressionTypeChecker.setInInitial(false);
    }

    private void addError(CompileErrorException compileErrorException) {
        errors.add(compileErrorException);
        numOfErrors++;
    }

    private void checkActorNotDeclared(int lineNumber, String name, int t, String instanceName) {
        try {
            SymbolTableActorItem symbolTableActorItem = (SymbolTableActorItem) (SymbolTable.root.get(SymbolTableActorItem.STARTKEY + name));
        } catch (ItemNotFoundException e) {
            addError(new ActorNotDeclared(lineNumber, name));
            if(t == 1) {
                try {
                    ((SymbolTableVariableItem)(((SymbolTableActorItem)(SymbolTable.root.get(SymbolTableActorItem.STARTKEY + currentActorDeclaration.getName().getName()))).getActorSymbolTable().get(SymbolTableVariableItem.STARTKEY + instanceName))).setType(new NoType());
                } catch (ItemNotFoundException e1) {}
            }
        }
    }

    private boolean checkArgs(ArrayList<VarDeclaration> first, ArrayList<Expression> second) {
        boolean check = true;
        for (int i = 0; i < second.size(); i++) {
            Type secondType = expressionTypeChecker.visitExpr(second.get(i));
            if(!(((first.get(i).getType() instanceof IntType) && (secondType instanceof IntType)) ||
                ((first.get(i).getType() instanceof BooleanType) && (secondType instanceof BooleanType)) ||
                ((first.get(i).getType() instanceof StringType) && (secondType instanceof StringType)) ||
                ((first.get(i).getType() instanceof ArrayType) && (secondType instanceof ArrayType) && (((ArrayType)(first.get(i).getType())).getSize() == ((ArrayType)secondType).getSize())) ||
                ((first.get(i).getType() instanceof ActorType) && (secondType instanceof SenderType)) ||
                ((first.get(i).getType() instanceof SenderType) && (secondType instanceof ActorType)) ||
                ((first.get(i).getType() instanceof SenderType) && (secondType instanceof SenderType)) ||
                ((first.get(i).getType() instanceof ActorType) && (secondType instanceof ActorType) && expressionTypeChecker.isSubType(((ActorType)(first.get(i).getType())).getName().getName(), ((ActorType)(secondType)).getName().getName())) ||
                (secondType instanceof NoType))) {
                check = false;
            }
        }
        return check;
    }

    private void visitStatement( Statement stat )
    {
        if( stat == null )
            return;
        else if( stat instanceof MsgHandlerCall )
            this.visit( ( MsgHandlerCall ) stat );
        else if( stat instanceof Block )
            this.visit( ( Block ) stat );
        else if( stat instanceof Conditional )
            this.visit( ( Conditional ) stat );
        else if( stat instanceof For )
            this.visit( ( For ) stat );
        else if( stat instanceof Break )
            this.visit( ( Break ) stat );
        else if( stat instanceof Continue )
            this.visit( ( Continue ) stat );
        else if( stat instanceof Print )
            this.visit( ( Print ) stat );
        else if( stat instanceof Assign )
            this.visit( ( Assign ) stat );
    }

    @Override
    public void visit(Program program) {
        expressionTypeChecker.setProgram(program);
        if(program.getActors() != null) {
            for (ActorDeclaration actorDeclaration : program.getActors()) {
                actorDeclaration.accept(this);
            }
        }

        expressionTypeChecker.setMsgHandlerDeclaration(null);
        expressionTypeChecker.setActorDeclaration(null);
        currentActorDeclaration = null;
        program.getMain().accept(this);
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        expressionTypeChecker.setActorDeclaration(actorDeclaration);
        currentActorDeclaration = actorDeclaration;

        if(actorDeclaration.getParentName() != null) {
            checkActorNotDeclared(actorDeclaration.getLine(), actorDeclaration.getParentName().getName(), 0, "");
        }

        if(actorDeclaration.getKnownActors() != null) {
            for (VarDeclaration varDeclaration : actorDeclaration.getKnownActors()) {
                varDeclaration.accept(this);
                checkActorNotDeclared(varDeclaration.getLine(),((ActorType)(varDeclaration.getType())).getName().getName(), 1, varDeclaration.getIdentifier().getName());
            }
        }

        if(actorDeclaration.getInitHandler() != null) {
            isInInitial = true;
            expressionTypeChecker.setInInitial(true);
            actorDeclaration.getInitHandler().accept(this);
            isInInitial = false;
            expressionTypeChecker.setInInitial(false);
        }

        if(actorDeclaration.getMsgHandlers() != null) {
            for (HandlerDeclaration msgHandlerDeclaration : actorDeclaration.getMsgHandlers()) {
                msgHandlerDeclaration.accept(this);
            }
        }
    }


    @Override
    public void visit(HandlerDeclaration handlerDeclaration) {
        expressionTypeChecker.setMsgHandlerDeclaration(handlerDeclaration);

        if(handlerDeclaration.getBody() != null) {
            for (Statement statement : handlerDeclaration.getBody()) {
                visitStatement(statement);
            }
        }
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        if (varDeclaration.getType() instanceof ActorType) {
            try {
                SymbolTableActorItem symbolTableActorItem = (SymbolTableActorItem) (SymbolTable.root.get(SymbolTableActorItem.STARTKEY + ((ActorType) (varDeclaration.getType())).getName().getName()));
                ((ActorType) (varDeclaration.getType())).setActorDeclaration(symbolTableActorItem.getActorDeclaration());
            } catch (ItemNotFoundException e) {
                ((ActorType) (varDeclaration.getType())).setActorDeclaration(null);
            }
        }
    }

    @Override
    public void visit(Main mainActors) {
        if(mainActors.getMainActors() != null) {
            for (ActorInstantiation actorInstantiation : mainActors.getMainActors()) {
                actorInstantiation.accept(this);
            }
        }
    }

    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        try {
            SymbolTableActorItem symbolTableActorItem = (SymbolTableActorItem) (SymbolTable.root.get(SymbolTableActorItem.STARTKEY + ((ActorType) (actorInstantiation.getType())).getName().getName()));
            ((ActorType) (actorInstantiation.getType())).setActorDeclaration(symbolTableActorItem.getActorDeclaration());
        } catch (ItemNotFoundException e) {
            ((ActorType) (actorInstantiation.getType())).setActorDeclaration(null);
        }

        if(((ActorType) (actorInstantiation.getType())).getActorDeclaration() == null) {
            addError(new ActorNotDeclared(actorInstantiation.getLine(), ((ActorType) (actorInstantiation.getType())).getName().getName()));
            try {
                ((SymbolTableVariableItem)(((SymbolTableMainItem)SymbolTable.root.get(SymbolTableMainItem.STARTKEY + "main")).getMainSymbolTable().get(SymbolTableVariableItem.STARTKEY + actorInstantiation.getIdentifier().getName()))).setType(new NoType());
            } catch(ItemNotFoundException e) {}
            if(actorInstantiation.getKnownActors() != null) {
                for (int i = 0; i < actorInstantiation.getKnownActors().size(); i++) {
                    expressionTypeChecker.visitExpr(actorInstantiation.getKnownActors().get(i));
                }
            }
            if(actorInstantiation.getInitArgs() != null) {
                for(Expression expression : actorInstantiation.getInitArgs()) {
                    expressionTypeChecker.visitExpr(expression);
                }
            }
            return;
        }

        ActorDeclaration instanceActorDeclaration = null;
        try {
            SymbolTableItem symbolTableItem = SymbolTable.root.get(SymbolTableActorItem.STARTKEY + ((ActorType) (actorInstantiation.getType())).getName().getName());
            instanceActorDeclaration = ((SymbolTableActorItem)symbolTableItem).getActorDeclaration();
        } catch (ItemNotFoundException e) {}

        if(instanceActorDeclaration == null) return;

        int numberOfInstanceActorKnownActors = 0, numberOfInstanceKnownActors = 0;
        if(instanceActorDeclaration.getKnownActors() != null)
            numberOfInstanceActorKnownActors = instanceActorDeclaration.getKnownActors().size();
        if(actorInstantiation.getKnownActors() != null)
            numberOfInstanceKnownActors = actorInstantiation.getKnownActors().size();

        if(numberOfInstanceActorKnownActors != numberOfInstanceKnownActors) {
            if(actorInstantiation.getKnownActors() != null) {
                for (int i = 0; i < numberOfInstanceKnownActors; i++) {
                    expressionTypeChecker.visitExpr(actorInstantiation.getKnownActors().get(i));
                }
            }
            addError(new KnownActorsNotMatchDefinition(actorInstantiation.getLine()));
        }
        else if(numberOfInstanceKnownActors != 0) {
            boolean check = true;
            for(int i = 0; i < numberOfInstanceKnownActors; i++) {
                Type knownActorType = expressionTypeChecker.visitExpr(actorInstantiation.getKnownActors().get(i));
                if(knownActorType instanceof NoType)
                    check = false;
                if(((ActorType)instanceActorDeclaration.getKnownActors().get(i).getType()).getActorDeclaration() == null) {
                    check = false;
                }
            }
            if(check) {
                check = false;
                for(int i = 0; i < numberOfInstanceKnownActors; i++) {
                    Type knownActorType = expressionTypeChecker.visitExpr(actorInstantiation.getKnownActors().get(i));
                    if (expressionTypeChecker.isSubType(((ActorType)instanceActorDeclaration.getKnownActors().get(i).getType()).getName().getName(), ((ActorType)knownActorType).getName().getName()))
                        continue;
                    check = true;
                    break;
                }
                if (check)
                    addError(new KnownActorsNotMatchDefinition(actorInstantiation.getLine()));
            }
        }

        if(actorInstantiation.getInitArgs() != null) {
            SymbolTableActorItem symbolTableActorItem = null;
            try {
                SymbolTableItem symbolTableItem = SymbolTable.root.get(SymbolTableActorItem.STARTKEY + ((ActorType) (actorInstantiation.getType())).getName().getName());
                symbolTableActorItem = (SymbolTableActorItem)symbolTableItem;
            } catch (ItemNotFoundException e) {}

            if(symbolTableActorItem == null) return;

            InitHandlerDeclaration initHandlerDeclaration = symbolTableActorItem.getActorDeclaration().getInitHandler();
            ArrayList<VarDeclaration> initDecArgs = null;

            int numberOfInitDecArgs = 0, numberOfInitArgs = 0;

            if((initHandlerDeclaration != null) && (initHandlerDeclaration.getArgs() != null)) {
                initDecArgs = initHandlerDeclaration.getArgs();
                numberOfInitDecArgs = initDecArgs.size();
            }

            numberOfInitArgs = actorInstantiation.getInitArgs().size();

            if(numberOfInitDecArgs != numberOfInitArgs) {
                for(Expression expression : actorInstantiation.getInitArgs()) {
                    expressionTypeChecker.visitExpr(expression);
                }
                addError(new InitialArgsNotMatchDeclaration(actorInstantiation.getLine(), actorInstantiation.getIdentifier().getName()));
            }
            else if(numberOfInitArgs != 0) {
                if(!checkArgs(initDecArgs, actorInstantiation.getInitArgs())) {
                    addError(new InitialArgsNotMatchDeclaration(actorInstantiation.getLine(), actorInstantiation.getIdentifier().getName()));
                }
            }

        }

    }


    @Override
    public void visit(UnaryExpression unaryExpression) { }

    @Override
    public void visit(BinaryExpression binaryExpression) { }

    @Override
    public void visit(ArrayCall arrayCall) { }

    @Override
    public void visit(ActorVarAccess actorVarAccess) { }

    @Override
    public void visit(Identifier identifier) { }

    @Override
    public void visit(Self self) { }

    @Override
    public void visit(Sender sender) { }

    @Override
    public void visit(BooleanValue value) { }

    @Override
    public void visit(IntValue value) { }

    @Override
    public void visit(StringValue value) { }

    @Override
    public void visit(Block block) {
        if(block.getStatements() != null) {
            for (Statement statement : block.getStatements()) {
                visitStatement(statement);
            }
        }
    }

    @Override
    public void visit(Conditional conditional) {
        Type conditionType = expressionTypeChecker.visitExpr(conditional.getExpression());
        if(!(conditionType instanceof BooleanType) && !(conditionType instanceof NoType)) {
            addError(new ConditionNotBoolean(conditional.getLine()));
        }

        if(conditional.getThenBody() != null) {
            conditional.getThenBody().accept(this);
        }

        if(conditional.getElseBody() != null) {
            conditional.getElseBody().accept(this);
        }
    }

    @Override
    public void visit(For loop) {

        if(loop.getInitialize() != null) {
            loop.getInitialize().accept(this);
        }

        if(loop.getCondition() != null) {
            Type conditionType = expressionTypeChecker.visitExpr(loop.getCondition());
            if(!(conditionType instanceof BooleanType) && !(conditionType instanceof NoType)) {
                addError(new ConditionNotBoolean(loop.getLine()));
            }
        }

        if(loop.getUpdate() != null) {
            loop.getUpdate().accept(this);
        }

        if(loop.getBody() != null) {
            boolean lastIsInFor = isInFor;
            isInFor = true;
            loop.getBody().accept(this);
            isInFor = lastIsInFor;
        }
    }

    @Override
    public void visit(Break breakLoop) {
        if(!isInFor) {
            addError(new BreakContinueNotInLoop(breakLoop.getLine(), 0));
        }
    }

    @Override
    public void visit(Continue continueLoop) {
        if(!isInFor) {
            addError(new BreakContinueNotInLoop(continueLoop.getLine(), 1));
        }
    }

    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        Type instanceType = expressionTypeChecker.visitExpr(msgHandlerCall.getInstance());
        if(!((instanceType instanceof NoType) || (instanceType instanceof SenderType) || (instanceType instanceof ActorType))) {
            addError(new VariableNotCallable(msgHandlerCall.getLine(), ((Identifier) (msgHandlerCall.getInstance())).getName()));
            if(msgHandlerCall.getArgs() != null) {
                for(Expression expression : msgHandlerCall.getArgs()) {
                    expressionTypeChecker.visitExpr(expression);
                }
            }
            return;
        }
        if((instanceType instanceof NoType) || (instanceType instanceof SenderType)) {
            if(msgHandlerCall.getArgs() != null) {
                for(Expression expression : msgHandlerCall.getArgs()) {
                    expressionTypeChecker.visitExpr(expression);
                }
            }
            return;
        }

        String msgHandlerName = msgHandlerCall.getMsgHandlerName().getName();
        String actorName = ((ActorType)instanceType).getName().getName();

        SymbolTableActorItem symbolTableActorItem = null;
        try {
            SymbolTableItem symbolTableItem = SymbolTable.root.get(SymbolTableActorItem.STARTKEY + actorName);
            symbolTableActorItem = (SymbolTableActorItem)symbolTableItem;
        } catch (ItemNotFoundException e) {
            return;
        }

        SymbolTableHandlerItem symbolTableHandlerItem = null;
        try {
            SymbolTableItem symbolTableItem = symbolTableActorItem.getActorSymbolTable().get(SymbolTableHandlerItem.STARTKEY + msgHandlerName);
            symbolTableHandlerItem = (SymbolTableHandlerItem)symbolTableItem;
        } catch (ItemNotFoundException e) {
            addError(new MsgHandlerNotAvailableInActor(msgHandlerCall.getLine(), msgHandlerName, actorName));
            if(msgHandlerCall.getArgs() != null) {
                for(Expression expression : msgHandlerCall.getArgs()) {
                    expressionTypeChecker.visitExpr(expression);
                }
            }
            return;
        }

        if(msgHandlerCall.getArgs() != null) {
            int numberOfDecArgs = 0, numberOfArgs = 0;
            numberOfArgs = msgHandlerCall.getArgs().size();
            if((symbolTableHandlerItem.getHandlerDeclaration() != null) || (symbolTableHandlerItem.getHandlerDeclaration().getArgs() != null)) {
                numberOfDecArgs = symbolTableHandlerItem.getHandlerDeclaration().getArgs().size();
            }

            if(numberOfDecArgs != numberOfArgs) {
                for(Expression expression : msgHandlerCall.getArgs()) {
                    expressionTypeChecker.visitExpr(expression);
                }
                addError(new MsgHandlerArgsNotMatchDeclaration(msgHandlerCall.getLine(), msgHandlerName));
            }
            else if(numberOfArgs != 0) {
                if (!checkArgs(symbolTableHandlerItem.getHandlerDeclaration().getArgs(), msgHandlerCall.getArgs())) {
                    addError(new MsgHandlerArgsNotMatchDeclaration(msgHandlerCall.getLine(), msgHandlerName));
                }
            }
        }
    }

    @Override
    public void visit(Print print) {
        Type printExpressionType = expressionTypeChecker.visitExpr(print.getArg());
        if(!((printExpressionType instanceof IntType) || (printExpressionType instanceof StringType) ||
        (printExpressionType instanceof BooleanType) || (printExpressionType instanceof ArrayType) ||
        (printExpressionType instanceof NoType))) {
            addError(new UnsupportedPrintType(print.getLine()));
        }
    }

    @Override
    public void visit(Assign assign) {
        Type leftType = expressionTypeChecker.visitExpr(assign.getlValue());
        Type rightType = expressionTypeChecker.visitExpr(assign.getrValue());
        BinaryOperator operator = BinaryOperator.assign;

        if (!(((leftType instanceof IntType) && (rightType instanceof IntType)) ||
                ((leftType instanceof StringType) && (rightType instanceof StringType)) ||
                ((leftType instanceof BooleanType) && (rightType instanceof BooleanType)) ||
                ((leftType instanceof ArrayType) && (rightType instanceof ArrayType)) ||
                ((leftType instanceof ActorType) && (rightType instanceof SenderType)) ||
                ((leftType instanceof SenderType) && (rightType instanceof ActorType)) ||
                ((leftType instanceof SenderType) && (rightType instanceof SenderType)) ||
                ((leftType instanceof ActorType) && (rightType instanceof ActorType) && expressionTypeChecker.isSubType(((ActorType)(leftType)).getName().getName(), ((ActorType)(rightType)).getName().getName())) ||
                ((leftType instanceof NoType) || (rightType instanceof NoType)))) {

            addError(new UnsupportedOperandType(assign.getLine(), operator.name()));
        }

        if ((leftType instanceof ArrayType) && (rightType instanceof ArrayType) && (((ArrayType)leftType).getSize() != ((ArrayType)rightType).getSize())) {
            addError(new NotEqualArraySizes(assign.getLine()));
        }

        if(!expressionTypeChecker.isLvalue(assign.getlValue())) {
            addError(new LeftSideNotLvalue(assign.getLine()));
        }

    }
}

