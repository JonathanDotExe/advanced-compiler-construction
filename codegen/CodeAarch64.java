package codegen;

class CodeAarch64 implements Code {
    private final byte[] code;
    private int pos;

    private final StringBuilder assembly;

    public static final int SP = 31;

    public CodeAarch64() {
        code = new byte[64 * 1024];
        pos = 0;
        assembly = new StringBuilder();
    }

    public void save(String filename) {
        // open filename for writing and write the code byte array to it
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filename)) {
            fos.write(code, 0, pos);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        System.out.println("Generated assembly code into " + filename + ":");
        System.out.println(assembly.toString());
    }

    private void emit(byte b) {
        code[pos] = b;
        pos += 1;
    }

    private void emitInstruction(byte b1, byte b2, byte b3, byte b4) {
        emit(b1);
        emit(b2);
        emit(b3);
        emit(b4);
    }

    private void emitInstruction(int instr) {
        emitInstruction(
                (byte) (instr & 0xFF),
                (byte) ((instr >> 8) & 0xFF),
                (byte) ((instr >> 16) & 0xFF),
                (byte) ((instr >> 24) & 0xFF));
    }

    private String registerName(int reg) {
        if (reg == SP) {
            return "sp";
        }
        return "x" + reg;
    }

    public void emitLoadRegister(int targetReg, int baseReg, int offset) {
        assembly.append("ldr ").append(registerName(targetReg)).append(", [").append(registerName(baseReg)).append(", #").append(offset).append("]\n");
        int instr;

        // Offset must be positive, a multiple of 8, and fit in 12 bits (max 4095 * 8 = 32760)
        if (offset >= 0 && offset % 8 == 0 && (offset / 8) <= 4095) {
            int imm12 = offset / 8;
            instr = 0xF9400000
                    | (imm12 << 10)
                    | ((baseReg & 0x1F) << 5)
                    | (targetReg & 0x1F);
        } else {
            throw new IllegalArgumentException("Offset out of bounds for LDR.");
        }

        emitInstruction(instr);
    }

    public void emitLoadPairOfRegisterPostIndexed(int reg1, int reg2, int baseReg, int offset) {
        assembly.append("ldp ").append(registerName(reg1)).append(", ").append(registerName(reg2)).append(", [").append(registerName(baseReg)).append("], #").append(offset).append("\n");

        if (offset % 8 != 0) {
            throw new IllegalArgumentException("Offset must be a multiple of 8");
        }
        if (offset < -512 || offset > 504) {
            throw new IllegalArgumentException("Offset out of bounds [-512, 504]");
        }

        int imm7 = (offset / 8) & 0x7F;

        int instr = 0xA8C00000
                | (imm7 << 15)
                | ((reg2 & 0x1F) << 10)
                | ((baseReg & 0x1F) << 5)
                | (reg1 & 0x1F);

        emitInstruction(instr);
    }

    public void emitStorePairOfRegisterPreIndex(int reg1, int reg2, int baseReg, int offset) {
        assembly.append("stp ").append(registerName(reg1)).append(", ").append(registerName(reg2)).append(", [").append(registerName(baseReg)).append(", #").append(offset).append("]!\n");

        int instr = 0;
        instr |= (0b101010011 << 23);

        // imm7 is a 7-bit signed immediate. For 64-bit STP, the offset must be a multiple of 8.
        int imm7 = (offset / 8) & 0x7F;
        instr |= (imm7 << 15);

        instr |= ((reg2 & 0x1F) << 10);   // Rt2
        instr |= ((baseReg & 0x1F) << 5); // Rn
        instr |= (reg1 & 0x1F);           // Rt

        emitInstruction(instr);
    }

    public void emitMove(int targetReg, int sourceReg) {
        assembly.append("mov ").append(registerName(targetReg)).append(", ").append(registerName(sourceReg)).append("\n");

        int instr;

        // Check if the Stack Pointer (SP) is either the source or the target
        if (targetReg == SP || sourceReg == SP) {
            // Alias for: ADD Xd, Xn, #0 (Add immediate)
            // When using the ADD immediate instruction, register 31 is interpreted as SP.
            // Base encoding bits:
            // sf(1) op(0) S(0) 100010(6 bits) shift(00) imm12(0x000) Rn(5 bits) Rd(5 bits)
            // This simplifies to a base of 0x91000000
            instr = 0x91000000
                    | ((sourceReg & 0x1F) << 5)   // Rn: Source register
                    | (targetReg & 0x1F);         // Rd: Target register
        } else {
            // Alias for: ORR Xd, XZR, Xm (Logical OR, shifted register)
            // Used for general purpose registers (X0-X30). Here, register 31 is interpreted as XZR.
            // We are OR'ing the source register with 0 (XZR) and storing it in the target.
            // Base encoding bits:
            // sf(1) opc(01) 01010(5 bits) shift(00) N(0) Rm(5 bits) imm6(0) Rn(11111 for XZR) Rd(5 bits)
            // This simplifies to a base of 0xAA0003E0
            instr = 0xAA0003E0
                    | ((sourceReg & 0x1F) << 16)  // Rm: Source register
                    | (targetReg & 0x1F);         // Rd: Target register
        }
        emitInstruction(instr);
    }

    public void emitMoveImmediate(int targetReg, byte immediate) {
        assembly.append("mov ").append(registerName(targetReg)).append(", #").append(immediate).append("\n");
        int rd = targetReg & 0x1F;

        // MOVZ Xd, #low16, LSL #0
        // Base: 0xD2800000 | imm16 << 5 | Rd
        emitInstruction(0xD2800000 | (immediate << 5) | rd);
    }

    public void emitStoreRegister(int sourceReg, int baseReg, int offset) {
        assembly.append("str ").append(registerName(sourceReg)).append(", [").append(registerName(baseReg)).append(", #").append(offset).append("]\n");
        int instr;

        // Offset must be positive, a multiple of 8, and fit in 12 bits (max 4095 * 8 = 32760)
        if (offset >= 0 && offset % 8 == 0 && (offset / 8) <= 4095) {
            int imm12 = offset / 8;
            instr = 0xF9000000
                    | (imm12 << 10)
                    | ((baseReg & 0x1F) << 5)
                    | (sourceReg & 0x1F);
        } else {
            throw new IllegalArgumentException("Offset out of bounds for STR.");
        }

        emitInstruction(instr);
    }

    public void emitBranchWithLinkToRegister(int targetReg) {
        assembly.append("blr ").append(registerName(targetReg)).append("\n");

        int instr = 0xD63F0000 | ((targetReg & 0x1F) << 5);
        emitInstruction(instr);
    }

    public void emitReturn() {
        assembly.append("ret\n");
        emitInstruction(0xD65F03C0);
    }
}
