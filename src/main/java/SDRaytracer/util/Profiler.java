package SDRaytracer.util;

import SDRaytracer.SDRaytracer;

public class Profiler {
    public void profileRenderImage(SDRaytracer sdr) {
        long end, start, time;

        sdr.renderImage(); // initialisiere Datenstrukturen, erster Lauf verfälscht sonst Messungen

        for (int procs = 1; procs < 6; procs++) {
            sdr.maxRec = procs - 1;
            System.out.print(procs);
            for (int i = 0; i < 10; i++) {
                start = System.currentTimeMillis();

                sdr.renderImage();

                end = System.currentTimeMillis();
                time = end - start;
                System.out.print(";" + time);
            }
            System.out.println("");
        }
    }
}
