package at.hocheneder.accproject.symtab;

public class SymbolTable {

    // predefined objects and types
    public static final Struct noType = new Struct(Struct.Kind.None, 0);
    public static final Struct intType = new Struct(Struct.Kind.Int, 4);
    public static final Struct charType = new Struct(Struct.Kind.Char, 1);
    public final Obj noObj = new Obj("noObj", Obj.Kind.Var, noType);

    public final Obj chrObj;
    public final Obj ordObj;
    public final Obj putObj;
    public final Obj putLnObj;

    public Scope curScope;

    public int getCurLevel() {
        return curLevel;
    }

    private int curLevel;


    public SymbolTable() {
        // construct universe
        openScope();

        insert("int", Obj.Kind.Type, intType);
        insert("char", Obj.Kind.Type, charType);

        //CHR
        chrObj = insert("CHR", Obj.Kind.Proc, charType);
        openScope();
        Obj iVarObj = insert("i", Obj.Kind.Var, intType);
        iVarObj.level = 1;
        chrObj.nPars = curScope.nVars();
        chrObj.nVars = curScope.nVars();
        chrObj.locals = curScope.locals();
        closeScope();

        //Ord
        ordObj = insert("ORD", Obj.Kind.Proc, intType);
        openScope();
        Obj chVarObj = insert("ch", Obj.Kind.Var, charType);
        chVarObj.level = 1;
        ordObj.nPars = curScope.nVars();
        ordObj.nVars = curScope.nVars();
        ordObj.locals = curScope.locals();
        closeScope();

        //Put
        putObj = insert("put", Obj.Kind.Proc, noType);
        openScope();
        Obj eVarObj = insert("e", Obj.Kind.Var, charType);
        eVarObj.level = 1;
        putObj.nPars = curScope.nVars();
        putObj.nVars = curScope.nVars();
        putObj.locals = curScope.locals();
        closeScope();

        //Put ln
        putLnObj = insert("putLn", Obj.Kind.Proc, noType);
        openScope();
        putLnObj.nPars = curScope.nVars();
        putLnObj.nVars = curScope.nVars();
        putLnObj.locals = curScope.locals();
        closeScope();

    }

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
        throw new RuntimeException("Name not found: " + name); //FIXME
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
