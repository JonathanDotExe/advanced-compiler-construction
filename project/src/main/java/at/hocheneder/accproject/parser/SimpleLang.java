package codegen;

public class SimpleLang {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java SimpleLang <filename>");
            return;
        }
        String filename = args[0];
        Parser p = new Parser(new Scanner(filename));
        p.Parse();
    }
}
