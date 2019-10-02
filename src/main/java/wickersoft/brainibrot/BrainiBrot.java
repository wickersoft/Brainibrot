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
 * @author root
 */
public class BrainiBrot {

    public static final int width = 2400, height = 1920;
    //public static final int width = 1920, height = 1080;
    public static final int order = 8;
    public static final double scaleFactor = 6000, gamma = 0.5;
    public static final double[] xRange = {-2.0, 0.75};
    public static final double[] yRange = {-1.1, 1.1};
    public static final int iterations = 50000;
    public static final int[] itsteps = {40, 300, 50000};
    public static final int threads = 4;
    public static final double dpp = 2;
    public static final double gammaI = 1.2;
    public static final double gammaC = 1;
    public static final boolean dump = false;

    public static final int[] pixels;
    public static final float[][] palette;
    private static byte[] maskBytes;
    public static final double restep = (xRange[1] - xRange[0]) / width;
    public static final double imstep = (yRange[1] - yRange[0]) / height;
    public static int mask_width = 24000, mask_height = 19200;
    public static int[] brightest = new int[3];
    public static long points = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        // Initialize stuff
        System.out.print("Loading mask..");
        BufferedImage renderMask = ImageIO.read(new File("mask.png")); //High-res bitmap to skip non-escaping orbits. Use Maskgen.java to generate your own
        mask_width = renderMask.getWidth();
        mask_height = renderMask.getHeight();
        maskBytes = ((DataBufferByte) renderMask.getRaster().getDataBuffer()).getData();
        System.out.println("Done.");
        
        
        
        // Render multi-threaded
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
        
        // Export raw
        if (dump) {
            File file = new File("Braini.raw");
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            for (int i = 0; i < pixels.length; i++) {
                dos.writeInt(pixels[i]);
            }
            dos.flush();
            dos.close();
        }
        
        
        // Export PNG
        int total_brightest = Math.max(Math.max(brightest[0], brightest[1]), brightest[2]);
        System.out.printf("Brightest: %d %d %d %d", brightest[0], brightest[1], brightest[2], total_brightest);
        System.out.println("Points: " + points);
        double scale = scaleFactor / Math.pow(total_brightest, gamma);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] actualPixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        penis = System.currentTimeMillis();
        int r, g, b, p;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                p = 3 * (width * y + x);
                if ((r = (int) (Math.pow(pixels[p], gamma) * scale)) > 255) {
                    r = 255;
                }
                if ((g = (int) (Math.pow(pixels[p + 1], gamma) * scale)) > 255) {
                    g = 255;
                }
                if ((b = (int) (Math.pow(pixels[p + 2], gamma) * scale)) > 255) {
                    b = 255;
                }
                actualPixels[width * y + x] = (r << 16) + (g << 8) + b;
            }
        }
        System.out.println("Draw time: " + (System.currentTimeMillis() - penis));
        ImageIO.write(img, "PNG", new File("Braini" + order + "_" + (int) dpp + "x_" + (int) (width * height / 1000000) + "MP.png"));
    }

    public static boolean shouldDraw(double x, double y) {
        int iX = (int) ((x + 2.0) / 2.5 * mask_width);
        int iY = (int) ((y + 1.0) / 2.0 * mask_height);
        if (iX < 0 || iX >= mask_width) {
            return true;
        }
        if (iY < 0 || iY >= mask_height) {
            return true;
        }
        return maskBytes[mask_width * iY + iX] != 0;
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

    public static synchronized void drawLin(int p, int its) {
        for (int j = 5; j < its; j++) {
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

    public static synchronized void drawLinRGB(int p, int its) {
        int add_b = Math.min(its, itsteps[0]) - 5;
        if ((pixels[p + 2] += add_b) > brightest[0]) {
            brightest[0] = pixels[p + 2];
        }
        if (its <= itsteps[0]) {
            return;
        }
        
        int add_g = Math.min(its, itsteps[1]) - itsteps[0];
        if ((pixels[p + 1] += add_g) > brightest[1]) {
            brightest[1] = pixels[p + 1];
        }
        if (its <= itsteps[1]) {
            return;
        }
        
        int add_r = Math.min(its, itsteps[2]) - itsteps[1];
        if ((pixels[p] += add_r) > brightest[2]) {
            brightest[2] = pixels[p];
        }
    }

    public static class Renderer extends Thread {

        private final int offset;

        public Renderer(int offset) {
            this.offset = offset;
        }

        public void run() {
            int q = offset, loc = 0;
            int x = 0, y = 0, i;
            for (double re = xRange[0] + restep / dpp * offset; re < xRange[1]; re += threads * restep / dpp) {
                for (double im = yRange[0]; im < yRange[1]; im += imstep / dpp) {
                    if (!shouldDraw(re, im)) {
                        continue;
                    }
                    double[] z = {re, im};
                    double[] c = {re, im};
                    double[] t = {0, 0};
                    for (i = 0; i < iterations && z[0] * z[0] + z[1] * z[1] < 16; i++) {
                        if (i == order) {
                            x = (int) ((z[0] - xRange[0]) / restep);
                            y = (int) ((z[1] - yRange[0]) / imstep);
                        }
                        t[0] = z[0] * z[0] - z[1] * z[1] + c[0];
                        t[1] = 2 * z[0] * z[1] + c[1];
                        z[0] = t[0];
                        z[1] = t[1];
                    }
                    if (i < iterations && i > order && x > 0 && x < width && y > 0 && y < height) {
                        loc = 3 * (width * y + x);
                        BrainiBrot.drawLinRGB(loc, i);
                    }
                }
                if ((q += threads) % 25 == 0) {
                    System.out.println("Thread " + offset + ": " + q + " (" + (int) (100 * q / width / dpp) + "%)");
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
        palette = new float[itsteps[2]][3];
        for (int i = 1; i < itsteps[2]; i++) {
            double omega = Math.pow(i, gammaI) * n;
            palette[i][0] = (float) Math.pow(Math.cos(omega) + 1, gammaC);
            palette[i][1] = (float) Math.pow(Math.cos(omega - 2.09439510239) + 1, gammaC);
            palette[i][2] = (float) Math.pow(Math.cos(omega - 4.18879020479) + 1, gammaC);
        }
        System.out.println("Done.");
    }
}
