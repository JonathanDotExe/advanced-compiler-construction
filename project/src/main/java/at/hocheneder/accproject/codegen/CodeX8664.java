package at.hocheneder.accproject.codegen;

import at.hocheneder.accproject.symtab.Obj;
import at.hocheneder.accproject.symtab.SymbolTable;
import at.hocheneder.accproject.symtab.op.Operand;

import java.util.HashSet;
import java.util.Set;

public class CodeX8664 extends Code {
    public static final int RAX = 0;
    public static final int RBX = 3;
    public static final int RSP = 4;    //Stack pointer
    public static final int RBP = 5;    //Base pointer
    public static final int RDI = 7;
    public static final int R12 = 12;

    public static final int NO_REG = -1;

    private final Set<Integer> usedRegisters = new HashSet<>();

    private final SymbolTable tab;

    public CodeX8664(SymbolTable tab) {
        this.tab = tab;
    }


    // Operands
    Operand varOperand(Obj obj) {
        //kind = var varpar
        Operand op = new Operand();
        op.type = obj.type;
        if (obj.level == 0) { //global
            op.kind = Operand.Kind.Abs;
            op.adr = obj.adr;
            op.reg = NO_REG;
            op.inx = NO_REG;
        }
        else { //local
            op.kind = Operand.Kind.RegRel;
            op.reg = base(tab.getCurLevel() - obj.level);
            op.adr = obj.adr;
            op.inx = NO_REG;
        }
        return op;
    }

    private int base(int d) {
        if (d == 0)
            return RBP;
        Operand sl = new Operand(); //follow static/lexical link
        sl.kind = Operand.Kind.RegRel;
        sl.reg = RBP;
        sl.adr = 8;
        sl.inx = NO_REG;
        sl.type = SymbolTable.intType;

        while (d > 0) {
            load(sl);
            sl.kind = Operand.Kind.RegRel;
            sl.adr = 8;
            d--;
        }

        return sl.reg;
    }

    private Operand conOperand(int val) {
        return new Operand(val);
    }

    private Operand conOperand(char val) {
        return new Operand(val);
    }

    /**
     *
     * @param reg - NO_REG meany any
     * @return
     */
    private Operand regOperand(int reg) {
        Operand x = new Operand();
        x.kind = Operand.Kind.Reg;
        x.type = SymbolTable.intType;
        x.inx = NO_REG;
        if (reg == NO_REG) {
            x.reg = getRegister();
        }
        else {
            x.reg = getRegister(reg);
        }
        return x;
    }

    private Operand regRelOperand(int reg, int adr) {
        Operand x = new Operand();
        x.kind = Operand.Kind.RegRel;
        x.reg = reg;
        x.adr = adr;
        x.inx = NO_REG;
        return x;
    }

    private void freeOperand(Operand op) {
        if (op.kind == Operand.Kind.Reg || op.kind == Operand.Kind.RegRel) {
            freeRegister(op.reg);
            op.reg = NO_REG;
        }
        if (op.inx != NO_REG) {
            freeRegister(op.inx);
            op.inx = NO_REG;
        }
    }

    //Value loading
    public void load(Operand op) {
        //TODO
    }

    // Register management
    public int getRegister() {
        int r = NO_REG;
        if (!usedRegisters.contains(RAX)) { r = RAX; }
        else if (!usedRegisters.contains(RBX)) { r = RBX; }
        else if (!usedRegisters.contains(RDI)) { r = RDI; }
        else if (!usedRegisters.contains(R12)) { r = R12; }
        else {
            throw new RuntimeException("No registers free."); //FIXME
        }
        usedRegisters.add(r);
        return r;
    }

    public int getRegister(int r) {
        if (usedRegisters.contains(r)) {
            throw new RuntimeException("Register not free."); //FIXME
        }
        usedRegisters.add(r);
        return r;
    }

    public void freeRegister(int r) {
        usedRegisters.remove(r);
    }

    public boolean isRegisterFree(int r) {
        return !usedRegisters.contains(r);
    }

    public void freeAllRegisters() {
        usedRegisters.clear();
    }

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

    // Emit code
    void emitOpds(int reg, Operand x) {
        //----- put modrm byte
        int rm, mod;
        if (x.inx == NO_REG) {
            switch (x.kind) {
                case Operand.Kind.Abs:
                    mod = 0;
                    rm = 5;
                    break;
                case Operand.Kind.Reg:
                    mod = 3;
                    rm = x.reg;
                    break;
                case Operand.Kind.RegRel:
                    if (x.reg != RSP) {
                        mod = mod(x.adr);
                        rm = x.reg;
                    }
                    else {
                        throw new RuntimeException("RSP not allowed"); //TODO
                    }
                    break;
                default:
                    throw new RuntimeException("Operand must have inx");
            }
        } else { // indexed
            assert x.inx != RSP && (x.kind == Operand.Kind.Abs || x.kind == Operand.Kind.RegRel);
            rm = 4;
            if (x.kind == Operand.Kind.RegRel) {
                mod = mod(x.adr);
            } else { // x.kind == Opd.Abs
                mod = 0;
                x.reg = RBP;
            }
        }
        emit((byte) ((mod << 6) + (reg << 3) + rm));

        //----- put SIB byte
        if (x.inx != NO_REG) {
            emit((byte) ((x.scale << 6) + (x.inx << 3) + x.reg));
        }
        //----- put displacement
        if (mod == 0 && rm == 5) emitInstruction(x.adr); // absolute address
        else if (mod == 0 && rm == 4 && x.reg == RBP) emitInstruction(x.adr); // absolute indexed
        else if (mod == 1) emit((byte) x.adr);
        else if (mod == 2) emitInstruction(x.adr);

        //TODO assembly codegen
        //TODO refactor to use existing emit methods
    }

    public int mod(int n) {
        if (n == 0) return 0;
        else if (n >= -128 && n < 127) return 1;
        else return 2;
    }

	public void emitMoveImmediate(int register, byte imm) {
		assembly.append("mov ").append(registerName(register)).append(", ").append(imm).append("\n");

        int signExtended = imm; // Java sign-extends byte → int automatically
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

        byte rex    = (byte) (0x48
                        | (sourceReg >= 8 ? 0x04 : 0x00)   // REX.R
                        | (targetReg >= 8 ? 0x01 : 0x00));  // REX.B
        byte modRM  = (byte) (0xC0
                        | ((sourceReg & 0x7) << 3)
                        |  (targetReg & 0x7));

        emit(rex);
        emit((byte) 0x89);   // opcode: MOV r/m64, r64
        emit(modRM);
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