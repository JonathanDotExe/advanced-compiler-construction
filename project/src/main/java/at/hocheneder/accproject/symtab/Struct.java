package at.hocheneder.accproject.symtab;

/**
 * @param kind the Kind of the type
 */
public record Struct(Kind kind) {

    public enum Kind {
        None, Int, Char
    }
    //int size; // size in byte

    //Map<String, Obj> fields; // Rec: fields
}