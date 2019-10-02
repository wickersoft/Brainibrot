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
public class Buddha4D {

    /**
     * @param args the command line arguments
     */
    public static final int width = 1920;
    public static final int height = 1080;
    public static final int iterations = 50000;
    public static final double scaleFactor = 1000;
    public static final int threads = 4;
    public static final double density = 6;
    public static final int[] itsteps = {1000, 5000, 50000};
    // Dump the 2D (Not 4D) historgram for brightness adjustment using BuddhaRenderer
    public static final boolean dump = true;

    // Set up unit vectors. Must be orthogonal for the diagram to make sense.
    // Varying the vectors' magnitudes results in magnification.
    public static double[] aX = {0, 0, 1, 0};
    public static double[] aY = {0, 0, 0, 1};
    public static double[] aZ = {1, 0, 0, 0};
    public static double[] aN = {0, 1, 0, 0};
    
    public static double[] dX = MathUtil.parametrize(new double[] {1, 0, 0, 0}, aX, aY, aZ, aN);
    public static double[] dY = MathUtil.parametrize(new double[] {0, 1, 0, 0}, aX, aY, aZ, aN);
    public static double[] dZ = MathUtil.parametrize(new double[] {0, 0, 1, 0}, aX, aY, aZ, aN);
    public static double[] dW = MathUtil.parametrize(new double[] {0, 0, 0, 1}, aX, aY, aZ, aN);
    

    /*
     Mandelbrot:
     1 0 0 0
     0 1 0 0
     0 0 1 0
     0 0 0 1
    
     Buddhabrot (Starting point and orbit planes swapped):
     0 0 1 0
     0 0 0 1
     1 0 0 0
     0 1 0 0
     */
    public static final int[] pixels = new int[3 * width * height];
    public static int[] brightest = new int[3];
    private static byte[] maskBytes;
    public static final double restep = 2.5 / width;
    public static final double imstep = 2.0 / height;
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
        double scale = scaleFactor / brightest[0];
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
        //points += its;
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
        private final int[] trajectory = new int[iterations];
        private final double[][] dTrajectory = new double[iterations][2];
        private final double[][] matrixD = {{}, {}, {}, {}}, matrixX = {{}, {}, {}, {}}, matrixY = {{}, {}, {}, {}};
        private final double[] co = {0, 0};
        private final double[] h = new double[6];
        private final double[] p1 = new double[4];

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
                        double[] constantCoords = {dTrajectory[0][0] * dX[0] + dTrajectory[0][1] * dY[0], dTrajectory[0][0] * dX[1] + dTrajectory[0][1] * dY[1]};
                        for (int j = 1; j < i; j++) {
                            z[0] = constantCoords[0] + dTrajectory[j][0] * dZ[0] + dTrajectory[j][1] * dW[0];
                            z[1] = constantCoords[1] + dTrajectory[j][0] * dZ[1] + dTrajectory[j][1] * dW[1];
                            x = (int) ((z[0] + 2.0) / restep);
                            y = (int) ((z[1] + 1.0) / imstep);
                            if (x >= 0 && x < width && y >= 0 && y < height) {
                                trajectory[pixelsToDraw++] = 3 * (width * y + x);
                            } else {
                            }
                        }
                        Buddha4D.points += pixelsToDraw;
                        Buddha4D.drawLinRGB(trajectory, pixelsToDraw);
                    } else {
                    }
                }
                if ((q += threads) % 25 == 0) {
                    System.out.println("Thread " + offset + ": " + q);
                }
            }
            System.out.println("Thread " + offset + " finished");
        }

        public double[] cross4d(double[] i, double[] j, double[] k) {
            // Calculates 4D "cross product" of three 4-vectors. 
            // The attentive observer notices the striking resemblance to a 4x4 determinant
            double[] l = new double[4];
            h[0] = i[0] * j[1] - i[1] * j[0];
            h[1] = i[0] * j[2] - i[2] * j[0];
            h[2] = i[0] * j[3] - i[3] * j[0];
            h[3] = i[1] * j[2] - i[2] * j[1];
            h[4] = i[1] * j[3] - i[3] * j[1];
            h[5] = i[2] * j[3] - i[3] * j[2];

            l[0] = h[3] * k[3] - h[4] * k[2] + h[5] * k[1];
            l[1] = h[1] * k[3] - h[2] * k[2] + h[5] * k[0];
            l[2] = h[0] * k[3] - h[2] * k[1] + h[4] * k[0];
            l[3] = h[0] * k[2] - h[1] * k[1] + h[3] * k[0];
            return l;
        }

        private double det(double[][] matrix) {
            // Calculates determinants using discrete operations for much improved speed
            switch (matrix.length) {
                case 1:
                    return (matrix[0][0]);
                case 2:
                    return matrix[0][0] * matrix[1][1] - matrix[1][0] * matrix[0][1];
                case 3:
                    h[0] = matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
                    h[1] = matrix[0][0] * matrix[1][2] - matrix[0][2] * matrix[1][0];
                    h[3] = matrix[0][1] * matrix[1][2] - matrix[0][2] * matrix[1][1];
                    return h[0] * matrix[2][2] - h[1] * matrix[2][1] + h[3] * matrix[2][0];
                case 4:
                    h[0] = matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
                    h[1] = matrix[0][0] * matrix[1][2] - matrix[0][2] * matrix[1][0];
                    h[2] = matrix[0][0] * matrix[1][3] - matrix[0][3] * matrix[1][0];
                    h[3] = matrix[0][1] * matrix[1][2] - matrix[0][2] * matrix[1][1];
                    h[4] = matrix[0][1] * matrix[1][3] - matrix[0][3] * matrix[1][1];
                    h[5] = matrix[0][2] * matrix[1][3] - matrix[0][3] * matrix[1][2];
                    return (h[0] * matrix[2][2] - h[1] * matrix[2][1] + h[3] * matrix[2][0]) * matrix[3][3]
                        + (h[0] * matrix[2][3] + h[2] * matrix[2][1] - h[4] * matrix[2][0]) * matrix[3][2]
                        + (h[1] * matrix[2][3] - h[2] * matrix[2][2] + h[5] * matrix[2][0]) * matrix[3][1]
                        + (h[3] * matrix[2][3] + h[4] * matrix[2][2] - h[5] * matrix[2][1]) * matrix[3][0];

            }
            /*
             Matrices larger than 4x4 are irrelevant for this code. 
             For an implementation I used for testing, visit 
             http://professorjava.weebly.com/matrix-determinant.html
             */
            return 0;
        }

        private double dotProduct(double[] v1, double[] v2) {
            // Utility function to test orthogonality of vectors
            return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3] * v2[3];
        }

        private double[] parametrize(double[] p, double[] x, double[] y, double[] k, double[] n) {
            matrixD[0] = x;
            matrixD[1] = y;
            matrixD[2] = k;
            matrixD[3] = n;

            matrixX[0] = p;
            matrixX[1] = y;
            matrixX[2] = k;
            matrixX[3] = n;

            matrixY[0] = x;
            matrixY[1] = p;
            matrixY[2] = k;
            matrixY[3] = n;
            double d = det(matrixD);
            double kx = det(matrixX);
            double ky = det(matrixY);
            co[0] = kx / d;
            co[1] = ky / d;
            return co;
        }
    }
}
