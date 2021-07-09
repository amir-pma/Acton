package main.compileError.TypeCheckErrors;

import main.compileError.CompileErrorException;

public class IncDecOperandNotLvalue extends CompileErrorException {
    //type: 0->inc  1->dec
    public IncDecOperandNotLvalue (int line, int type) {
        super(line, "lvalue required as " + ((type==0) ? "increment" : "decrement") + " operand");
    }
}