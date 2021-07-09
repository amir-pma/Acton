package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class ArrayCallOnNotArray extends CompileErrorException {
    public ArrayCallOnNotArray(int line) {
        super(line, "cannot use index on not array type expression");
    }
}
