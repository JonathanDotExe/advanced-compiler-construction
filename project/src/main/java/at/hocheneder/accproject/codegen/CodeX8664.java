package at.hocheneder.accproject.codegen;

import at.hocheneder.accproject.symtab.Obj;
import at.hocheneder.accproject.symtab.SymbolTable;
import at.hocheneder.accproject.symtab.op.MathOp;
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


    public static final int ADD = 0x00;
    public static final int ADC = 0x10;
    public static final int SUB = 0x28;
    public static final int SBB = 0x18;
    public static final int AND = 0x20;
    public static final int OR = 0x08;
    public static final int XOR = 0x30;
    public static final int CMP = 0x38;


    private final Set<Integer> usedRegisters = new HashSet<>();

    public final SymbolTable tab;

    public CodeX8664(SymbolTable tab) {
        this.tab = tab;
    }


    // Operands
    public Operand varOperand(Obj obj) {
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
    public Operand regOperand(int reg) {
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

    public Operand regRelOperand(int reg, int adr) {
        Operand x = new Operand();
        x.kind = Operand.Kind.RegRel;
        x.reg = reg;
        x.adr = adr;
        x.inx = NO_REG;
        return x;
    }

    public void freeOperand(Operand op) {
        if (op.kind == Operand.Kind.Reg || op.kind == Operand.Kind.RegRel) {
            freeRegister(op.reg);
            op.reg = NO_REG;
        }
        if (op.inx != NO_REG) {
            freeRegister(op.inx);
            op.inx = NO_REG;
        }
    }


    // Register management
    public int getRegister() {
        int r = NO_REG;
        //if (!usedRegisters.contains(RAX)) { r = RAX; }
        if (!usedRegisters.contains(RBX)) { r = RBX; }
        else if (!usedRegisters.contains(RDI)) { r = RDI; }
        else if (!usedRegisters.contains(R12)) { r = R12; }
        else {
            throw new RuntimeException("No registers free."); //FIXME
        }
        System.out.println("Assign register " + registerName(r));


        usedRegisters.add(r);
        return r;
    }

    public int getRegister(int r) {
        if (usedRegisters.contains(r)) {
            throw new RuntimeException("Register not free."); //FIXME
        }
        System.out.println("Use register " + registerName(r));
        usedRegisters.add(r);
        return r;
    }

    public void freeRegister(int r) {
        if (r != NO_REG) {
            System.out.println("Free register " + registerName(r));
        }
        usedRegisters.remove(r);
    }

    public boolean isRegisterFree(int r) {
        return !usedRegisters.contains(r);
    }

    public void freeAllRegisters() {
        System.out.println("Free all registers");
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
    void emitOperands(int reg, Operand x) {

        //----- emit modrm byte
        int rm, mod;
        if (x.inx == NO_REG) {
            switch (x.kind) {
                case Abs:
                    mod = 0;
                    rm = 5;
                    break;
                case Reg:
                    mod = 3;
                    rm = x.reg;
                    break;
                case RegRel:
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
            } else { // x.kind == Operand.Abs
                mod = 0;
                x.reg = RBP;
            }
        }
        emit((byte) ((mod << 6) + (reg << 3) + rm));

        //----- emit SIB byte
        if (x.inx != NO_REG) {
            emit((byte) ((x.scale << 6) + (x.inx << 3) + x.reg));
        }
        //----- emit displacement
        if (mod == 0 && rm == 5) emitInstruction(x.adr); // absolute address
        else if (mod == 0 && rm == 4 && x.reg == RBP) emitInstruction(x.adr); // absolute indexed
        else if (mod == 1) emit((byte) x.adr);
        else if (mod == 2) emitInstruction(x.adr);

        //TODO assembly codegen
        //TODO refactor to use existing emit methods
    }

    public void emitMOV(Operand x, Operand y) { // MOV x, y
        assert x.type.size() == y.type.size();
        int sizeFlag = emitPrefix(x);
        if (y.kind == Operand.Kind.Con) {
            if (x.kind == Operand.Kind.Reg) { // r := imm
                emitInstruction(0xB0 + (sizeFlag << 3) + x.reg);
                emitConst(y.type.size(), y.val);
            } else { // m := imm
                emitInstruction(0xC6 + sizeFlag);
                emitOperands(0, x);
                emitConst(y.type.size(), y.val);
            }
        } else if (x.kind == Operand.Kind.Reg) { // r := rm
            emit(0x8A + sizeFlag);
            emitOperands(x.reg, y);
        } else if (y.kind == Operand.Kind.Reg) { // rm := r
            emit(0x88 + sizeFlag);
            emitOperands(y.reg, x);
        } else {
            throw new RuntimeException(); //FIXME
        };
    }

    public int emitPrefix (Operand x) {
        if (x.type.size() == 1) return 0;
        else if (x.type.size() == 2) { emit(0x66); return 1; }
        else if (x.type.size() == 4) return 1;
        else {
            throw new RuntimeException(); //FIXME
        }
    }

    public void emitDyadic (int op, Operand x, Operand y) {
        int sf = emitPrefix(x);
        if (x.kind == Operand.Kind.Reg && x.reg == RAX && y.kind == Operand.Kind.Con && x.type.size() == y.type.size()) {
        // EAX := EAX op imm
            emit(op + 4 + sf); emitConst(y.type.size(), y.val);
        } else if (y.kind == Operand.Kind.Con) {
            if (x.type.size() > 1 && -128 <= y.val && y.val <= 127) { // rm := rm op signextend(imm8)
                emit(0x82 + sf); emitOperands(op/8, x); emitConst(1, y.val);
            } else if (x.type.size() == y.type.size()) { // rm := rm op imm
                emit(0x80 + sf); emitOperands(op/8, x); emitConst(y.type.size(), y.val);
            } else {
                throw new RuntimeException(); //FIXME
            }
        } else if (x.kind == Operand.Kind.Reg) { // r := r op rm
            emit(op + 2 + sf);
            emitOperands(x.reg, y);
        } else if (y.kind == Operand.Kind.Reg) { // rm := rm op r
            emit(op + sf);
            emitOperands(y.reg, x);
        } else {
            throw new RuntimeException(); //FIXME
        }
        freeOperand(y);
    }

    public void load (Operand x) {
        Operand r = null; // destination register
        switch (x.kind) {
            case Reg:
                return;
            case RegRel:
                if (x.reg == RBP)
                    r = regOperand(NO_REG);
                else
                    r = regOperand(x.reg);
                r.type = x.type;
                emitMOV(r, x);
                freeRegister(x.inx);
                break;
            case Con:
            case Abs:
                r = regOperand(NO_REG); r.type = x.type;
                emitMOV(r, x);
                freeRegister(x.inx);
                break;
            default:
                throw new RuntimeException(); //FIXME
        }
        x.kind = Operand.Kind.Reg;
        x.reg = r.reg;
        x.adr = 0;
        x.inx = NO_REG;
    }

    public void genOp (MathOp op, Operand x, Operand y) {
        if (x.kind == Operand.Kind.Con && y.kind == Operand.Kind.Con) {
            switch(op) {
                case Plus: x.val += y.val; break;
                case Minus: x.val -= y.val; break;
                case Times: x.val *= y.val; break;
                case Divide: x.val /= y.val; break;
                case Modulo: x.val /= y.val; break;
            }
        } else {
            load(x);
            switch(op) {
                case Plus: emitDyadic(ADD, x, y); break;
                case Minus: emitDyadic(SUB, x, y); break;
                default:
                    throw new RuntimeException("Not yet implemented"); //FIXME
            }
        }
    }

    /*public void loadAdr(Operand x) {
        if (x.kind == Operand.Kind.RegRel && x.adr == 0 && x.inx == NO_REG) {
            x.kind = Operand.Kind.Reg;
        } else if (x.kind == Operand.Kind.RegRel || x.kind == Operand.Kind.Abs) {
            Operand r;
            if (x.kind == Operand.Kind.Abs || x.reg == RBP)
                r = regOperand(NO_REG);
            else // x.kind == Operand.RegRel && x.reg != EBP
                r = regOperand(x.reg);
            emitLEA(r, x);
            freeRegister(x.inx);
            x.kind = Operand.Kind.Reg;
            x.reg = r.reg;
            x.adr = 0;
            x.inx = NO_REG;
        } else {
            throw new RuntimeException(); //FIXME
        }
    }

    public void emitLEA(Operand x, Operand y) { // MOV x, y
        assert x.type.size() == y.type.size();
        int sizeFlag = emitPrefix(x);
        if (y.kind == Operand.Kind.Con) {
            if (x.kind == Operand.Kind.Reg) { // r := imm
                emitInstruction(0xB0 + (sizeFlag << 3) + x.reg);
                emitConst(y.type.size(), y.val);
            } else { // m := imm
                emitInstruction(0xC6 + sizeFlag);
                emitOperands(0, x);
                emitConst(y.type.size(), y.val);
            }
        } else if (x.kind == Operand.Kind.Reg) { // r := rm
            emit(0x8A + sizeFlag);
            emitOperands(x.reg, y);
        } else if (y.kind == Operand.Kind.Reg) { // rm := r
            emit(0x88 + sizeFlag);
            emitOperands(y.reg, x);
        } else {
            throw new RuntimeException(); //FIXME
        };
    }*/

    public int mod(int n) {
        if (n == 0) return 0;
        else if (n >= -128 && n < 127) return 1;
        else return 2;
    }

    public void putNEG(Operand x) {
        //TODO

    }

	/*public void emitMoveImmediate(int register, byte imm) {
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
    }*/
}