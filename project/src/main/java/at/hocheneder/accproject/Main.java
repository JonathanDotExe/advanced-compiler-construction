package at.hocheneder.accproject;

import at.hocheneder.accproject.codegen.CodeX8664;
import at.hocheneder.accproject.parser.Parser;
import at.hocheneder.accproject.parser.Scanner;
import at.hocheneder.accproject.symtab.SymbolTable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 1) {
            System.err.println("Usage: java SimpleLang <filename>");
            return;
        }

        CodeX8664 code = new CodeX8664(new SymbolTable());

        String filename = args[0];
        Parser p = new Parser(new Scanner(filename), code);
        p.Parse();

        //Store code
        String name = filename.substring(0, filename.lastIndexOf("."));
        code.save(name + ".bin", name + ".asm");
    }
}
