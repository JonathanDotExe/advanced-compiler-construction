package codegen;

import java.io.FileOutputStream;

abstract class Code {
    private final byte[] code;
    private int pos;

    protected final StringBuilder assembly;

    public Code() {
        code = new byte[64 * 1024];
        pos = 0;
        assembly = new StringBuilder();
    }

    protected void emit(byte b) {
        code[pos] = b;
        pos += 1;
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

    public void save(String filename) {
        // open filename for writing and write the code byte array to it
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(code, 0, pos);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        System.out.println("Generated assembly code into " + filename + ":");
        System.out.println(assembly.toString());
    }
}
