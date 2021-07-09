package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class SelfNotInMain extends CompileErrorException {
    public SelfNotInMain (int line) {
        super(line, "self doesn't refer to any actor");
    }
}
