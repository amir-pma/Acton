package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class VariableNotCallable extends CompileErrorException {
    public VariableNotCallable (int line, String name) {
        super(line, "variable " + name + " is not callable");
    }
}