package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class SenderInInitial extends CompileErrorException {
    public SenderInInitial (int line) {
        super(line, "no sender in initial msghandler");
    }
}