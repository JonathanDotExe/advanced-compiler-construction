package at.hocheneder.accproject.parser;

import at.hocheneder.accproject.codegen.CodeX8664;
import at.hocheneder.accproject.symtab.SymbolTable;

public class SimpleLang {
    static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java SimpleLang <filename>");
            return;
        }

        CodeX8664 code = new CodeX8664(new SymbolTable());

        String filename = args[0];
        Parser p = new Parser(new Scanner(filename), code);
        p.Parse();
    }
}
