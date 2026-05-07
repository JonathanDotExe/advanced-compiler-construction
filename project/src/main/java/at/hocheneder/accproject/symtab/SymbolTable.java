package at.hocheneder.accproject.symtab;

public class SymbolTable {

    // predefined objects and types
    public static final Struct noType = new Struct(Struct.Kind.None);
    public static final Struct intType = new Struct(Struct.Kind.Int);
    public static final Struct charType = new Struct(Struct.Kind.Char);
    public final Obj noObj = new Obj("noObj", Obj.Kind.Var, noType);


    private Scope curScope;
    private int curLevel;
    public Obj insert (String name, Obj.Kind kind, Struct type) {
        // Exercise UE-P-4
        if (curScope.findLocal(name) != null) {
            throw new RuntimeException("Duplicate local variables."); //FIXME
            //return noObj;
        }

        Obj obj = new Obj(name, kind, type);
        obj.level = curLevel;
        if (kind == Obj.Kind.Var) {
            obj.adr = curScope.nVars(); //FIXME?
        }
        curScope.insert(obj);

        return obj;
    }
    public Obj find (String name) {
        Obj obj = curScope.findGlobal(name);
        if (obj != null) {
            return obj;
        }
        throw new RuntimeException("Name not found."); //FIXME
    }
    //public Obj findField (String name, Struct recType) {}

    public void openScope() {
        curScope = new Scope(curScope);
        curLevel++;
    }
    public void closeScope() {
        curScope = curScope.outer();
        curLevel--;
    }

}
