package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class UnsupportedOperandType extends CompileErrorException {
    public UnsupportedOperandType(int line, String name) {
        super(line, "unsupported operand type for " + name);
    }
}