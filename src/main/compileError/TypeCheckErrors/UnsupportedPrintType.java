package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class UnsupportedPrintType extends CompileErrorException {
    public UnsupportedPrintType (int line) {
        super(line, "unsupported type for print");
    }
}