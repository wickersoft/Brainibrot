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
public class BuddhaJulia {

    /**
     * @param args the command line arguments
     */
    public static final int width = 1920;
    public static final int height = 960;
    public static final double[] c = {-0.2, -0.8};
    public static final int[] itsteps = {20, 40, 60};
    public static final double[] xRange = {-2.0, 2.0};
    public static final double[] yRange = {-1.0, 1.0};
    public static final int threads = 4;
    public static final double density = 8;
    public static final double gammaI = 1.2;
    public static final double gammaC = 0;
    public static final boolean dump = true;

    public static final int[] pixels = new int[3 * width * height];
    private static byte[] maskBytes;
    public static final double restep = (xRange[1] - xRange[0]) / width;
    public static final double imstep = (yRange[1] - yRange[0]) / height;
    public static int[] brightest = new int[3];
    public static long points = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
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
            File file = new File("BuddhaJulia.raw");
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
        double scale = 8000.0 / brightest[0];
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
        ImageIO.write(img, "PNG", new File("BuddhaJulia.png"));
    }

    public static boolean shouldDraw(double x, double y) {
        int iX = (int) ((x + 2.0) / 2.5 * 19200);
        int iY = (int) ((y + 1.0) / 2.0 * 15360);
        return maskBytes[19200 * iY + iX] != 0;
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
        private int[] trajectory = new int[itsteps[2]];
        private double[][] dTrajectory = new double[itsteps[2]][2];

        public Renderer(int offset) {
            this.offset = offset;
        }

        public void run() {
            int q = offset, pixelsToDraw;
            int x, y, i;
            for (double re = xRange[0] + restep / density * offset; re < xRange[1]; re += threads * restep / density) {
                for (double im = yRange[0]; im < yRange[1]; im += imstep / density) {
                    double[] z = {re, im};
                    double[] t = {0, 0};
                    for (i = 0; i < itsteps[2] && z[0] * z[0] + z[1] * z[1] < 16; i++) {
                        dTrajectory[i][0] = z[0];
                        dTrajectory[i][1] = z[1];
                        t[0] = z[0] * z[0] - z[1] * z[1] + c[0];
                        t[1] = 2 * z[0] * z[1] + c[1];
                        z[0] = t[0];
                        z[1] = t[1];
                    }
                    if (i <= itsteps[2]) {
                        pixelsToDraw = 0;
                        for (int j = 0; j < i; j++) {
                            z = dTrajectory[j];
                            x = (int) ((z[0] + 2.0) / restep);
                            y = (int) ((z[1] + 1.0) / imstep);
                            if (x >= 0 && x < width && y >= 0 && y < height) {
                                trajectory[pixelsToDraw++] = 3 * (width * y + x);
                            }
                        }
                        BuddhaJulia.drawLinRGB(trajectory, pixelsToDraw);
                    }
                }
                if ((q += threads) % 25 == 0) {
                    System.out.println("Thread " + offset + ": " + q + " (" + (int) (100 * q / width / density) + "%)");
                }
            }
            System.out.println("Thread " + offset + " finished");
        }
    }
}
