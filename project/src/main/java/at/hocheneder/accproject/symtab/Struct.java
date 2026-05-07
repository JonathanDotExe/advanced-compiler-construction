package at.hocheneder.accproject.symtab;

import java.util.Map;

public class Struct {
    public Struct(Kind kind) {
        this.kind = kind;
    }

    public enum Kind {
        None, Int, Char
    }
    public final Kind kind; // the Kind of the type
    //int size; // size in byte

    //Map<String, Obj> fields; // Rec: fields
}