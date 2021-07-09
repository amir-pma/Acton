package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class MsgHandlerArgsNotMatchDeclaration extends CompileErrorException {
    public MsgHandlerArgsNotMatchDeclaration(int line, String name) {
        super(line, "arguments do not match with definition");
    }
}
