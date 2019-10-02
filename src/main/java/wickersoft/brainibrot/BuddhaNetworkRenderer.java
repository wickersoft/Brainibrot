package wickersoft.brainibrot;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author root
 */
public class BuddhaNetworkRenderer {

    private static BufferedImage img;
    private static Graphics gr;
    private static int[] actualPixels = {};
    
    public static void main(String[] args) throws IOException {
    }

    public static int render(int f, double scale) throws FileNotFoundException, IOException {
        File in = new File("frames/Buddha" + f + ".raw");
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(in)));
        int width = dis.readInt();
        int height = dis.readInt();
        int brightest = dis.readInt();

        if (img == null || img.getWidth() != width || img.getHeight() != height) {
            img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            //gr = img.getGraphics();
            actualPixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        }

        // Create BufferedImage and retrieve raw buffer
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = (int) (dis.readInt() * scale);
                if (r > 255) {
                    r = 255;
                }
                int g = (int) (dis.readInt() * scale);
                if (g > 255) {
                    g = 255;
                }
                int b = (int) (dis.readInt() * scale);
                if (b > 255) {
                    b = 255;
                }
                actualPixels[width * y + x] = (r << 16) + (g << 8) + b;
            }
        }
        ImageIO.write(img, "PNG", new File("frames/Buddha" + f + ".png"));
        in.delete();
        return brightest;
    }
}
