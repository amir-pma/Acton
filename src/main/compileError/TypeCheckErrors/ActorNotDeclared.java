package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class ActorNotDeclared extends CompileErrorException {
    public ActorNotDeclared(int line, String name) {
        super(line, "actor " + name + " is not declared");
    }
}
