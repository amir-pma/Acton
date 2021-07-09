package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class VariableNotDeclared extends CompileErrorException {
    public VariableNotDeclared(int line, String name) {
        super(line, "variable " + name + " is not declared");
    }
}