package at.hocheneder.accproject.codegen;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class Code {
    private final byte[] code;
    private int pos;

    protected final StringBuilder assembly;

    public Code() {
        code = new byte[64 * 1024];
        pos = 0;
        assembly = new StringBuilder();
    }

    protected void emit(int b) {
        code[pos] = (byte) b;
        pos += 1;
    }

    protected void emit2(int b) {
        emit((byte) b);
        emit((byte) (b >> 8));
    }

    private void emitInstruction(byte b1, byte b2, byte b3, byte b4) {
        emit(b1);
        emit(b2);
        emit(b3);
        emit(b4);
    }

    protected void emitInstruction(int instr) {
        emitInstruction(
                (byte) (instr & 0xFF),
                (byte) ((instr >> 8) & 0xFF),
                (byte) ((instr >> 16) & 0xFF),
                (byte) ((instr >> 24) & 0xFF));
    }

    public void emitConst (int size, int x) {
        if (size == 1) emit((byte) x);
        else if (size == 2) emit2(x);
        else emitInstruction(x);
    }

    public void save(String binFileName, String asmFileName) {
        System.out.println("Generated binary code into " + binFileName + ":");
        try (FileOutputStream fos = new FileOutputStream(binFileName)) {
            fos.write(code, 0, pos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String asm = assembly.toString();

        System.out.println("Generated assembly program into " + asmFileName + ":");
        try (PrintWriter pw = new PrintWriter(asmFileName)) {
            pw.println(asm);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Generated Assembly:");
        System.out.println(asm);
    }
}
