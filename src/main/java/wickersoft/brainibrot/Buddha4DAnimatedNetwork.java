/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wickersoft.brainibrot;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.imageio.ImageIO;

/**
 *
 * @author root
 */
public class Buddha4DAnimatedNetwork {

    private static final String host = "WSDELRW62";
    public static final int threads = Runtime.getRuntime().availableProcessors();

    public static int width, height;
    public static double[] xRange, yRange;
    public static int density;
    public static boolean dump, hud;
    public static int[] itsteps;
    public static double[] aX, aY, aZ, aW;
    public static int[] pixels;
    private static byte[] maskBytes;
    public static double restep, imstep;
    public static int brightest, f;
    private static Socket s;
    private static DataInputStream dis;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.print("Loading mask..");
        BufferedImage renderMask = ImageIO.read(new File("mask.png"));
        maskBytes = ((DataBufferByte) renderMask.getRaster().getDataBuffer()).getData();
        System.out.println("Done.");

        s = new Socket();
        s.connect(new InetSocketAddress(InetAddress.getByName(host), 666));
        dis = new DataInputStream(s.getInputStream());

        while (true) {
            f = dis.readInt();
            width = dis.readInt();
            height = dis.readInt();
            xRange = new double[]{dis.readDouble(), dis.readDouble()};
            yRange = new double[]{dis.readDouble(), dis.readDouble()};
            itsteps = new int[]{dis.readInt(), dis.readInt(), dis.readInt()};
            density = dis.readInt();
            aX = new double[]{dis.readDouble(), dis.readDouble(), dis.readDouble(), dis.readDouble()};
            aY = new double[]{dis.readDouble(), dis.readDouble(), dis.readDouble(), dis.readDouble()};
            aZ = new double[]{dis.readDouble(), dis.readDouble(), dis.readDouble(), dis.readDouble()};
            aW = new double[]{dis.readDouble(), dis.readDouble(), dis.readDouble(), dis.readDouble()};
            restep = (xRange[1] - xRange[0]) / width;
            imstep = (yRange[1] - yRange[0]) / height;

            if (pixels == null || pixels.length != 3 * width * height) {
                pixels = new int[width * height * 3];
            }
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
            penis = System.currentTimeMillis();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File("frames/Buddha" + f + ".raw"))));

            dos.writeInt(width);
            dos.writeInt(height);
            dos.writeInt(brightest);

            for (int i = 0; i < pixels.length; i++) {
                dos.writeInt(pixels[i]);
                pixels[i] = 0;
            }

            dos.flush();
            dos.close();

            System.out.println(f + ": " + renderTime + "/" + (System.currentTimeMillis() - penis) + "ms");
            System.out.println("Brightest: " + brightest + " / " + brightest + " / " + brightest);
            brightest = 0;
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
            if ((pixels[p] += 1) > brightest) {
                brightest++;
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
            if ((pixels[p] += 1) > brightest) {
                brightest++;
            }
            if ((pixels[p + 1] += 1) > brightest) {
                brightest++;
            }
            if ((pixels[p + 2] += 1) > brightest) {
                brightest++;
            }
        }
        for (; j < its && j < itsteps[1]; j++) {
            p = trajectory[j];
            if ((pixels[p] += 1) > brightest) {
                brightest++;
            }
            if ((pixels[p + 1] += 1) > brightest) {
                brightest++;
            }
        }
        for (; j < its; j++) {
            p = trajectory[j];
            if ((pixels[p] += 1) > brightest) {
                brightest++;

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
                            double[] p1 = MathUtil.project(p, aX, aY, aZ, aW);
                            z = parametrize(p1, aX, aY, aZ, aW);
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
            }
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
