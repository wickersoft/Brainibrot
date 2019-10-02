/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wickersoft.brainibrot;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Dennis
 */
public class Shitbrot {
    public static final int width = 1920, height = 1536;
    public static final double[] xRange = {-2.0, 0.5};
    public static final double[] yRange = {-1.0, 1.0};
    public static final int iterations = 5000;
    public static final int threads = 3;
    public static final double density = 1;
    public static final String outFile = "C:/users/dennis/desktop/shit.png";

    
    public static final int[] pixels = new int[width * height];
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
        System.out.println("Brightest: " + brightest[0]);
        System.out.println("Points: " + points);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final byte[] actualPixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        penis = System.currentTimeMillis();
        for (int p = 0; p < actualPixels.length; p++) {
            actualPixels[p] = (byte) (pixels[p] != 0 ? 255 : 0);
        }
        System.out.println("Draw time: " + (System.currentTimeMillis() - penis));
        ImageIO.write(img, "PNG", new File(outFile));
    }

    public static synchronized void drawLin(int p, int its) {
        for (int j = 5; j < its; j++) {
            if ((pixels[p] += 1) > brightest[0]) {
                brightest[0]++;
            }
        }
    }

    public static class Renderer extends Thread {
        private final int offset;
        private final double sqrt2 = Math.sqrt(2.0);

        public Renderer(int offset) {
            this.offset = offset;
        }

        public void run() {
            int q = offset;
            int x = 0, y = 0, i;
            for (double re = xRange[0] + restep / density * offset; re < xRange[1]; re += threads * restep / density) {
                for (double im = yRange[0]; im < yRange[1]; im += imstep / density) {
                    double[] z = {re, im};
                    double t = 0;
                    x = (int) ((z[0] - xRange[0]) / restep);
                    y = (int) ((z[1] - yRange[0]) / imstep);
                    for (i = 0; i < iterations && z[0] * z[0] + z[1] * z[1] < 16; i++) {
                        t = z[0] + z[1];
                        z[1] = (z[0] - z[1]);
                        z[0] = t;
                    }
                    if (i < iterations) {
                        MaskGen.drawLin(width * y + x, i);
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