package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class BreakContinueNotInLoop extends CompileErrorException {
    //type: 0->break  1->continue
    public BreakContinueNotInLoop (int line, int type) {
        super(line, ((type == 0) ? "break" : "continue") + " statement not within loop");
    }
}