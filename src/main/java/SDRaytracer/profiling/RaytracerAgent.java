package SDRaytracer.profiling;

import java.lang.instrument.Instrumentation;

public class RaytracerAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new RaytracerTransformer());
    }

}
