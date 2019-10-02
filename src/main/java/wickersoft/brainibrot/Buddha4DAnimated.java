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
public class Buddha4DAnimated {

    /**
     * @param args the command line arguments
     */
    public static final int width = 1920;
    public static final int height = 1080;
    public static final double[] xRange = {-2.75, 2.75};
    public static final double[] yRange = {-1.546875, 1.546875};
    public static final int iterations = 1000;
    public static final int threads = 4;
    public static final int density = 8;
    public static final boolean dump = false, hud = false;
    public static final int[] itsteps = {1000, 5000, 50000};

    public static double[] aX = {1, 0, 0, 0};
    public static double[] aY = {0, 1, 0, 0};
    public static double[] aZ = {0, 0, 1, 0};
    public static double[] aN = {0, 0, 0, 1};

    public static final int[] pixels = new int[width * height * 3];
    private static byte[] maskBytes;
    public static final double restep = (xRange[1] - xRange[0]) / width;
    public static final double imstep = (yRange[1] - yRange[0]) / height;
    public static int[] brightest = new int[3];
    private static int stepSize, procId;
    private static final HashMap<Integer, double[]> trace = new HashMap<>();
    private static double[] omega = {0, 0, 0, 0, 0, 0};
    private static double[] targetOmega = {0.01, 0.01, 0.01, 0.01, 0.01, 0.01};

    public static void main(String[] args) throws IOException, InterruptedException {
        final boolean dev = args.length == 0;
        int frameOffset = 0;
        int frameEnd = 100;
        if (!dev) { // Use default parameters if launching from IDE / without arguments
            stepSize = Integer.parseInt(args[0]);
            procId = Integer.parseInt(args[1]);
            if (args.length >= 3) {
                frameOffset = Integer.parseInt(args[2]);
            }
        }
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
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics gr = img.getGraphics();
        final int[] actualPixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        for (int f = 0; f < frameEnd; f++) {
            if (dev || f % stepSize == procId && f >= frameOffset && !(new File("frames/Buddha" + f + ".png").exists())) {
                Renderer[] renderers = new Renderer[threads];
                long penis = System.currentTimeMillis();
                for (int i = 0; i < threads; i++) {
                    renderers[i] = new Renderer(i, f);
                    renderers[i].start();
                }
                for (int i = 0; i < threads; i++) {
                    renderers[i].join();
                }
                long renderTime = System.currentTimeMillis() - penis;
                if (dump) {
                    file = new File("Buddha.raw");
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
                double[] scale = {2000.0 / brightest[0], 2000.0 / brightest[0], 2000.0 / brightest[0]};
                penis = System.currentTimeMillis();
                int r, g, b, p;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        p = 3 * (width * y + x);
                        if ((r = (int) (pixels[p] * scale[0])) > 255) {
                            r = 255;
                        }
                        if ((g = (int) (pixels[p + 1] * scale[1])) > 255) {
                            g = 255;
                        }
                        if ((b = (int) (pixels[p + 2] * scale[2])) > 255) {
                            b = 255;
                        }
                        actualPixels[width * y + x] = (r << 16) + (g << 8) + b;
                        pixels[p] = 0;
                        pixels[p + 1] = 0;
                        pixels[p + 2] = 0; //Reset internal frame buffer
                    }
                }
                
                if(hud) {
                
                gr.setColor(Color.white);
                gr.setFont(new Font("Arial", Font.PLAIN, 60));
                gr.drawString("Screen Plane:", 1240, 70);
                
                gr.setFont(new Font("Arial", Font.PLAIN, 42));
                
                gr.fillPolygon(new int[] {
                    1600, 1600, 1590, 1607, 1624, 1614, 1614
                }, new int[] {
                    410, 300, 300, 180, 300, 300, 410
                }, 7);
                
                gr.fillPolygon(new int[] {
                    1600, 1720, 1720, 1840, 1720, 1720, 1600
                }, new int[] {
                    410, 410, 403, 419, 428, 421, 421
                }, 7);
                
                gr.drawString("(" 
                        + (Math.round(aY[0] * 100.0) / 100.0) + "  " 
                        + (Math.round(aY[1] * 100.0) / 100.0) + "  " 
                        + (Math.round(aY[2] * 100.0) / 100.0) + "  " 
                        + (Math.round(aY[3] * 100.0) / 100.0) + ")"
                        , 1416, 150);
                gr.drawString("(" 
                        + (Math.round(aX[0] * 100.0) / 100.0) + "  " 
                        + (Math.round(aX[1] * 100.0) / 100.0) + "  " 
                        + (Math.round(aX[2] * 100.0) / 100.0) + "  " 
                        + (Math.round(aX[3] * 100.0) / 100.0) + ")"
                        , 1416, 475);
                
                
                gr.setFont(new Font("Arial", Font.PLAIN, 60));
                gr.drawString("Normal Plane:", 1240, 610);
                
                gr.setFont(new Font("Arial", Font.PLAIN, 42));
                
                gr.fillPolygon(new int[] {
                    1600, 1600, 1590, 1607, 1624, 1614, 1614
                }, new int[] {
                    950, 840, 840, 720, 840, 840, 950
                }, 7);
                
                gr.fillPolygon(new int[] {
                    1600, 1720, 1720, 1840, 1720, 1720, 1600
                }, new int[] {
                    950, 950, 943, 959, 968, 961, 961
                }, 7);
                
                gr.drawString("(" 
                        + (Math.round(aZ[0] * 100.0) / 100.0) + "  " 
                        + (Math.round(aZ[1] * 100.0) / 100.0) + "  " 
                        + (Math.round(aZ[2] * 100.0) / 100.0) + "  " 
                        + (Math.round(aZ[3] * 100.0) / 100.0) + ")"
                        , 1416, 690);
                gr.drawString("(" 
                        + (Math.round(aN[0] * 100.0) / 100.0) + "  " 
                        + (Math.round(aN[1] * 100.0) / 100.0) + "  " 
                        + (Math.round(aN[2] * 100.0) / 100.0) + "  " 
                        + (Math.round(aN[3] * 100.0) / 100.0) + ")"
                        , 1416, 1015);
                
                }
                
                System.out.println(f + ": " + renderTime + "/" + (System.currentTimeMillis() - penis) + "ms");
                System.out.println("Brightest: " + brightest[0] + " / " + brightest[0] + " / " + brightest[0]);
                ImageIO.write(img, "PNG", new File("frames/Buddha" + f + ".png"));
                brightest[0] = 0;
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
            aN = MathUtil.rotateXY(aN, omega[0]);

            aX = MathUtil.rotateXZ(aX, omega[1]);
            aY = MathUtil.rotateXZ(aY, omega[1]);
            aZ = MathUtil.rotateXZ(aZ, omega[1]);
            aN = MathUtil.rotateXZ(aN, omega[1]);
            
            aX = MathUtil.rotateXW(aX, omega[2]);
            aY = MathUtil.rotateXW(aY, omega[2]);
            aZ = MathUtil.rotateXW(aZ, omega[2]);
            aN = MathUtil.rotateXW(aN, omega[2]);

            aX = MathUtil.rotateYZ(aX, omega[3]);
            aY = MathUtil.rotateYZ(aY, omega[3]);
            aZ = MathUtil.rotateYZ(aZ, omega[3]);
            aN = MathUtil.rotateYZ(aN, omega[3]);
            
            aX = MathUtil.rotateYW(aX, omega[4]);
            aY = MathUtil.rotateYW(aY, omega[4]);
            aZ = MathUtil.rotateYW(aZ, omega[4]);
            aN = MathUtil.rotateYW(aN, omega[4]);

            aX = MathUtil.rotateZW(aX, omega[5]);
            aY = MathUtil.rotateZW(aY, omega[5]);
            aZ = MathUtil.rotateZW(aZ, omega[5]);
            aN = MathUtil.rotateZW(aN, omega[5]);
        }
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
        private final int[] trajectory = new int[itsteps[2]];
        private final double[][] dTrajectory = new double[itsteps[2]][2];
        private final double[][] matrixD = {{}, {}, {}, {}}, matrixX = {{}, {}, {}, {}}, matrixY = {{}, {}, {}, {}};
        private final double[] co = {0, 0};
        private final double[] h = new double[6];
        private final double[] p1 = new double[4];
        private final double pulse;

        public Renderer(int offset, int f) {
            this.offset = offset;
            pulse = 1 + Math.sin(6.283185307 * f / 12.0) / 2;
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
                    for (i = 0; i < itsteps[2] && z[0] * z[0] + z[1] * z[1] < 16; i++) {
                        dTrajectory[i][0] = z[0];
                        dTrajectory[i][1] = z[1];
                        t[0] = z[0] * z[0] - z[1] * z[1] + c[0];
                        t[1] = 2 * z[0] * z[1] + c[1];
                        z[0] = t[0];
                        z[1] = t[1];
                    }
                    if (i < itsteps[2]) {
                        pixelsToDraw = 0;
                        double[] p = {dTrajectory[0][0], dTrajectory[0][1], 0, 0};
                        for (int j = 1; j < i; j++) {
                            p[2] = dTrajectory[j][0];
                            p[3] = dTrajectory[j][1];
                            double[] p1 = MathUtil.project(p, aX, aY, aZ, aN);
                            z = parametrize(p1, aX, aY, aZ, aN);
                            x = (int) ((z[0] - xRange[0]) / restep);
                            y = (int) ((z[1] - yRange[0]) / imstep);
                            //System.out.println(p1[0] + " " + p1[1] + " " + p1[2] + " " + p1[3] + " " + z[0] + " " + z[1]);
                            if (x >= 0 && x < width && y >= 0 && y < height) {
                                trajectory[pixelsToDraw++] = 3 * (width * y + x);
                            } else {
                            }
                        }
                        Buddha4DAnimated.drawLinRGB(trajectory, pixelsToDraw);
                    } else {
                    }
                }
                if ((q += threads) % 25 == 0) {
                    //System.out.println("Thread " + offset + ": " + q);
                }
            }
            //System.out.println("Thread " + offset + " finished");
        }

        private double[] cross4d(double[] i, double[] j, double[] k) {
            double[] l = new double[4];
            l[3] = (i[0] * j[1] - i[1] * j[0]) * k[2] - (i[0] * j[2] - i[2] * j[0]) * k[1] + (i[1] * j[2] - i[2] * j[1]) * k[0];
            l[2] = (i[0] * j[1] - i[1] * j[0]) * k[3] - (i[0] * j[3] - i[3] * j[0]) * k[1] + (i[1] * j[3] - i[3] * j[1]) * k[0];
            l[1] = (i[0] * j[2] - i[2] * j[0]) * k[3] - (i[0] * j[3] - i[3] * j[0]) * k[2] + (i[2] * j[3] - i[3] * j[2]) * k[0];
            l[0] = (i[1] * j[2] - i[2] * j[1]) * k[3] - (i[1] * j[3] - i[3] * j[1]) * k[2] + (i[2] * j[3] - i[3] * j[2]) * k[1];
            return l;
        }

        private double det(double[][] matrix) { //method sig. takes a matrix (two dimensional array), returns determinant.            
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
            double s, sum = 0;
            for (int i = 0; i < matrix.length; i++) { //finds determinant using row-by-row expansion
                double[][] smaller = new double[matrix.length - 1][matrix.length - 1]; //creates smaller matrix- values not in same row, column
                for (int a = 1; a < matrix.length; a++) {
                    for (int b = 0; b < matrix.length; b++) {
                        if (b < i) {
                            smaller[a - 1][b] = matrix[a][b];
                        } else if (b > i) {
                            smaller[a - 1][b - 1] = matrix[a][b];
                        }
                    }
                }
                s = 1 - 2 * (i % 2);
                sum += s * matrix[0][i] * det(smaller);
            }
            return sum; //returns determinant value. once stack is finished, returns final determinant.
        }

        private double[] project(double[] p, double[] i, double[] j, double[] k, double[] n) {
            double r = -(p[0] * n[0] + p[1] * n[1] + p[2] * n[2] + p[3] * n[3]) / (n[0] * n[0] + n[1] * n[1] + n[2] * n[2] + n[3] * n[3]);
            p1[0] = p[0] + r * n[0];
            p1[1] = p[1] + r * n[1];
            p1[2] = p[2] + r * n[2];
            p1[3] = p[3] + r * n[3];
            r = -(p1[0] * k[0] + p1[1] * k[1] + p1[2] * k[2] + p1[3] * k[3]) / (k[0] * k[0] + k[1] * k[1] + k[2] * k[2] + k[3] * k[3]);
            p1[0] = p1[0] + r * k[0];
            p1[1] = p1[1] + r * k[1];
            p1[2] = p1[2] + r * k[2];
            p1[3] = p1[3] + r * k[3];
            return p1;
        }

        private double dotProduct(double[] v1, double[] v2) {
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
