package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class ArrayIndexNotInt extends CompileErrorException {
    public ArrayIndexNotInt(int line) {
        super(line, "index of array is not an int value");
    }
}