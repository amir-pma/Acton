package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class ConditionNotBoolean extends CompileErrorException {
    public ConditionNotBoolean(int line) {
        super(line, "condition type must be boolean");
    }
}