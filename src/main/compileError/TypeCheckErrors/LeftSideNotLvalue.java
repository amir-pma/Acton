package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class LeftSideNotLvalue extends CompileErrorException {
    public LeftSideNotLvalue (int line) {
        super(line, "left side of assignment must be a valid lvalue");
    }
}