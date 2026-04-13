package codegen;

public class ExampleHelloAarch64 {

    private static void emitPutChar(CodeAarch64 code, char ch, int callTargetReg, int putStackOffset) {
        code.emitLoadRegister(callTargetReg, CodeAarch64.SP, putStackOffset);
        code.emitMoveImmediate(0, (byte) ch);
        code.emitBranchWithLinkToRegister(callTargetReg);
    }

    public static void main(String[] args) {
        CodeAarch64 code = new CodeAarch64();

        // stp     x29, x30, [sp, #-48]! ; save the old frame pointer and return address, and before doing that update sp (the !)
        code.emitStorePairOfRegisterPreIndex(29, 30, CodeAarch64.SP, -48);

        // mov     x29, sp
        code.emitMove(29, CodeAarch64.SP);

        // load the addresses of Put and PutLn into registers
        code.emitLoadRegister(9, 0, 0); // Load Put into x9
        code.emitLoadRegister(10, 0, 8); // Load PutLn into x10

        // store the addresses of Put and PutLn on the stack so that they can be used after calls
        code.emitStoreRegister(9, CodeAarch64.SP, 16);
        code.emitStoreRegister(10, CodeAarch64.SP, 24);

        emitPutChar(code, 'H', 9, 16);
        emitPutChar(code, 'e', 9, 16);
        emitPutChar(code, 'l', 9, 16);
        emitPutChar(code, 'l', 9, 16);
        emitPutChar(code, 'o', 9, 16);

        code.emitLoadRegister(9, CodeAarch64.SP, 24); // Load PutLn into x9
        code.emitBranchWithLinkToRegister(9); // Call PutLn

        code.emitMoveImmediate(0, (byte) 0); // Return 0 from main

        code.emitLoadPairOfRegisterPostIndexed(29, 30, CodeAarch64.SP, 48);

        code.emitReturn();

        code.save("hello-aarch64.bin");
    }
}
