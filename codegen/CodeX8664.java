package codegen;

class CodeX8664 extends Code {
    public static final int RAX = 0;
    public static final int RBX = 3;
    public static final int RSP = 4;
    public static final int RBP = 5;
    public static final int RDI = 7;
    public static final int R12 = 12;

    private String registerName(int reg) {
        switch (reg) {
            case RAX: return "rax";
            case RBX: return "rbx";
            case RSP: return "rsp";
            case RBP: return "rbp";
            case RDI: return "rdi";
            case R12: return "r12";
            default: throw new IllegalArgumentException("Unknown register: " + reg);
        }
    }

	public void emitMoveImmediate(int register, byte imm) {
		assembly.append("mov ").append(registerName(register)).append(", ").append(imm).append("\n");

        int signExtended = (int) imm; // Java sign-extends byte → int automatically
        byte opcode      = (byte) (0xB8 | (register & 0x7));
        byte immByte0    = (byte)  (signExtended         & 0xFF);
        byte immByte1    = (byte) ((signExtended >>  8)  & 0xFF);
        byte immByte2    = (byte) ((signExtended >> 16)  & 0xFF);
        byte immByte3    = (byte) ((signExtended >> 24)  & 0xFF);

        emit(opcode);
        emit(immByte0);
        emit(immByte1);
        emit(immByte2);
        emit(immByte3);
	}

    public void emitCallRegister(int callTargetReg) {
        assembly.append("call ").append(registerName(callTargetReg)).append("\n");

        byte modRM = (byte) (0xD0 | (callTargetReg & 0x7));

        if (callTargetReg >= 8) {
            emit((byte) 0x41);        // REX.B
        }
        emit((byte) 0xFF);            // opcode
        emit(modRM);                  // ModRM
    }

    public void emitPushRegister(int register) {
        assembly.append("push ").append(registerName(register)).append("\n");

        if (register >= 8) {
            emit((byte) 0x41);        // REX.B
        }
        emit((byte) (0x50 | (register & 0x7)));
    }

    public void emitMoveRegister(int targetReg, int sourceReg) {
        assembly.append("mov ").append(registerName(targetReg)).append(", ").append(registerName(sourceReg)).append("\n");

        byte modRM = (byte) (0xC0 | ((targetReg & 0x7) << 3) | (sourceReg & 0x7));

        if (targetReg >= 8 || sourceReg >= 8) {
            emit((byte) 0x41);        // REX.B
        }
        emit((byte) 0x89);            // opcode
        emit(modRM);                  // ModRM



    //         byte rex    = (byte) (0x48
    //                     | (sourceReg >= 8 ? 0x04 : 0x00)   // REX.R
    //                     | (targetReg >= 8 ? 0x01 : 0x00));  // REX.B
    // byte modRM  = (byte) (0xC0
    //                     | ((sourceReg & 0x7) << 3)
    //                     |  (targetReg & 0x7));

    // emit(rex);
    // emit((byte) 0x89);   // opcode: MOV r/m64, r64
    // emit(modRM);
    }

    public void emitMoveRegisterFromMemory(int targetReg, int baseReg, int displacement) {
        assembly.append("mov ").append(registerName(targetReg)).append(", [").append(registerName(baseReg)).append(" + ").append(displacement).append("]\n");

        boolean needsSib  = (baseReg & 0x7) == 4;   // RSP or R12
        boolean dispZero  = (displacement == 0) && (baseReg & 0x7) != 5; // RBP/R13 always needs disp
        boolean disp8ok   = !dispZero && displacement >= -128 && displacement <= 127;

        int mod = dispZero ? 0b00 : (disp8ok ? 0b01 : 0b10);

        byte rex   = (byte) (0x48
                            | (targetReg >= 8 ? 0x04 : 0x00)   // REX.R
                            | (baseReg   >= 8 ? 0x01 : 0x00));  // REX.B
        byte modRM = (byte) ((mod << 6)
                            | ((targetReg & 0x7) << 3)
                            |  (baseReg   & 0x7));

        emit(rex);
        emit((byte) 0x8B);
        emit(modRM);

        if (needsSib) {
            emit((byte) 0x24);   // SIB: scale=00, index=100 (no index), base=reg
        }

        if (!dispZero) {
            if (disp8ok) {
                emit((byte) displacement);
            } else {
                emit((byte)  (displacement         & 0xFF));
                emit((byte) ((displacement >>  8)  & 0xFF));
                emit((byte) ((displacement >> 16)  & 0xFF));
                emit((byte) ((displacement >> 24)  & 0xFF));
            }
        }
    }

    public void emitPopRegister(int register) {
        assembly.append("pop ").append(registerName(register)).append("\n");

        if (register >= 8) {
            emit((byte) 0x41);        // REX.B
        }
        emit((byte) (0x58 | (register & 0x7)));
    }

    public void emitReturn() {
        assembly.append("ret\n");
        emit((byte) 0xC3);
    }
}