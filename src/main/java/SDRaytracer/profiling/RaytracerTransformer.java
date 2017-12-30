package SDRaytracer.profiling;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class RaytracerTransformer implements ClassFileTransformer {

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if ("edu/unitrier/st/teaching/instrumentation/test/SDRaytracer".equals(className)) {
            try {
                ClassReader classReader = new ClassReader(classfileBuffer);
                ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                ProfilingClassVisitor profilingClassVisitor = new ProfilingClassVisitor(classWriter);
                classReader.accept(profilingClassVisitor, ClassReader.EXPAND_FRAMES);
                return classWriter.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
                return classfileBuffer;
            }
        }
        return classfileBuffer;
    }

}
