package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class InitialArgsNotMatchDeclaration extends CompileErrorException {
    public InitialArgsNotMatchDeclaration(int line, String name) {
        super(line, "arguments do not match with definition");
    }
}
