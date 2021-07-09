package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class KnownActorsNotMatchDefinition extends CompileErrorException {
    public KnownActorsNotMatchDefinition (int line) {
        super(line, "knownactors do not match with definition");
    }
}