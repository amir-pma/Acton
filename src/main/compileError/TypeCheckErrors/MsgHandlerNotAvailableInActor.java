package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class MsgHandlerNotAvailableInActor extends CompileErrorException {
    public MsgHandlerNotAvailableInActor(int line, String msgHandlerName, String actorName) {
        super(line, "there is no msghandler named " + msgHandlerName + " in actor " + actorName);
    }
}