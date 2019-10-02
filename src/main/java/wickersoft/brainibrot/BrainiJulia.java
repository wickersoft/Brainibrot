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
public class BrainiJulia {

    public static final int width = 1920, height = 960;
    //public static final int width = 1920, height = 1080;
    public static final int order = 100;
    public static final double scaleFactor = 6000, gamma = 0.5;
    public static final double[] xRange = {-2.0, 2.0};
    public static final double[] yRange = {-1.0, 1.0};
    public static final double[] c = {-0.835, -0.2321};
    public static final int[] itsteps = {12, 50, 500};
    public static final int threads = 4;
    public static final double density = 32;
    public static final double gammaI = 1.2;
    public static final double gammaC = 1;
    public static final boolean dump = false;

    public static final int[] pixels = new int[3 * width * height];
    public static final double restep = (xRange[1] - xRange[0]) / width;
    public static final double imstep = (yRange[1] - yRange[0]) / height;
    public static int[] brightest = new int[3];
    public static long points = 0;
    public static int[] maskDimensions = {0, 0};

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
            File file = new File("BrainiJ.raw");
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            for (int i = 0; i < pixels.length; i++) {
                dos.writeInt(pixels[i]);
            }
            dos.flush();
            dos.close();
        }
        System.out.println("Brightest: " + brightest[0]);
        System.out.println("Points: " + points);
        double scale = scaleFactor / Math.pow(brightest[0], gamma);
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
        ImageIO.write(img, "PNG", new File("BrainiJ" + order + "_" + c[0] + "+" + c[1] + "i_" + (int) density + "x_" + (int) (width * height / 1000000) + "MP.png"));
    }

    public static synchronized void drawLinRGB(int p, int its) {
        points += its;
        int j = 0;
        if(its < itsteps[0] + order) {
            if ((pixels[p + 2] += 1) > brightest[0]) {
                brightest[0]++;
            }
        } else if (its < itsteps[1] + order) {
            if ((pixels[p + 1] += 1) > brightest[0]) {
                brightest[0]++;
            }
        } else {
            if ((pixels[p] += 1) > brightest[0]) {
                brightest[0]++;
            }
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
            for (double re = xRange[0] + restep / density * offset; re < xRange[1]; re += threads * restep / density) {
                for (double im = yRange[0]; im < yRange[1]; im += imstep / density) {
                    double[] z = {re, im};
                    //double[] c = {re, im};
                    double[] t = {0, 0};
                    for (i = 0; i < itsteps[2] && z[0] * z[0] + z[1] * z[1] < 16; i++) {
                        if (i == order) {
                            x = (int) ((z[0] - xRange[0]) / restep);
                            y = (int) ((z[1] - yRange[0]) / imstep);
                        }
                        t[0] = z[0] * z[0] - z[1] * z[1] + c[0];
                        t[1] = 2 * z[0] * z[1] + c[1];
                        z[0] = t[0];
                        z[1] = t[1];
                    }
                    if (i > order && x > 0 && x < width && y > 0 && y < height) {
                        loc = 3 * (width * y + x);
                        BrainiJulia.drawLinRGB(loc, i);
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
