/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wickersoft.brainibrot;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;

/**
 *
 * @author Dennis
 */
public class Buddha4DAnimatedCached {

    public static final int width = 1920;
    public static final int height = 1080;
    public static final double[] xRange = {-2.75, 2.75};
    public static final double[] yRange = {-1.546875, 1.546875};
    public static final double scaleFactor = 500, gamma = 0.5;
    public static final int threads = 4;
    public static final double density = 8;
    public static final int cacheSize = 800000000;
    public static final int[] itsteps = {1000, 5000, 50000};
    // Dump the 2D (Not 4D) historgram for brightness adjustment using BuddhaRenderer
    public static final boolean dump = false, hud = false;

    // Set up unit vectors. Must be orthogonal for the diagram to make sense.
    // Varying the vectors' magnitudes results in magnification.
    public static double[] aX = {1, 0, 0, 0};
    public static double[] aY = {0, 1, 0, 0};
    public static double[] aZ = {0, 0, 1, 0};
    public static double[] aW = {0, 0, 0, 1};

    /*
     Mandelbrot:            Buddhabrot (Starting point and orbit planes swapped):
     1 0 0 0                0 0 1 0
     0 1 0 0                0 0 0 1
     0 0 1 0                1 0 0 0
     0 0 0 1                0 1 0 0
     */
    private static final int[] pixels = new int[3 * width * height];
    private static int f;
    private static final int avgSize = 4;
    private static final int[] brightest = new int[avgSize];
    private static byte[] maskBytes;
    private static final double restep = (xRange[1] - xRange[0]) / width;
    private static final double imstep = (yRange[1] - yRange[0]) / height;
    private static int points = 0;
    private static int frameEnd = 0;
    private static final HashMap<Integer, double[]> trace = new HashMap<>();
    private static double[] omega = {0, 0, 0, 0, 0, 0};
    private static double[] targetOmega = {0.01, 0.01, 0.01, 0.01, 0.01, 0.01};

    // Allocate oodles of memory for the point cache
    public static final float[] pointCache = new float[cacheSize];
    public static int[] cachePointers = new int[(int) (width * height * density * density) + 1];
    public static int cachePointerPointer = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.print("Loading trace..");
        File file = new File("trace.txt");
        FileInputStream fis = new FileInputStream(file);
        byte[] bin = new byte[(int) file.length()];
        fis.read(bin);
        String[] lines = new String(bin).split("\r\n");
        for (String line : lines) {
            String[] l = line.split(";");
            if (l.length == 1) {
                frameEnd = Integer.parseInt(l[0]);
            } else {
                int ind = Integer.parseInt(l[0]);
                double[] omega = {Double.parseDouble(l[1]), Double.parseDouble(l[2]), Double.parseDouble(l[3]), Double.parseDouble(l[4]), Double.parseDouble(l[5]), Double.parseDouble(l[6])};
                trace.put(ind, omega);
            }
        }
        System.out.println(trace.size() + " scenes loaded");
        System.out.print("Loading mask..");
        BufferedImage renderMask = ImageIO.read(new File("mask.png"));
        maskBytes = ((DataBufferByte) renderMask.getRaster().getDataBuffer()).getData();
        System.out.println("Done.");
        Renderer[] renderers = new Renderer[threads];
        long penis = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            renderers[i] = new Renderer(i);
            renderers[i].start();
        }
        for (int i = 0; i < threads; i++) {
            renderers[i].join();
        }
        System.out.println("Cache time: " + (System.currentTimeMillis() - penis));
        System.out.println("Cache Utilization: " + cachePointers[cachePointerPointer] + "(" + (Math.round(100.0 * cachePointers[cachePointerPointer] / (double) cacheSize)) + "%)");
        System.out.println("Points Drawn: " + cachePointerPointer + "(" + (Math.round(100.0 * cachePointerPointer / width / height / density / density)) + "%)");
        penis = System.currentTimeMillis();
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics gr = img.getGraphics();
        final int[] actualPixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        for (f = 0; f < frameEnd; f++) {
            brightest[f % avgSize] = 0;
            if (!(new File("frames/Buddha" + f + ".jpg").exists())) {
                double[] dX = MathUtil.parametrize(new double[]{1, 0, 0, 0}, aX, aY, aZ, aW);
                double[] dY = MathUtil.parametrize(new double[]{0, 1, 0, 0}, aX, aY, aZ, aW);
                double[] dZ = MathUtil.parametrize(new double[]{0, 0, 1, 0}, aX, aY, aZ, aW);
                double[] dW = MathUtil.parametrize(new double[]{0, 0, 0, 1}, aX, aY, aZ, aW);
                System.out.println("Differential vectors: X={" + dX[0] + "," + dX[1] + "} Y={" + dY[0] + "," + dY[1] + "} Z={" + dZ[0] + "," + dZ[1] + "} W={" + dW[0] + "," + dW[1] + "}");
                System.out.println("Dot products: " + MathUtil.dotProduct(aX, aY) + " " + MathUtil.dotProduct(aX, aZ) + " " + MathUtil.dotProduct(aX, aW) + " " + MathUtil.dotProduct(aY, aZ) + " " + MathUtil.dotProduct(aY, aW) + " " + MathUtil.dotProduct(aZ, aW));
                double[] screenCoordinates = {0, 0};
                int x, y, j, k;
                for (int i = 0; i < cachePointers.length - 1; i++) {
                    j = cachePointers[i];
                    screenCoordinates[0] = pointCache[j] * dX[0] + pointCache[j + 1] * dY[0];
                    screenCoordinates[1] = pointCache[j] * dX[1] + pointCache[j + 1] * dY[1];
                    for (j = 5; j < (cachePointers[i + 1] - cachePointers[i]) / 2; j++) {
                        k = cachePointers[i] + 2 * j;
                        x = (int) ((screenCoordinates[0] + pointCache[k] * dZ[0] + pointCache[k + 1] * dW[0] - xRange[0]) / restep);
                        y = (int) ((screenCoordinates[1] + pointCache[k] * dZ[1] + pointCache[k + 1] * dW[1] - yRange[0]) / imstep);
                        if (x >= 0 && x < width && y >= 0 && y < height) {
                            if (j < 2 * itsteps[0]) {
                                if (pixels[3 * (width * y + x) + 2]++ > brightest[f % avgSize]) {
                                    brightest[f % avgSize]++;
                                }
                            }
                            if (j < 2 * itsteps[1]) {
                                if (pixels[3 * (width * y + x) + 1]++ > brightest[f % avgSize]) {
                                    brightest[f % avgSize]++;
                                }
                            }
                            if (pixels[3 * (width * y + x)]++ > brightest[f % avgSize]) {
                                brightest[f % avgSize]++;
                            }
                        }
                    }
                }
                if (f == 0) {
                    for (int i = 1; i < avgSize; i++) {
                        brightest[i] = brightest[0];
                    }
                }
                int sum = 0;
                for (int t : brightest) {
                    sum += t;
                }
                System.out.println("Render time: " + (System.currentTimeMillis() - penis));
                System.out.println("Brightest: " + brightest[f % avgSize] + " / " + (sum / avgSize));
                double base = Math.pow(sum / avgSize, gamma);
                double scale = scaleFactor / base;
                penis = System.currentTimeMillis();
                int r, g, b, p;
                for (x = 0; x < width; x++) {
                    for (y = 0; y < height; y++) {
                        p = 3 * (width * y + x);
                        //if ((r = (int) (pixels[p] * scale)) > 255) {
                        if ((r = (int) (Math.pow(pixels[p], gamma) * scale)) > 255) {
                            r = 255;
                        }
                        //if ((g = (int) (pixels[p + 1] * scale)) > 255) {
                        if ((g = (int) (Math.pow(pixels[p + 1], gamma) * scale)) > 255) {
                            g = 255;
                        }
                        //if ((b = (int) (pixels[p + 2] * scale)) > 255) {
                        if ((b = (int) (Math.pow(pixels[p + 2], gamma) * scale)) > 255) {
                            b = 255;
                        }
                        actualPixels[width * y + x] = (r << 16) + (g << 8) + b;
                        pixels[p] = 0;
                        pixels[p + 1] = 0;
                        pixels[p + 2] = 0;
                    }
                }
                if (hud) {
                    gr.setColor(Color.white);
                    gr.setFont(new Font("Arial", Font.PLAIN, 60));
                    gr.drawString("Screen Plane:", 1240, 70);

                    gr.setFont(new Font("Arial", Font.PLAIN, 42));

                    gr.fillPolygon(new int[]{
                        1600, 1600, 1590, 1607, 1624, 1614, 1614
                    }, new int[]{
                        410, 300, 300, 180, 300, 300, 410
                    }, 7);

                    gr.fillPolygon(new int[]{
                        1600, 1720, 1720, 1840, 1720, 1720, 1600
                    }, new int[]{
                        410, 410, 403, 419, 428, 421, 421
                    }, 7);

                    gr.drawString("("
                            + (Math.round(aY[0] * 100.0) / 100.0) + "  "
                            + (Math.round(aY[1] * 100.0) / 100.0) + "  "
                            + (Math.round(aY[2] * 100.0) / 100.0) + "  "
                            + (Math.round(aY[3] * 100.0) / 100.0) + ")", 1416, 150);
                    gr.drawString("("
                            + (Math.round(aX[0] * 100.0) / 100.0) + "  "
                            + (Math.round(aX[1] * 100.0) / 100.0) + "  "
                            + (Math.round(aX[2] * 100.0) / 100.0) + "  "
                            + (Math.round(aX[3] * 100.0) / 100.0) + ")", 1416, 475);

                    gr.setFont(new Font("Arial", Font.PLAIN, 60));
                    gr.drawString("Normal Plane:", 1240, 610);

                    gr.setFont(new Font("Arial", Font.PLAIN, 42));

                    gr.fillPolygon(new int[]{
                        1600, 1600, 1590, 1607, 1624, 1614, 1614
                    }, new int[]{
                        950, 840, 840, 720, 840, 840, 950
                    }, 7);

                    gr.fillPolygon(new int[]{
                        1600, 1720, 1720, 1840, 1720, 1720, 1600
                    }, new int[]{
                        950, 950, 943, 959, 968, 961, 961
                    }, 7);

                    gr.drawString("("
                            + (Math.round(aW[0] * 100.0) / 100.0) + "  "
                            + (Math.round(aW[1] * 100.0) / 100.0) + "  "
                            + (Math.round(aW[2] * 100.0) / 100.0) + "  "
                            + (Math.round(aW[3] * 100.0) / 100.0) + ")", 1416, 690);
                    gr.drawString("("
                            + (Math.round(aZ[0] * 100.0) / 100.0) + "  "
                            + (Math.round(aZ[1] * 100.0) / 100.0) + "  "
                            + (Math.round(aZ[2] * 100.0) / 100.0) + "  "
                            + (Math.round(aZ[3] * 100.0) / 100.0) + ")", 1416, 1015);

                }
                System.out.println(f + ": " + (System.currentTimeMillis() - penis) + "ms");
                ImageIO.write(img, "JPG", new File("frames/Buddha" + f + ".jpg"));
            }
            if (trace.containsKey(f)) {
                targetOmega = trace.get(f);
            }
            omega[0] += (targetOmega[0] - omega[0]) / 8;
            omega[1] += (targetOmega[1] - omega[1]) / 8;
            omega[2] += (targetOmega[2] - omega[2]) / 8;
            omega[3] += (targetOmega[3] - omega[3]) / 8;
            omega[4] += (targetOmega[4] - omega[4]) / 8;
            omega[5] += (targetOmega[5] - omega[5]) / 8;

            aX = MathUtil.rotateXY(aX, omega[0]);
            aY = MathUtil.rotateXY(aY, omega[0]);
            aZ = MathUtil.rotateXY(aZ, omega[0]);
            aW = MathUtil.rotateXY(aW, omega[0]);

            aX = MathUtil.rotateXZ(aX, omega[1]);
            aY = MathUtil.rotateXZ(aY, omega[1]);
            aZ = MathUtil.rotateXZ(aZ, omega[1]);
            aW = MathUtil.rotateXZ(aW, omega[1]);

            aX = MathUtil.rotateXW(aX, omega[2]);
            aY = MathUtil.rotateXW(aY, omega[2]);
            aZ = MathUtil.rotateXW(aZ, omega[2]);
            aW = MathUtil.rotateXW(aW, omega[2]);

            aX = MathUtil.rotateYZ(aX, omega[3]);
            aY = MathUtil.rotateYZ(aY, omega[3]);
            aZ = MathUtil.rotateYZ(aZ, omega[3]);
            aW = MathUtil.rotateYZ(aW, omega[3]);

            aX = MathUtil.rotateYW(aX, omega[4]);
            aY = MathUtil.rotateYW(aY, omega[4]);
            aZ = MathUtil.rotateYW(aZ, omega[4]);
            aW = MathUtil.rotateYW(aW, omega[4]);

            aX = MathUtil.rotateZW(aX, omega[5]);
            aY = MathUtil.rotateZW(aY, omega[5]);
            aZ = MathUtil.rotateZW(aZ, omega[5]);
            aW = MathUtil.rotateZW(aW, omega[5]);
        }
    }

    public static boolean shouldDraw(double x, double y) {
        int iX = (int) ((x + 2.0) / 2.5 * 19200);
        int iY = (int) ((y + 1.0) / 2.0 * 15360);
        return maskBytes[19200 * iY + iX] != 0;
    }

    public static synchronized void cachePoints(float[] fTrajectory, int its) {
        int q = cachePointers[cachePointerPointer];
        if (q + 2 * its > pointCache.length) {
            //System.err.println("Cache Overflow!");
            return;
        }
        for (int i = 0; i < its; i++) {
            pointCache[q++] = fTrajectory[2 * i];
            pointCache[q++] = fTrajectory[2 * i + 1];
        }
        cachePointers[++cachePointerPointer] = q;
    }

    public static class Renderer extends Thread {

        private final int offset;
        private final float[] fTrajectory = new float[2 * itsteps[2]];

        public Renderer(int offset) {
            this.offset = offset;
        }

        public void run() {
            int q = offset;
            int i;
            for (double re = -2 + restep / density * offset; re < 0.5; re += threads * restep / density) {
                for (double im = -1; im < 1; im += imstep / density) {
                    if (!shouldDraw(re, im)) {
                        continue;
                    }
                    double[] z = {re, im};
                    double[] c = {re, im};
                    double[] t = {0, 0};
                    for (i = 0; i < itsteps[2] && z[0] * z[0] + z[1] * z[1] < 16; i++) {
                        fTrajectory[2 * i] = (float) z[0];
                        fTrajectory[2 * i + 1] = (float) z[1];
                        t[0] = z[0] * z[0] - z[1] * z[1] + c[0];
                        t[1] = 2 * z[0] * z[1] + c[1];
                        z[0] = t[0];
                        z[1] = t[1];
                    }
                    if (i < itsteps[2]) {
                        Buddha4DAnimatedCached.cachePoints(fTrajectory, i);
                    }
                }
                if ((q += threads) % 25 == 0) {
                    System.out.println("Thread " + offset + ": " + q);
                }
            }
            System.out.println("Thread " + offset + " finished");
        }
    }
    
    
}
