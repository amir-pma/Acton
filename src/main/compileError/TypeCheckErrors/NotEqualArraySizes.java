package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class NotEqualArraySizes extends CompileErrorException {
    public NotEqualArraySizes (int line) {
        super(line, "operation assign requires equal array sizes");
    }
}
