package SDRaytracer;

import SDRaytracer.datatypes.*;
import SDRaytracer.util.Profiler;
import SDRaytracer.scenes.Scenes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/* Implementation of a very simple Raytracer
   Stephan Diehl, Universität Trier, 2010-2016
*/

public class SDRaytracer extends JFrame {
    private static final long serialVersionUID = 1L;
    int width = 1000;
    int height = 1000;

    private Future[] futureList = new Future[width];
    private int nrOfProcessors = Runtime.getRuntime().availableProcessors();
    private ExecutorService eservice = Executors.newFixedThreadPool(nrOfProcessors);

    public int maxRec = 3;
    int rayPerPixel = 1;
    int startX, startY, startZ;

    private List<Triangle> triangles;

    private Light mainLight = new Light(new Vec3D(0, 100, 0), new RGB(0.1f, 0.1f, 0.1f));

    private Light lights[] = new Light[]{mainLight
            , new Light(new Vec3D(100, 200, 300), new RGB(0.5f, 0, 0.0f))
            , new Light(new Vec3D(-100, 200, 300), new RGB(0.0f, 0, 0.5f))
            //,new sdraytracer.datatypes.Light(new sdraytracer.datatypes.Vec3D(-100,0,0), new sdraytracer.datatypes.RGB(0.0f,0.8f,0.0f))
    };

    RGB[][] image = new RGB[width][height];

    private float fovx = (float) 0.628;
    private float fovy = (float) 0.628;
    private RGB ambient_color = new RGB(0.01f, 0.01f, 0.01f);
    private RGB black = new RGB(0.0f, 0.0f, 0.0f);
    private int y_angle_factor = 4, x_angle_factor = -4;

    public static void main(String argv[]) {
        long start = System.currentTimeMillis();
        SDRaytracer sdr = new SDRaytracer();
        long end = System.currentTimeMillis();
        long time = end - start;
        System.out.println("time: " + time + " ms");
        System.out.println("nrprocs=" + sdr.nrOfProcessors);
    }


    private SDRaytracer() {
        boolean profiling = false;

        createScene();

        if (!profiling) renderImage();
        else {
            Profiler profiler = new Profiler();
            profiler.profileRenderImage(this);
        }

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        JPanel area = new JPanel() {
            public void paint(Graphics g) {
                System.out.println("fovx=" + fovx + ", fovy=" + fovy + ", xangle=" + x_angle_factor + ", yangle=" + y_angle_factor);
                if (image == null) return;
                for (int i = 0; i < width; i++)
                    for (int j = 0; j < height; j++) {
                        g.setColor(image[i][j].color());
                        // zeichne einzelnen Pixel
                        g.drawLine(i, height - j, i, height - j);
                    }
            }
        };

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                boolean redraw = false;
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    x_angle_factor--;
                    //mainLight.position.y-=10;
                    //fovx=fovx+0.1f;
                    //fovy=fovx;
                    //maxRec--; if (maxRec<0) maxRec=0;
                    redraw = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    x_angle_factor++;
                    //mainLight.position.y+=10;
                    //fovx=fovx-0.1f;
                    //fovy=fovx;
                    //maxRec++;if (maxRec>10) return;
                    redraw = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    y_angle_factor--;
                    //mainLight.position.x-=10;
                    //startX-=10;
                    //fovx=fovx+0.1f;
                    //fovy=fovx;
                    redraw = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    y_angle_factor++;
                    //mainLight.position.x+=10;
                    //startX+=10;
                    //fovx=fovx-0.1f;
                    //fovy=fovx;
                    redraw = true;
                }
                if (redraw) {
                    createScene();
                    renderImage();
                    repaint();
                }
            }
        });

        area.setPreferredSize(new Dimension(width, height));
        contentPane.add(area);
        this.pack();
        this.setVisible(true);
    }

    Ray eye_ray = new Ray();
    double tan_fovx;
    double tan_fovy;

    public void renderImage() {
        tan_fovx = Math.tan(fovx);
        tan_fovy = Math.tan(fovy);
        for (int i = 0; i < width; i++) {
            futureList[i] = (Future) eservice.submit(new RaytraceTask(this, i));
        }

        for (int i = 0; i < width; i++) {
            try {
                RGB[] col = (RGB[]) futureList[i].get();
                for (int j = 0; j < height; j++)
                    image[i][j] = col[j];
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            }
        }
    }


    RGB rayTrace(Ray ray, int rec) {
        if (rec > maxRec) return black;
        IPoint ip = hitObject(ray);  // (ray, p, n, triangle);
        if (ip.dist > IPoint.epsilon)
            return lighting(ray, ip, rec);
        else
            return black;
    }


    private IPoint hitObject(Ray ray) {
        IPoint isect = new IPoint(null, null, -1);
        float idist = -1;
        for (Triangle t : triangles) {
            IPoint ip = ray.intersect(t);
            if (ip.dist != -1)
                if ((idist == -1) || (ip.dist < idist)) { // save that intersection
                    idist = ip.dist;
                    isect.ipoint = ip.ipoint;
                    isect.dist = ip.dist;
                    isect.triangle = t;
                }
        }
        return isect;  // return intersection point and normal
    }


    RGB addColors(RGB c1, RGB c2, float ratio) {
        return new RGB((c1.red + c2.red * ratio),
                (c1.green + c2.green * ratio),
                (c1.blue + c2.blue * ratio));
    }

    private RGB lighting(Ray ray, IPoint ip, int rec) {
        Vec3D point = ip.ipoint;
        Triangle triangle = ip.triangle;
        RGB color = addColors(triangle.color, ambient_color, 1);
        Ray shadow_ray = new Ray();
        for (Light light : lights) {
            shadow_ray.start = point;
            shadow_ray.dir = light.position.minus(point).mult(-1);
            shadow_ray.dir.normalize();
            IPoint ip2 = hitObject(shadow_ray);
            if (ip2.dist < IPoint.epsilon) {
                float ratio = Math.max(0, shadow_ray.dir.dot(triangle.normal));
                color = addColors(color, light.color, ratio);
            }
        }
        Ray reflection = new Ray();
        //R = 2N(N*L)-L)    L ausgehender Vektor
        Vec3D L = ray.dir.mult(-1);
        reflection.start = point;
        reflection.dir = triangle.normal.mult(2 * triangle.normal.dot(L)).minus(L);
        reflection.dir.normalize();
        RGB rcolor = rayTrace(reflection, rec + 1);
        float ratio = (float) Math.pow(Math.max(0, reflection.dir.dot(L)), triangle.shininess);
        color = addColors(color, rcolor, ratio);
        return (color);
    }

    private void createScene() {
        triangles = new ArrayList<Triangle>();
        Scenes.createSceneOne(triangles, y_angle_factor, x_angle_factor);
    }

    static class RaytraceTask implements Callable {
        SDRaytracer tracer;
        int i;

        RaytraceTask(SDRaytracer t, int ii) {
            tracer = t;
            i = ii;
        }

        public RGB[] call() {
            RGB[] col = new RGB[tracer.height];
            for (int j = 0; j < tracer.height; j++) {
                tracer.image[i][j] = new RGB(0, 0, 0);
                for (int k = 0; k < tracer.rayPerPixel; k++) {
                    double di = i + (Math.random() / 2 - 0.25);
                    double dj = j + (Math.random() / 2 - 0.25);
                    if (tracer.rayPerPixel == 1) {
                        di = i;
                        dj = j;
                    }
                    Ray eye_ray = new Ray();
                    eye_ray.setStart(tracer.startX, tracer.startY, tracer.startZ);   // ro
                    eye_ray.setDir((float) (((0.5 + di) * tracer.tan_fovx * 2.0) / tracer.width - tracer.tan_fovx),
                            (float) (((0.5 + dj) * tracer.tan_fovy * 2.0) / tracer.height - tracer.tan_fovy),
                            (float) 1f);    // rd
                    eye_ray.normalize();
                    col[j] = tracer.addColors(tracer.image[i][j], tracer.rayTrace(eye_ray, 0), 1.0f / tracer.rayPerPixel);
                }
            }
            return col;
        }
    }
}