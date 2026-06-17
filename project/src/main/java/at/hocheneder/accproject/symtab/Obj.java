package at.hocheneder.accproject.symtab;

import java.util.Map;

public class Obj {

    public enum Kind {
        Con,    // constant value
        Type,   // type (int, char the lang doesnt support more)
        Var,    // variable
        //ValPar, // pass by value parameter
        Proc    // function
    }

    public final String name;
    public final Kind kind;
    public Struct type;
    public int val; // Con: constant value
    public int adr; // Var, ValPar, VarPar, Field, Proc: address
    public int level;// Var: 0 = global, 1 = local
    public int nVars, nPars; // for Proc (Procedure/Function)
    public Map<String, Obj> locals; // Proc: parameters and local objects

    public Obj(String name, Kind kind, Struct type) {
        this.name = name;
        this.kind = kind;
        this.type = type;
    }
}
