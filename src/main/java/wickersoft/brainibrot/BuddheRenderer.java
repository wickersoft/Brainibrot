/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wickersoft.brainibrot;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Dennis
 */
public class BuddheRenderer {

    public static final int width = 2500;
    public static final int height = 2000;
    public static final int iterations = 10000;
    public static final double density = 4;
    public static final double gammaI = 0.3; //Gradient Gamma
    public static final double gammaC = 1.2; //Color Gamma

    public static final double[][][] pixels = new double[width][height][3];
    public static final double[][] palette;

    public static void main(String[] args) throws IOException {
        File in = new File("D:/Buddha_10Ki_200K.raw");
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(in)));
        int brightest = 0;
        int q = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if ((pixels[i][j][0] = dis.readInt()) > q) {
                    q++;
                }
                if ((pixels[i][j][1] = dis.readInt()) > q) {
                    q++;
                }
                if ((pixels[i][j][2] = dis.readInt()) > q) {
                    q++;
                }
            }
            if (j % 100 == 0) {
                System.out.println(j);
            }
        }
        System.out.println("brightest: " + brightest);
        double scale = 4096.0 / brightest;
        double base = Math.pow(brightest, 1 / (255.0));
        double ln = Math.log(base);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] actualPixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //int rgb = (int) (Math.log(pixels[x][y]) / ln);
                int r = (int) (pixels[x][y][0] * scale);
                if (r > 255) {
                    r = 255;
                }
                int g = (int) (pixels[x][y][1] * scale);
                if (g > 255) {
                    g = 255;
                }
                int b = (int) (pixels[x][y][2] * scale);
                if (b > 255) {
                    b = 255;
                }
                actualPixels[width * y + x] = (r << 16) + (g << 8) + b;
            }
        }
        ImageIO.write(img, "PNG", new File("D:/Buddha.png"));
    }

    static {
        double n = 4.18879020479 / Math.pow(iterations, gammaI);
        palette = new double[iterations][3];
        for (int i = 1; i < iterations; i++) {
            double omega = Math.pow(i, gammaI) * n;
            palette[i][0] = Math.pow(Math.cos(omega) + 1, gammaC);
            palette[i][1] = Math.pow(Math.cos(omega - 2.09439510239) + 1, gammaC) * 0.25;
            palette[i][2] = Math.pow(Math.cos(omega - 4.18879020479) + 1, gammaC);
        }
    }
}
