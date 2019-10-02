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
 * Generates an image file from a histogram dump. Useful for adjusting the scale
 * factor without re-calculating if your image is over- or underexposed.
 *
 * @author Dennis
 */
public class BuddhaRenderer {

    public static int width = 19200;
    public static int height = 15360;
    public static double scaleFactor = 14000, gamma = 0.5;
    public static String inFile = "Braini.raw";
    public static String outFile = "Braini.bmp"; // Adjust encoding in ImageIO if you want a different format
    public static int brightest = 22280800;

    public static int[] pixels = new int[3 * width * height];

    public static void main(String[] args) throws IOException {
        File in = new File(inFile);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(in)));
        
        double scale = scaleFactor / Math.pow(brightest, gamma);

        // Create BufferedImage and retrieve raw buffer
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] actualPixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        int r, g, b, p;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                p = 3 * (y * width + x);
                r = (int) (Math.pow(dis.readInt(), gamma) * scale);
                if (r > 255) {
                    r = 255;
                }
                g = (int) (Math.pow(dis.readInt(), gamma) * scale);
                if (g > 255) {
                    g = 255;
                }
                b = (int) (Math.pow(dis.readInt(), gamma) * scale);
                if (b > 255) {
                    b = 255;
                }
                actualPixels[width * y + x] = (r << 16) + (g << 8) + b;
            }
            if(y % 100 == 0) {
                System.out.println(y);
            }
        }
        ImageIO.write(img, "BMP", new File(outFile));
    }
}
