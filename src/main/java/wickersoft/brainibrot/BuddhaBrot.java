/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wickersoft.brainibrot;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Dennis
 */
public class BuddhaBrot {

    /**
     * @param args the command line arguments
     */
    public static final int width = 1920;
    public static final int height = 1080;
    public static final int iterations = 50000;
    public static final int[] itsteps = {1000, 5000, 50000};
    public static final int threads = 4;
    public static final double density = 8;
    public static final double gammaI = 1.2;
    public static final double gammaC = 0;
    public static final boolean dump = true;

    public static final int[] pixels;
    public static final float[][] palette;
    private static byte[] maskBytes;
    public static final double restep = 2.5 / width;
    public static final double imstep = 2.0 / height;
    public static int[] brightest = new int[3];
    public static int points = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
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
        System.out.println("Render time: " + (System.currentTimeMillis() - penis));
        if (dump) {
            File file = new File("Buddha.raw");
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            int p;
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    p = 3 * (width * j + i);
                    dos.writeFloat(pixels[p]);
                    dos.writeFloat(pixels[p + 1]);
                    dos.writeFloat(pixels[p + 2]);
                }
            }
            dos.flush();
            dos.close();
        }
        System.out.println("Brightest: " + brightest[0]);
        System.out.println("Points: " + points);
        double scale = 1000.0 / brightest[0];
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] actualPixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        penis = System.currentTimeMillis();
        int r, g, b, p;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                p = 3 * (width * y + x);
                if ((r = (int) (pixels[p] * scale)) > 255) {
                    r = 255;
                }
                if ((g = (int) (pixels[p + 1] * scale)) > 255) {
                    g = 255;
                }
                if ((b = (int) (pixels[p + 2] * scale)) > 255) {
                    b = 255;
                }
                actualPixels[width * y + x] = (r << 16) + (g << 8) + b;
            }
        }
        System.out.println("Draw time: " + (System.currentTimeMillis() - penis));
        ImageIO.write(img, "PNG", new File("Buddha.png"));
    }

    public static boolean shouldDraw(double x, double y) {
        int iX = (int) ((x + 2.0) / 2.5 * 19200);
        int iY = (int) ((y + 1.0) / 2.0 * 15360);
        return maskBytes[19200 * iY + iX] != 0;
    }

    public static synchronized void draw(int[][] trajectory, int its) {
        int p;
        for (int j = 0; j < its; j++) {
            p = 3 * (trajectory[j][0] + width * trajectory[j][1]);
            if ((pixels[p] += palette[its][0]) > brightest[0]) {
                brightest[0]++;
            }
            if ((pixels[p + 1] += palette[its][1]) > brightest[0]) {
                brightest[0]++;
            }
            if ((pixels[p + 2] += palette[its][2]) > brightest[0]) {
                brightest[0]++;
            }
        }
    }

    public static synchronized void drawLin(int[] trajectory, int its) {
        int p;
        for (int j = 5; j < its; j++) {
            p = trajectory[j];
            if ((pixels[p] += 1) > brightest[0]) {
                brightest[0]++;
            }
            pixels[p + 1] += 1;
            pixels[p + 2] += 1;
        }
    }
    
    public static synchronized void drawLinRGB(int[] trajectory, int its) {
        points += its;
        int p;
        int j = 5;
        for (; j < its && j < itsteps[0]; j++) {
            p = trajectory[j];
            if ((pixels[p] += 1) > brightest[0]) {
                brightest[0]++;
            }
            if ((pixels[p + 1] += 1) > brightest[0]) {
                brightest[0]++;
            }
            if ((pixels[p + 2] += 1) > brightest[0]) {
                brightest[0]++;
            }
        }
        for (; j < its && j < itsteps[1]; j++) {
            p = trajectory[j];
            if ((pixels[p] += 1) > brightest[0]) {
                brightest[0]++;
            }
            if ((pixels[p + 1] += 1) > brightest[0]) {
                brightest[0]++;
            }
        }
        for (; j < its; j++) {
            p = trajectory[j];
            if ((pixels[p] += 1) > brightest[0]) {
                brightest[0]++;
            }
        }
    }

    public static class Renderer extends Thread {

        private final int offset;
        private int[] trajectory = new int[iterations];
        private double[][] dTrajectory = new double[iterations][2];

        public Renderer(int offset) {
            this.offset = offset;
        }

        public void run() {
            int q = offset, pixelsToDraw;
            int x, y, i;
            for (double re = -2 + restep / density * offset; re < 0.5; re += threads * restep / density) {
                for (double im = -1; im < 1; im += imstep / density) {
                    if (!shouldDraw(re, im)) {
                        continue;
                    }
                    double[] z = {re, im};
                    double[] c = {re, im};
                    double[] t = {0, 0};
                    for (i = 0; i < iterations && z[0] * z[0] + z[1] * z[1] < 16; i++) {
                        dTrajectory[i][0] = z[0];
                        dTrajectory[i][1] = z[1];
                        t[0] = z[0] * z[0] - z[1] * z[1] + c[0];
                        t[1] = 2 * z[0] * z[1] + c[1];
                        z[0] = t[0];
                        z[1] = t[1];
                    }
                    if (i < iterations) {
                        pixelsToDraw = 0;
                        for (int j = 0; j < i; j++) {
                            z = dTrajectory[j];
                            x = (int) ((z[0] + 2.0) / restep);
                            y = (int) ((z[1] + 1.0) / imstep);
                            if (x >= 0 && x < width && y >= 0 && y < height) {
                                trajectory[pixelsToDraw++] = 3 * (width * y + x);
                            }
                        }
                        BuddhaBrot.drawLinRGB(trajectory, pixelsToDraw);
                    }
                }
                if ((q += threads) % 25 == 0) {
                    System.out.println("Thread " + offset + ": " + q);
                }
            }
            System.out.println("Thread " + offset + " finished");
        }
    }

    static {
        System.out.println("Claiming memory..");
        pixels = new int[width * height * 3];
        System.out.print("Generating palette..");
        double n = 4.18879020479 / Math.pow(iterations, gammaI);
        palette = new float[iterations][3];
        for (int i = 1; i < iterations; i++) {
            double omega = Math.pow(i, gammaI) * n;
            palette[i][0] = (float) Math.pow(Math.cos(omega) + 1, gammaC);
            palette[i][1] = (float) Math.pow(Math.cos(omega - 2.09439510239) + 1, gammaC);
            palette[i][2] = (float) Math.pow(Math.cos(omega - 4.18879020479) + 1, gammaC);
        }
        System.out.println("Done.");
    }
}