package main.visitor.codeGeneration;

import com.sun.org.apache.bcel.internal.classfile.Code;
import main.ast.node.*;
import main.ast.node.Program;
import main.ast.node.declaration.*;
import main.ast.node.declaration.handler.*;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.operators.BinaryOperator;
import main.ast.node.expression.operators.UnaryOperator;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.node.statement.*;
import main.ast.type.Type;
import main.ast.type.actorType.ActorType;
import main.ast.type.arrayType.ArrayType;
import main.ast.type.primitiveType.BooleanType;
import main.ast.type.primitiveType.IntType;
import main.ast.type.primitiveType.StringType;
import main.ast.type.senderType.SenderType;
import main.symbolTable.SymbolTable;
import main.symbolTable.SymbolTableActorItem;
import main.symbolTable.SymbolTableHandlerItem;
import main.symbolTable.SymbolTableItem;
import main.symbolTable.itemException.ItemNotFoundException;
import main.symbolTable.symbolTableVariableItem.SymbolTableVariableItem;
import main.visitor.Visitor;
import main.visitor.typeChecker.ExpressionTypeChecker;

import javax.imageio.IIOException;
import java.io.*;
import java.util.ArrayList;
import java.lang.Object;

import static javafx.scene.input.KeyCode.V;


public class CodeGenerator implements GeneratorVisitor {
    private String currentStartForLoopLabel;
    private String currentExitForLoopLabel;
    private int labelCounter;
    private ExpressionTypeChecker expressionTypeChecker;
    private ActorDeclaration currentActorDeclaration;
    private HandlerDeclaration currentHandlerDeclaration;
    private Main currentMain;
    private FileWriter currentFile;
    private FileWriter actorFile;
    private FileWriter defaultActorFile;
    private ArrayList<String> seendMsgHandlers;
    private String outputPath;
    private boolean isActorVar;

    protected void visitStatement( Statement stat )
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

    protected String visitExpr( Expression expr )
    {
        if( expr == null )
            return "";
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
        return "";
    }

    private String getFreshLabel() {
        labelCounter++;
        return "Label_" + labelCounter;
    }

    private void copyFile(String toBeCopied, String toBePasted) {
        try {
            File readingFile = new File(toBeCopied);
            File writingFile = new File(toBePasted);
            InputStream readingFileStream = new FileInputStream(readingFile);
            OutputStream writingFileStream = new FileOutputStream(writingFile);
            byte[] buffer = new byte[1024];
            int readLength;
            while ((readLength = readingFileStream.read(buffer)) > 0){
                writingFileStream.write(buffer, 0, readLength);
            }
            readingFileStream.close();
            writingFileStream.close();
        } catch (IOException e) { }
    }

    private void prepareOutputFolder() {
        outputPath = "output/";
        String jasminPath = "utilities/jarFiles/jasmin.jar";
        String ActorPath = "utilities/Actor.j";
        String MessagePath = "utilities/Message.j";

        try{
            File directory = new File(outputPath);
            File[] files = directory.listFiles();
            if(files != null)
                for (File file : files)
                    file.delete();
            directory.mkdir();
        }
        catch(SecurityException e) { }

        copyFile(jasminPath, outputPath + "jasmin.jar");
        copyFile(ActorPath, outputPath + "Actor.j");
        copyFile(MessagePath, outputPath + "Message.j");
        currentFile = createFile("DefaultActor");
        defaultActorFile = currentFile;
        makeDefaultActorReady();
    }

    private void makeDefaultActorReady() {
        addCommand(".class public DefaultActor\n" +
                ".super java/lang/Thread\n\n" +
                ".method public <init>()V\n" +
                ".limit stack 32\n" +
                ".limit locals 32\n" +
                "aload 0\n" +
                "invokespecial java/lang/Thread/<init>()V\n" +
                "return\n" +
                ".end method\n");
    }

    private String getArgsSignatures(ArrayList<VarDeclaration> varDeclarations) {
        String types = "";
        for (VarDeclaration varDeclaration : varDeclarations) {
            Type t = varDeclaration.getType();
            types += makeTypeSignature(t);
        }
        return types;
    }

    private void addMethodToDefaultActor(MsgHandlerDeclaration msgHandlerDeclaration) {
        String argumentsTypes = "";
        int stackSize = 2;
        if(msgHandlerDeclaration.getArgs() != null) {
            stackSize += msgHandlerDeclaration.getArgs().size();
            argumentsTypes = getArgsSignatures(msgHandlerDeclaration.getArgs());
        }
        currentFile = defaultActorFile;
        addCommand(".method public send_" + msgHandlerDeclaration.getName().getName() + "(LActor;" + argumentsTypes + ")V\n" +
                ".limit stack 32\n" +
                ".limit locals 32\n" +
                "getstatic java/lang/System/out Ljava/io/PrintStream;\n" +
                "ldc \"there is no msghandler named " + msgHandlerDeclaration.getName().getName() + " in sender\"\n" +
                "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n" +
                "return\n" +
                ".end method\n");
        currentFile = actorFile;
    }

    private String makeTypeSignature(Type t) {
        if(t instanceof IntType){
            return "I";
        }
        if(t instanceof BooleanType){
            return "Z";
        }
        if(t instanceof StringType){
            return "Ljava/lang/String;";
        }
        if(t instanceof ArrayType){
            return "[I";
        }
        if(t instanceof ActorType){
            return "L" + ((ActorType)t).getActorDeclaration().getName().getName() +";";
        }
        return "";
    }

    private FileWriter createFile(String name) {
        try {
            String path = outputPath + name + ".j";
            File file = new File(path);
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(path);
            if(currentFile != null)
                currentFile.flush();
            currentFile = fileWriter;
            return currentFile;
        } catch (IOException e) {}
        return null;
    }

    private void addCommand(String command) {
        try {
            currentFile.write(command + "\n");
            currentFile.flush();
        } catch (IOException e) {}
    }

    private int slotOfMain(Main main, Identifier identifier) {
        int count = 1;
        if(main.getMainActors() != null) {
            for (ActorInstantiation actorInstantiation : main.getMainActors()) {
                if(actorInstantiation.getIdentifier().getName().equals(identifier.getName()))
                    return count;
                count++;
            }
        }
        return -1;
    }

    private int slotOf(HandlerDeclaration handlerDeclaration, Identifier identifier) {
        int count = 1;
        if(handlerDeclaration instanceof MsgHandlerDeclaration)
            count = 2;

        if(handlerDeclaration.getArgs() != null) {
            for (VarDeclaration varDeclaration : handlerDeclaration.getArgs()) {
                if (varDeclaration.getIdentifier().getName().equals(identifier.getName()))
                    return count;
                count++;
            }
        }
        if(handlerDeclaration.getLocalVars() != null) {
            for (VarDeclaration varDeclaration : handlerDeclaration.getLocalVars()) {
                if (varDeclaration.getIdentifier().getName().equals(identifier.getName()))
                    return count;
                count++;
            }
        }
        return -1;
    }

    private boolean isField(Identifier identifier) {
        SymbolTable handlerSymbolTable = null;
        try {
            SymbolTableItem symbolTableItem = SymbolTable.root.get(SymbolTableActorItem.STARTKEY + currentActorDeclaration.getName().getName());
            SymbolTableActorItem symbolTableActorItem = (SymbolTableActorItem)symbolTableItem;
            SymbolTableItem symbolTableItem2 = symbolTableActorItem.getActorSymbolTable().get(SymbolTableHandlerItem.STARTKEY + currentHandlerDeclaration.getName().getName());
            handlerSymbolTable = ((SymbolTableHandlerItem)symbolTableItem2).getHandlerSymbolTable();
        } catch (ItemNotFoundException e) {}

        try {
            handlerSymbolTable.getInCurrentScope(SymbolTableVariableItem.STARTKEY + identifier.getName());
            return false;
        } catch (ItemNotFoundException e) {
            return true;
        }
    }

    private void createFileForMsgHandler() {
        String fileName = currentActorDeclaration.getName().getName() + "_" + currentHandlerDeclaration.getName().getName();
        currentFile = createFile(fileName);
        addCommand(".class public " + fileName + "\n" +
                ".super Message\n");

        String argumentsSignatures = "";
        String argsCommands = "";
        String argsCommandsForExecute = "";
        int argsNum = 3;
        int count = 2;
        if(currentHandlerDeclaration.getArgs() != null) {
            for (VarDeclaration varDeclaration : currentHandlerDeclaration.getArgs()) {
                count++;
                String varName = varDeclaration.getIdentifier().getName();
                String signatue = makeTypeSignature(varDeclaration.getType());
                String intOrObj = ((varDeclaration.getType() instanceof  IntType) || (varDeclaration.getType() instanceof BooleanType)) ? "i" : "a";
                addCommand(".field private " + varName + " " + signatue);
                argsCommands += "aload 0\n" +
                        intOrObj + "load " + count + "\n" +
                        "putfield " + fileName + "/" + varName + " " + signatue + "\n";
                argsCommandsForExecute += "aload 0\n" +
                        "getfield " + fileName + "/" + varName + " " + signatue + "\n";
            }
            addCommand(".field private receiver L" + currentActorDeclaration.getName().getName() + ";\n" +
                    ".field private sender LActor;\n");
            argumentsSignatures = getArgsSignatures(currentHandlerDeclaration.getArgs());
            argsNum += currentHandlerDeclaration.getArgs().size();

        }
        addCommand(".method public <init>(L" + currentActorDeclaration.getName().getName() + ";LActor;" + argumentsSignatures + ")V\n" +
                ".limit stack 32\n" +
                ".limit locals 32\n" +
                "aload 0\n" +
                "invokespecial Message/<init>()V\n");
        if(currentHandlerDeclaration.getArgs() != null) {
            for(VarDeclaration varDeclaration : currentHandlerDeclaration.getArgs()) {
                if((varDeclaration.getType() instanceof IntType) || (varDeclaration.getType() instanceof BooleanType)) {
                    addCommand("aload 0");
                    addCommand("ldc 0");
                    addCommand("putfield " + fileName + "/" + varDeclaration.getIdentifier().getName()
                            + " " + makeTypeSignature(varDeclaration.getType()));
                }
                else if(varDeclaration.getType() instanceof ArrayType) {
                    int size = ((ArrayType)varDeclaration.getType()).getSize();
                    addCommand("aload 0");
                    addCommand("ldc " + size);
                    addCommand("newarray int");
                    addCommand("putfield " + fileName + "/" + varDeclaration.getIdentifier().getName() + " [I");
                }
                else if(varDeclaration.getType() instanceof StringType) {
                    addCommand("aload 0");
                    addCommand("ldc \"\"");
                    addCommand("putfield " + fileName + "/" + varDeclaration.getIdentifier().getName()
                            + " " + makeTypeSignature(varDeclaration.getType()));
                }
            }
        }
        addCommand("aload 0\n" +
                "aload 1\n" +
                "putfield " + fileName + "/receiver L" + currentActorDeclaration.getName().getName() + ";\n" +
                "aload 0\n" +
                "aload 2\n" +
                "putfield " + fileName + "/sender LActor;\n" +
                argsCommands +
                "return\n" +
                ".end method\n");

        addCommand(".method public execute()V\n" +
                ".limit stack 32\n" +
                ".limit locals 32\n" +
                "aload 0\n" +
                "getfield " + fileName + "/receiver L" + currentActorDeclaration.getName().getName() + ";\n" +
                "aload 0\n" +
                "getfield " + fileName + "/sender LActor;\n" +
                argsCommandsForExecute +
                "invokevirtual " + currentActorDeclaration.getName().getName() + "/" + currentHandlerDeclaration.getName().getName() +  "(LActor;" + argumentsSignatures + ")V\n" +
                "return\n" +
                ".end method");
        currentFile = actorFile;
    }

    private boolean isMsgHandlerSeen(String name) {
        for(String s : this.seendMsgHandlers) {
            if(s.equals(name))
                return true;
        }
        this.seendMsgHandlers.add(name);
        return false;
    }

    public CodeGenerator(){
        expressionTypeChecker = new ExpressionTypeChecker();
        expressionTypeChecker.setInInitial(false);
        currentStartForLoopLabel = null;
        currentExitForLoopLabel = null;
        currentMain = null;
        currentFile = null;
        labelCounter = 0;
        isActorVar = false;
        seendMsgHandlers = new ArrayList<>();
        prepareOutputFolder();
    }

    @Override
    public void visit(Program program) {
        expressionTypeChecker.setProgram(program);

        if(program.getActors() != null) {
            for (ActorDeclaration actorDeclaration : program.getActors()) {
                this.visit(actorDeclaration);
            }
        }

        expressionTypeChecker.setMsgHandlerDeclaration(null);
        expressionTypeChecker.setActorDeclaration(null);
        currentActorDeclaration = null;
        currentHandlerDeclaration = null;
        currentMain = program.getMain();
        this.visit(program.getMain());
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        expressionTypeChecker.setActorDeclaration(actorDeclaration);
        currentActorDeclaration = actorDeclaration;
        actorFile = createFile(actorDeclaration.getName().getName());

        addCommand(".class public " + actorDeclaration.getName().getName() + "\n" +
                ".super Actor\n");

        String setKnownActorsCommand = "";
        if(actorDeclaration.getKnownActors() != null) {
            setKnownActorsCommand =
                    ".method public setKnownActors(" + getArgsSignatures(actorDeclaration.getKnownActors()) + ")V\n" +
                    ".limit stack 32\n" +
                    ".limit locals 32\n";
            int count = 0;
            for (VarDeclaration varDeclaration : actorDeclaration.getKnownActors()) {
                count++;
                addCommand(".field " + varDeclaration.getIdentifier().getName() + " " + makeTypeSignature(varDeclaration.getType()));
                setKnownActorsCommand += "aload 0\n" +
                                        "aload " + count + "\n" +
                                        "putfield " + currentActorDeclaration.getName().getName() + "/" + varDeclaration.getIdentifier().getName() + " " + makeTypeSignature(varDeclaration.getType()) + "\n";
            }
            setKnownActorsCommand += "\nreturn\n" +
                                    ".end method";

        }

        if(actorDeclaration.getActorVars() != null) {
            for (VarDeclaration varDeclaration : actorDeclaration.getActorVars()) {
                addCommand(".field " + varDeclaration.getIdentifier().getName() + " " + makeTypeSignature(varDeclaration.getType()));
            }
            addCommand("");
            addCommand(".method public <init>(I)V\n" +
                    ".limit stack 32\n" +
                    ".limit locals 32");
            isActorVar = true;
            for (VarDeclaration varDeclaration : actorDeclaration.getActorVars()) {
                this.visit(varDeclaration);
            }
            isActorVar = false;
            addCommand("aload 0\n" +
                    "iload 1\n" +
                    "invokespecial Actor/<init>(I)V\n" +
                    "return\n" +
                    ".end method\n");
            addCommand(setKnownActorsCommand);
            if(!setKnownActorsCommand.equals(""))
                addCommand("");
        }

        if(actorDeclaration.getInitHandler() != null) {
            this.visit(actorDeclaration.getInitHandler());
        }
        addCommand("");

        if(actorDeclaration.getMsgHandlers() != null) {
            for (HandlerDeclaration msgHandlerDeclaration : actorDeclaration.getMsgHandlers()) {
                this.visit(msgHandlerDeclaration);
            }
        }
    }

    @Override
    public void visit(HandlerDeclaration handlerDeclaration) {
        expressionTypeChecker.setMsgHandlerDeclaration(handlerDeclaration);
        currentHandlerDeclaration = handlerDeclaration;
        if(handlerDeclaration instanceof InitHandlerDeclaration) {
            String argsSignature1 = "";
            if(handlerDeclaration.getArgs() != null) {
                for(VarDeclaration varDeclaration : handlerDeclaration.getArgs())
                    argsSignature1 += makeTypeSignature(varDeclaration.getType());
            }
            addCommand(".method public initial(" + argsSignature1 + ")V\n" +
                    ".limit stack 32\n" +
                    ".limit locals 32");
            int argsSize = 0;
            if(handlerDeclaration.getArgs() != null) {
                argsSize = handlerDeclaration.getArgs().size();
            }
            if(handlerDeclaration.getLocalVars() != null) {
                argsSize += handlerDeclaration.getLocalVars().size();
                for(VarDeclaration varDeclaration : handlerDeclaration.getLocalVars()) {
                    this.visit(varDeclaration);
                }
            }
            if(handlerDeclaration.getBody() != null) {
                for(Statement statement : handlerDeclaration.getBody()) {
                    this.visitStatement(statement);
                }
            }
            addCommand("return\n" +
                    ".end method\n");
            return;
        }
        String sigs = "";
        if(handlerDeclaration.getArgs() != null) {
            for(VarDeclaration varDeclaration : handlerDeclaration.getArgs())
                sigs += makeTypeSignature(varDeclaration.getType());
        }
        if(!isMsgHandlerSeen(handlerDeclaration.getName().getName() + sigs))
            addMethodToDefaultActor((MsgHandlerDeclaration) handlerDeclaration);
        createFileForMsgHandler();

        String argsSignature = "";
        if(handlerDeclaration.getArgs() != null) {
            for(VarDeclaration varDeclaration : handlerDeclaration.getArgs())
                argsSignature += makeTypeSignature(varDeclaration.getType());
        }

        String handlerArgsSignature = "";
        int argsNumber = 2;
        if(handlerDeclaration.getArgs() != null) {
            argsNumber = handlerDeclaration.getArgs().size();
            for(VarDeclaration varDeclaration : handlerDeclaration.getArgs())
                handlerArgsSignature += makeTypeSignature(varDeclaration.getType());
        }

        addCommand(".method public " + handlerDeclaration.getName().getName() + "(LActor;" + handlerArgsSignature + ")V\n" +
                ".limit stack 32\n" +
                ".limit locals 32");
        if(handlerDeclaration.getLocalVars() != null) {
            for(VarDeclaration varDeclaration : handlerDeclaration.getLocalVars())
                this.visit(varDeclaration);
        }

        if(handlerDeclaration.getBody() != null) {
            for(Statement statement : handlerDeclaration.getBody()) {
                this.visitStatement(statement);
            }
        }
        addCommand("return\n" +
                ".end method\n");

        String fileName = currentActorDeclaration.getName().getName() + "_" + handlerDeclaration.getName().getName();
        addCommand(".method public send_" + handlerDeclaration.getName().getName() + "(LActor;" + handlerArgsSignature + ")V\n" +
                ".limit stack 32\n" +
                ".limit locals 32\n" +
                "aload 0\n" +
                "new " + fileName + "\n" +
                "dup\n" +
                "aload 0\n" +
                "aload 1");
        if(handlerDeclaration.getArgs() != null) {
            for(VarDeclaration varDeclaration : handlerDeclaration.getArgs()) {
                if((varDeclaration.getType() instanceof IntType) || (varDeclaration.getType() instanceof BooleanType))
                    addCommand("iload " + (slotOf(handlerDeclaration, varDeclaration.getIdentifier())));
                if((varDeclaration.getType() instanceof StringType) || (varDeclaration.getType() instanceof ArrayType))
                    addCommand("aload " + (slotOf(handlerDeclaration, varDeclaration.getIdentifier())));
            }
        }
        addCommand("invokespecial " + fileName + "/<init>(L" + currentActorDeclaration.getName().getName() + ";LActor;" + handlerArgsSignature + ")V\n" +
                "invokevirtual " + currentActorDeclaration.getName().getName() + "/send(LMessage;)V\n" +
                "return\n" +
                ".end method\n");

    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        if((varDeclaration.getType() instanceof IntType) || (varDeclaration.getType() instanceof BooleanType)) {
            if(isActorVar) {
                addCommand("aload 0");
                addCommand("ldc 0");
                addCommand("putfield " + currentActorDeclaration.getName().getName() + "/" +
                        varDeclaration.getIdentifier().getName() + " " + makeTypeSignature(varDeclaration.getType()));
            }
            else {
                addCommand("ldc 0");
                addCommand("istore " + slotOf(currentHandlerDeclaration, varDeclaration.getIdentifier()));
            }
        }
        else if(varDeclaration.getType() instanceof ArrayType) {
            int size = ((ArrayType)varDeclaration.getType()).getSize();
            if(isActorVar) {
                addCommand("aload 0");
                addCommand("ldc " + size);
                addCommand("newarray int");
                addCommand("putfield " + currentActorDeclaration.getName().getName() + "/" +
                        varDeclaration.getIdentifier().getName() + " [I");
            }
            else {
                addCommand("ldc " + size);
                addCommand("newarray int");
                addCommand("astore " + slotOf(currentHandlerDeclaration, varDeclaration.getIdentifier()));
            }
        }
        else if(varDeclaration.getType() instanceof StringType) {
            if(isActorVar) {
                addCommand("aload 0");
                addCommand("ldc \"\"");
                addCommand("putfield " + currentActorDeclaration.getName().getName() + "/" +
                        varDeclaration.getIdentifier().getName() + " " + makeTypeSignature(varDeclaration.getType()));
            }
            else {
                addCommand("ldc \"\"");
                addCommand("astore " + slotOf(currentHandlerDeclaration, varDeclaration.getIdentifier()));
            }
        }
    }

    @Override
    public void visit(Main main) {
        currentFile = createFile("Main");

        addCommand(".class public Main\n" +
                ".super java/lang/Object\n" +
                "\n" +
                ".method public <init>()V\n" +
                ".limit stack 32\n" +
                ".limit locals 32\n" +
                "0: aload 0\n" +
                "1: invokespecial java/lang/Object/<init>()V\n" +
                "4: return\n" +
                ".end method\n");

        addCommand(".method public static main([Ljava/lang/String;)V\n" +
                ".limit stack 32\n" +
                ".limit locals 32");

        String setKnownActorsCommands = "";
        String initialCommands = "";
        if(main.getMainActors() != null){
            int count = 0;
            for(ActorInstantiation actorInstantiation : main.getMainActors()) {
                count ++;
                String actorTypeName = ((ActorType)actorInstantiation.getType()).getName().getName();
                addCommand("new " + actorTypeName + "\n" +
                        "dup\n" +
                        "ldc " + ((ActorType)actorInstantiation.getType()).getActorDeclaration().getQueueSize() + "\n" +
                        "invokespecial " + actorTypeName + "/<init>(I)V\n" +
                        "astore " + count);

                setKnownActorsCommands += "aload " + count + "\n";
                String knownActorsSignatures = "";
                if(actorInstantiation.getKnownActors() != null) {
                    for(Identifier knownActor : actorInstantiation.getKnownActors()) {
                        setKnownActorsCommands += "aload " + slotOfMain(main, knownActor) + "\n";
                        Type t = expressionTypeChecker.visitExpr(knownActor);
                        knownActorsSignatures += makeTypeSignature((ActorType)t);
                    }
                }
                setKnownActorsCommands += "invokevirtual " + actorTypeName + "/setKnownActors(" + knownActorsSignatures + ")V";
                if(count != main.getMainActors().size())
                    setKnownActorsCommands += "\n";


                InitHandlerDeclaration initHandlerDeclaration = ((ActorType)actorInstantiation.getType()).getActorDeclaration().getInitHandler();
                if(initHandlerDeclaration != null) {
                    String signatures = "";
                    initialCommands += "aload " + count + "\n";
                    if(actorInstantiation.getInitArgs() != null) {
                        for (Expression expression : actorInstantiation.getInitArgs()) {
                            initialCommands += this.visitExpr(expression) + "\n";
                            signatures += makeTypeSignature(expressionTypeChecker.visitExpr(expression));
                        }
                    }
                    initialCommands += "invokevirtual " + actorTypeName + "/initial(" + signatures + ")V\n";
                }
            }
        }

        addCommand(setKnownActorsCommands);
        addCommand(initialCommands);

        if(main.getMainActors() != null) {
            int count = 0;
            for (ActorInstantiation actorInstantiation : main.getMainActors()) {
                count++;
                addCommand("aload " + count + "\n" +
                        "invokevirtual " + ((ActorType)actorInstantiation.getType()).getName().getName() + "/start()V");
            }
        }

        addCommand("return\n" +
                ".end method");
    }

    @Override
    public void visit(ActorInstantiation actorInstantiation) { }

    @Override
    public String visit(UnaryExpression unaryExpression) {
        UnaryOperator operator = unaryExpression.getUnaryOperator();
        String result = "";
        if(operator == UnaryOperator.minus) {
            result += this.visitExpr(unaryExpression.getOperand()) + "\n";
            result += "ineg";
        }
        else if(operator == UnaryOperator.not) {
            String falseLabel = getFreshLabel();
            String exitLabel = getFreshLabel();
            result += this.visitExpr(unaryExpression.getOperand()) + "\n";
            result += "ifne " + falseLabel + "\n";
            result += "ldc 1" + "\n";
            result += "goto " + exitLabel + "\n";
            result += falseLabel + ":\n";
            result += "ldc 0\n";
            result += exitLabel + ":";
        }
        else if(operator == UnaryOperator.predec) {
            if(unaryExpression.getOperand() instanceof Identifier) {
                if(isField((Identifier)unaryExpression.getOperand())) {
                    String actorVariableType = currentActorDeclaration.getName().getName() + "/" + ((Identifier)unaryExpression.getOperand()).getName() + " I";
                    result += "aload 0\ndup\n";
                    result += "getfield " + actorVariableType + "\n";
                    result += "ldc 1\nisub\ndup_x1\n";
                    result += "putfield " + actorVariableType;
                }
                else {
                    int slot = (currentMain == null) ? slotOf(currentHandlerDeclaration, (Identifier)unaryExpression.getOperand()) :
                            slotOfMain(currentMain, (Identifier)unaryExpression.getOperand());
                    result += "iinc " + slot + " -1\n";
                    result += this.visitExpr(unaryExpression.getOperand());
                }
            }
            else if(unaryExpression.getOperand() instanceof ArrayCall) {
                result += this.visitExpr(((ArrayCall)unaryExpression.getOperand()).getArrayInstance()) + "\n";
                result += this.visitExpr(((ArrayCall)unaryExpression.getOperand()).getIndex()) + "\n";
                result += "dup2\niaload\nldc 1\nisub\ndup_x2\niastore";
            }
            else if(unaryExpression.getOperand() instanceof ActorVarAccess) {
                String actorVariableType = currentActorDeclaration.getName().getName() + "/" + ((ActorVarAccess)unaryExpression.getOperand()).getVariable().getName() + " I";
                result += "aload 0\ndup\n";
                result += "getfield " + actorVariableType + "\n";
                result += "ldc 1\nisub\ndup_x1\n";
                result += "putfield " + actorVariableType;
            }
        }
        else if(operator == UnaryOperator.postdec) {
            if(unaryExpression.getOperand() instanceof Identifier) {
                if(isField((Identifier)unaryExpression.getOperand())) {
                    String actorVariableType = currentActorDeclaration.getName().getName() + "/" + ((Identifier)unaryExpression.getOperand()).getName() + " I";
                    result += "aload 0\ndup\n";
                    result += "getfield " + actorVariableType + "\n";
                    result += "dup_x1\nldc 1\nisub\n";
                    result += "putfield " + actorVariableType;
                }
                else {
                    int slot = (currentMain == null) ? slotOf(currentHandlerDeclaration, (Identifier) unaryExpression.getOperand()) :
                            slotOfMain(currentMain, (Identifier) unaryExpression.getOperand());
                    result += this.visitExpr(unaryExpression.getOperand()) + "\n";
                    result += "iinc " + slot + " -1";
                }
            }
            else if(unaryExpression.getOperand() instanceof ArrayCall) {
                result += this.visitExpr(((ArrayCall)unaryExpression.getOperand()).getArrayInstance()) + "\n";
                result += this.visitExpr(((ArrayCall)unaryExpression.getOperand()).getIndex()) + "\n";
                result += "dup2\niaload\ndup_x2\nldc 1\nisub\niastore";
            }
            else if(unaryExpression.getOperand() instanceof ActorVarAccess) {
                String actorVariableType = currentActorDeclaration.getName().getName() + "/" + ((ActorVarAccess)unaryExpression.getOperand()).getVariable().getName() + " I";
                result += "aload 0\ndup\n";
                result += "getfield " + actorVariableType + "\n";
                result += "dup_x1\nldc 1\nisub\n";
                result += "putfield " + actorVariableType;
            }
        }
        else if(operator == UnaryOperator.preinc) {
            if(unaryExpression.getOperand() instanceof Identifier) {
                if(isField((Identifier)unaryExpression.getOperand())) {
                    String actorVariableType = currentActorDeclaration.getName().getName() + "/" + ((Identifier)unaryExpression.getOperand()).getName() + " I";
                    result += "aload 0\ndup\n";
                    result += "getfield " + actorVariableType + "\n";
                    result += "ldc 1\niadd\ndup_x1\n";
                    result += "putfield " + actorVariableType;
                }
                else {
                    int slot = (currentMain == null) ? slotOf(currentHandlerDeclaration, (Identifier) unaryExpression.getOperand()) :
                            slotOfMain(currentMain, (Identifier) unaryExpression.getOperand());
                    result += "iinc " + slot + " 1\n";
                    result += this.visitExpr(unaryExpression.getOperand());
                }
            }
            else if(unaryExpression.getOperand() instanceof ArrayCall) {
                result += this.visitExpr(((ArrayCall)unaryExpression.getOperand()).getArrayInstance()) + "\n";
                result += this.visitExpr(((ArrayCall)unaryExpression.getOperand()).getIndex()) + "\n";
                result += "dup2\niaload\nldc 1\niadd\ndup_x2\niastore";
            }
            else if(unaryExpression.getOperand() instanceof ActorVarAccess) {
                String actorVariableType = currentActorDeclaration.getName().getName() + "/" + ((ActorVarAccess)unaryExpression.getOperand()).getVariable().getName() + " I";
                result += "aload 0\ndup\n";
                result += "getfield " + actorVariableType + "\n";
                result += "ldc 1\niadd\ndup_x1\n";
                result += "putfield " + actorVariableType;
            }
        }
        else if(operator == UnaryOperator.postinc) {
            if(unaryExpression.getOperand() instanceof Identifier) {
                if(isField((Identifier)unaryExpression.getOperand())) {
                    String actorVariableType = currentActorDeclaration.getName().getName() + "/" + ((Identifier)unaryExpression.getOperand()).getName() + " I";
                    result += "aload 0\ndup\n";
                    result += "getfield " + actorVariableType + "\n";
                    result += "dup_x1\nldc 1\niadd\n";
                    result += "putfield " + actorVariableType;
                }
                else {
                    int slot = (currentMain == null) ? slotOf(currentHandlerDeclaration, (Identifier) unaryExpression.getOperand()) :
                            slotOfMain(currentMain, (Identifier) unaryExpression.getOperand());
                    result += this.visitExpr(unaryExpression.getOperand()) + "\n";
                    result += "iinc " + slot + " 1";
                }
            }
            else if(unaryExpression.getOperand() instanceof ArrayCall) {
                result += this.visitExpr(((ArrayCall)unaryExpression.getOperand()).getArrayInstance()) + "\n";
                result += this.visitExpr(((ArrayCall)unaryExpression.getOperand()).getIndex()) + "\n";
                result += "dup2\niaload\ndup_x2\nldc 1\niadd\niastore";
            }
            else if(unaryExpression.getOperand() instanceof ActorVarAccess) {
                String actorVariableType = currentActorDeclaration.getName().getName() + "/" + ((ActorVarAccess)unaryExpression.getOperand()).getVariable().getName() + " I";
                result += "aload 0\ndup\n";
                result += "getfield " + actorVariableType + "\n";
                result += "dup_x1\nldc 1\niadd\n";
                result += "putfield " + actorVariableType;
            }
        }
        return result;
    }

    @Override
    public String visit(BinaryExpression binaryExpression) {
        BinaryOperator operator = binaryExpression.getBinaryOperator();
        String result = "";
        if(!((operator == BinaryOperator.and) || (operator == BinaryOperator.or) || (operator == BinaryOperator.assign))) {
            result += this.visitExpr(binaryExpression.getLeft()) + "\n";
            result += this.visitExpr(binaryExpression.getRight()) + "\n";
        }
        if (operator == BinaryOperator.add)
            result += "iadd";
        if (operator == BinaryOperator.sub)
            result += "isub";
        if (operator == BinaryOperator.mult)
            result += "imul";
        if (operator == BinaryOperator.div)
            result += "idiv";
        if (operator == BinaryOperator.mod)
            result += "irem";
        if((operator == BinaryOperator.gt) || (operator == BinaryOperator.lt)) {
            String falseLabel = getFreshLabel();
            String exitLabel = getFreshLabel();
            String command = (operator == BinaryOperator.gt) ? "if_icmple " : "if_icmpge ";
            result += command + falseLabel + "\n";
            result += "ldc 1" + "\n";
            result += "goto " + exitLabel + "\n";
            result += falseLabel + ":\n";
            result += "ldc 0\n";
            result += exitLabel + ":";
        }
        if((operator == BinaryOperator.eq) || (operator == BinaryOperator.neq)) {
            Type type = expressionTypeChecker.visitExpr(binaryExpression.getLeft());
            if((type instanceof IntType) || (type instanceof BooleanType)) {
                String falseLabel = getFreshLabel();
                String exitLabel = getFreshLabel();
                String command = (operator == BinaryOperator.eq) ? "if_icmpne " : "if_icmpeq ";
                result += command + falseLabel + "\n";
                result += "ldc 1" + "\n";
                result += "goto " + exitLabel + "\n";
                result += falseLabel + ":\n";
                result += "ldc 0\n";
                result += exitLabel + ":";
            }
            else {
                if(type instanceof ArrayType)
                    result += "invokestatic java/util/Arrays.equals([I[I)Z";
                else
                    result += "invokevirtual java/lang/Object.equals(Ljava/lang/Object;)Z";
                if(operator == BinaryOperator.neq) {
                    String falseLabel = getFreshLabel();
                    String exitLabel = getFreshLabel();
                    result += "\nifne " + falseLabel + "\n";
                    result += "ldc 1" + "\n";
                    result += "goto " + exitLabel + "\n";
                    result += falseLabel + ":\n";
                    result += "ldc 0\n";
                    result += exitLabel + ":";
                }
            }
        }
        if(operator == BinaryOperator.and) {
            String falseLabel = getFreshLabel();
            String exitLabel = getFreshLabel();
            result += this.visitExpr(binaryExpression.getLeft()) + "\n";
            result += "ifeq " + falseLabel + "\n";
            result += this.visitExpr(binaryExpression.getRight()) + "\n";
            result += "ifeq " + falseLabel + "\n";
            result += "ldc 1\n";
            result += "goto " + exitLabel + "\n";
            result += falseLabel + ":\n";
            result += "ldc 0\n";
            result += exitLabel + ":";

        }
        if(operator == BinaryOperator.or) {
            String falseLabel = getFreshLabel();
            String exitLabel = getFreshLabel();
            result += this.visitExpr(binaryExpression.getLeft()) + "\n";
            result += "ifne " + falseLabel + "\n";
            result += this.visitExpr(binaryExpression.getRight()) + "\n";
            result += "ifne " + falseLabel + "\n";
            result += "ldc 0\n";
            result += "goto " + exitLabel + "\n";
            result += falseLabel + ":\n";
            result += "ldc 1\n";
            result += exitLabel + ":";
        }
        if(operator == BinaryOperator.assign) {
            Type lValType = expressionTypeChecker.visitExpr(binaryExpression.getLeft());
            String rValCommands = this.visitExpr(binaryExpression.getRight());
            String currentActorName = currentActorDeclaration.getName().getName();
            if(binaryExpression.getLeft() instanceof Identifier) {
                if(isField((Identifier) binaryExpression.getLeft())) {
                    result += "aload 0\n";
                    result += rValCommands + "\n";
                    String varName = ((Identifier)binaryExpression.getLeft()).getName();
                    result += "putfield " + currentActorName + "/" + varName + " " +  makeTypeSignature(lValType) + "\n";
                    result += rValCommands;
                }
                else {
                    result += rValCommands + "\n";
                    String intOrObj = ((lValType instanceof IntType) || (lValType instanceof BooleanType)) ? "i" : "a";
                    result += intOrObj + "store " + slotOf(currentHandlerDeclaration, (Identifier) binaryExpression.getLeft()) + "\n";
                    result += rValCommands;
                }
            }
            else if(binaryExpression.getLeft() instanceof ActorVarAccess) {
                result += "aload 0\n";
                result += rValCommands + "\n";
                String instanceName = ((ActorVarAccess)binaryExpression.getLeft()).getVariable().getName();
                result += "putfield " + currentActorDeclaration.getName().getName() + "/" + instanceName + " " + makeTypeSignature(lValType) + "\n";
                result += rValCommands;
            }
            else if(binaryExpression.getLeft() instanceof ArrayCall) {
                result += this.visitExpr(((ArrayCall)binaryExpression.getLeft()).getArrayInstance()) + "\n";
                result += this.visitExpr(((ArrayCall)binaryExpression.getLeft()).getIndex()) + "\n";
                result += rValCommands + "\n";
                result += "iastore\n";
                result += rValCommands;
            }
        }
        return result;
    }

    @Override
    public String visit(ArrayCall arrayCall) {
        String result = this.visitExpr(arrayCall.getArrayInstance()) + "\n";
        result += this.visitExpr(arrayCall.getIndex()) + "\n";
        result += "iaload";
        return result;
    }

    @Override
    public String visit(ActorVarAccess actorVarAccess) {
        Type type = expressionTypeChecker.visitExpr(actorVarAccess);
        String currentActorName = currentActorDeclaration.getName().getName();
        return "aload 0\ngetfield " + currentActorName + "/" + actorVarAccess.getVariable().getName() + " " + makeTypeSignature(type);
    }

    @Override
    public String visit(Identifier identifier) {
        Type type = expressionTypeChecker.visitExpr(identifier);
        String currentActorName = currentActorDeclaration.getName().getName();
        String intOrObj = ((type instanceof IntType) || (type instanceof BooleanType)) ? "i" : "a";
        if((currentMain != null))
            return intOrObj + "load " + slotOfMain(currentMain, identifier);
        if(isField(identifier))
            return "aload 0\ngetfield " + currentActorName + "/" + identifier.getName() + " " + makeTypeSignature(type);
        return intOrObj + "load " + slotOf(currentHandlerDeclaration, identifier);
    }

    @Override
    public String visit(Self self) {
        return "aload 0";
    }

    @Override
    public String visit(Sender sender) {
        return "aload 1";
    }

    @Override
    public String visit(BooleanValue value) {
        int boolIntVal = (value.getConstant()) ? 1 : 0;
        return "ldc " + boolIntVal;
    }

    @Override
    public String visit(IntValue value) {
        return "ldc " + value.getConstant();
    }

    @Override
    public String visit(StringValue value) {
        return "ldc " + value.getConstant();
    }

    @Override
    public void visit(Block block) {
        if(block.getStatements() != null) {
            for (Statement statement : block.getStatements()) {
                this.visitStatement(statement);
            }
        }
    }

    @Override
    public void visit(Conditional conditional) {
        String elseLabel = getFreshLabel();
        String exitLabel = getFreshLabel();
        addCommand(this.visitExpr(conditional.getExpression()));
        addCommand("ifeq " + elseLabel);
        if(conditional.getThenBody() != null)
            this.visitStatement(conditional.getThenBody());
        addCommand("goto " + exitLabel);
        addCommand(elseLabel + ":");
        if(conditional.getElseBody() != null)
            this.visitStatement(conditional.getElseBody());
        addCommand(exitLabel + ":");
    }

    @Override
    public void visit(For loop) {
        String loopStartLabel = getFreshLabel();
        String loopExitLabel = getFreshLabel();
        String updateLabel = getFreshLabel();
        currentStartForLoopLabel = updateLabel;
        currentExitForLoopLabel = loopExitLabel;
        if(loop.getInitialize() != null)
            this.visitStatement(loop.getInitialize());
        addCommand(loopStartLabel + ":");
        if(loop.getCondition() != null) {
            addCommand(this.visitExpr(loop.getCondition()));
            addCommand("ifeq " + loopExitLabel);
        }
        if(loop.getBody() != null)
            this.visitStatement(loop.getBody());
        addCommand(updateLabel + ":");
        if(loop.getUpdate() != null)
            this.visitStatement(loop.getUpdate());
        addCommand("goto " + loopStartLabel);
        addCommand(loopExitLabel + ":");
    }

    @Override
    public void visit(Break breakLoop) {
        addCommand("goto " + currentExitForLoopLabel);
    }

    @Override
    public void visit(Continue continueLoop) {
        addCommand("goto " + currentStartForLoopLabel);
    }

    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        Type instanceType = expressionTypeChecker.visitExpr(msgHandlerCall.getInstance());
        addCommand(this.visitExpr(msgHandlerCall.getInstance()));
        addCommand("aload 0");
        String argsSignatures = "";
        String instanceTypeName;
        if(instanceType instanceof SenderType)
            instanceTypeName = "Actor";
        else
            instanceTypeName = ((ActorType)instanceType).getActorDeclaration().getName().getName();
        if(msgHandlerCall.getArgs() != null) {
            for(Expression expression : msgHandlerCall.getArgs()) {
                addCommand(this.visitExpr(expression));
                argsSignatures += makeTypeSignature(expressionTypeChecker.visitExpr(expression));
            }
        }
        addCommand("invokevirtual " + instanceTypeName + "/send_" + msgHandlerCall.getMsgHandlerName().getName() + "(LActor;" + argsSignatures + ")V");
    }

    @Override
    public void visit(Print print) {
        Type type = expressionTypeChecker.visitExpr(print.getArg());
        addCommand("getstatic java/lang/System/out Ljava/io/PrintStream;");
        if (type instanceof IntType) {
            addCommand(this.visitExpr(print.getArg()));
            addCommand("invokevirtual java/io/PrintStream/println(I)V");
        }
        else if(type instanceof BooleanType) {
            addCommand(this.visitExpr(print.getArg()));
            addCommand("invokevirtual java/io/PrintStream/println(Z)V");
        }
        else if (type instanceof StringType){
            addCommand(this.visitExpr(print.getArg()));
            addCommand("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
        }
        else if(type instanceof ArrayType){
            addCommand(this.visitExpr(print.getArg()));
            addCommand("invokestatic java/util/Arrays/toString([I)Ljava/lang/String;");
            addCommand("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
        }
    }

    @Override
    public void visit(Assign assign) {
        Type lValType = expressionTypeChecker.visitExpr(assign.getlValue());
        String rValCommands = this.visitExpr(assign.getrValue());
        String currentActorName = currentActorDeclaration.getName().getName();
        if(assign.getlValue() instanceof Identifier) {
            if(isField((Identifier) assign.getlValue())) {
                addCommand("aload 0");
                addCommand(rValCommands);
                String varName = ((Identifier)assign.getlValue()).getName();
                addCommand("putfield " + currentActorName + "/" + varName + " " +  makeTypeSignature(lValType));
            }
            else {
                addCommand(rValCommands);
                String intOrObj = ((lValType instanceof IntType) || (lValType instanceof BooleanType)) ? "i" : "a";
                addCommand(intOrObj + "store " + slotOf(currentHandlerDeclaration, (Identifier) assign.getlValue()));
            }
        }
        else if(assign.getlValue() instanceof ActorVarAccess) {
            addCommand("aload 0");
            addCommand(rValCommands);
            String instanceName = ((ActorVarAccess)assign.getlValue()).getVariable().getName();
            addCommand("putfield " + currentActorDeclaration.getName().getName() + "/" + instanceName + " " + makeTypeSignature(lValType));
        }
        else if(assign.getlValue() instanceof ArrayCall) {
            addCommand(this.visitExpr(((ArrayCall)assign.getlValue()).getArrayInstance()));
            addCommand(this.visitExpr(((ArrayCall)assign.getlValue()).getIndex()));
            addCommand(rValCommands);
            addCommand("iastore");
        }
    }
}

