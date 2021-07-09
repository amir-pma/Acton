package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class SenderNotInMain extends CompileErrorException {
    public SenderNotInMain (int line) {
        super(line, "self doesn't refer to any actor");
    }
}
