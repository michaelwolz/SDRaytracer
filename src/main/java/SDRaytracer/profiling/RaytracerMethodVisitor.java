package SDRaytracer.profiling;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class RaytracerMethodVisitor extends AdviceAdapter implements Opcodes {

    RaytracerMethodVisitor (int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("CALL " + name);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void onMethodEnter() {
    }

    @Override
    public void onMethodExit(int opcode) {
    }
}
