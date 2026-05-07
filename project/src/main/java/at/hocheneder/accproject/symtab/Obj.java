package at.hocheneder.accproject.symtab;

import java.util.Map;

public class Obj {

    public enum Kind { Con, Type, Var, ValPar, Proc }
    final String name;
    final Kind kind;
    final Struct type;
    int val; // Con: constant value
    int adr; // Var, ValPar, VarPar, Field, Proc: address
    int level;// Var: 0 = global, 1 = local
    int nVars, nPars; // for Proc (Procedure/Function)
    Map<String, Obj> locals; // Proc: parameters and local objects

    Obj(String name, Kind kind, Struct type) {
        this.name = name;
        this.kind = kind;
        this.type = type;
    }
}
