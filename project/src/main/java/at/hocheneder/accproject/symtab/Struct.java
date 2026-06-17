package at.hocheneder.accproject.symtab;

/**
 * @param kind the Kind of the type
 */
public record Struct(Kind kind, int size) {

    public enum Kind {
        None, Int, Char
    }

    //Map<String, Obj> fields; // Rec: fields
}