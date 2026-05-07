package at.hocheneder.accproject.symtab;

public class Operand {

    public enum Kind {
        Con, Abs, Reg, RegRel, Fun, Cond;
    };
    public Kind kind; // Con, Abs, Reg, RegRel, Fun, Cond
    public Struct type;
    public int val; // Con: constant value
    public int reg; // Reg, RegRel: register
    public int adr; // Abs, Fun: address; RegRel: offset
    public int inx; // Abs, RegRel: index register if not none
    public int scale; // Abs, RegRel: scale factor if inx != none
    int op; // Cond: operator of the last compare
    int T; // Cond: head of TJMP list
    int F; // Cond: head of FJMP list

}
