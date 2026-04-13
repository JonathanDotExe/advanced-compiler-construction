package codegen;

import static codegen.CodeX8664.RBP;
import static codegen.CodeX8664.RAX;
import static codegen.CodeX8664.RSP;
import static codegen.CodeX8664.RBX;
import static codegen.CodeX8664.RDI;
import static codegen.CodeX8664.R12;

public class ExampleHelloX8664 {
    private static void emitPutChar(CodeX8664 code, char ch, int callTargetReg) {
        code.emitMoveImmediate(RDI, (byte) ch);
        code.emitCallRegister(callTargetReg);
    }

    public static void main(String[] args) {   
        CodeX8664 code = new CodeX8664();

        code.emitPushRegister(RBP);
        code.emitMoveRegister(RBP, RSP);
        // push    rbx                     ; will hold Put  pointer
        code.emitPushRegister(RBX);

        // push    r12                     ; will hold PutLn pointer
        code.emitPushRegister(R12);

        // push    rax                     ; dummy push for alignment
        code.emitPushRegister(RAX);
    
        // Load the two function pointers from the struct pointed to by RDI
        // mov     rbx, [rdi]              ; rbx = Put
        code.emitMoveRegisterFromMemory(RBX, RDI, 0);

        // mov     r12, [rdi + 8]          ; r12 = PutLn
        code.emitMoveRegisterFromMemory(R12, RDI, 8);

        emitPutChar(code, 'H', RBX);
        emitPutChar(code, 'e', RBX);
        emitPutChar(code, 'l', RBX);
        emitPutChar(code, 'l', RBX);
        emitPutChar(code, 'o', RBX);
    
        // PutLn()
        code.emitCallRegister(R12);
    
        // Epilogue
        // pop     rax
        code.emitPopRegister(RAX);
        // pop     r12
        code.emitPopRegister(R12);
        // pop     rbx
        code.emitPopRegister(RBX);
        // pop     rbp
        code.emitPopRegister(RBP);
        // ret
        code.emitReturn();

        code.save("hello-x86_64.bin");
    }
}
