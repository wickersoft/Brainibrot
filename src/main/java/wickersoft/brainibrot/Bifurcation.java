package wickersoft.brainibrot;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Draws the bifurcation diagram based on the Mandelbrot equation as a
 * cross-section of the 4D-Brot particle cloud. 
 * The X axis corresponds to the real axis of the Mandelbrot set, the Y axis to the real axis of the
 * Buddhabrot figure.
 *
 * @author Dennis
 */
public class Bifurcation {

    public static final int width = 1300;
    public static final int height = 432;
    public static final int iterations = 10000;
    public static final int threads = 4;
    public static final double density = 16;

    public static int[] pixels = new int[width * height];
    public static int brightest;

    public static void main(String[] args) throws IOException, InterruptedException {
        long time = System.currentTimeMillis();
        
        // Initialize multiple render threads
        Renderer[] renderers = new Renderer[threads];
        for (int i = 0; i < threads; i++) {
            renderers[i] = new Renderer(i);
            renderers[i].start();
        }
        // Wait for Renderers to finish
        for (int i = 0; i < threads; i++) {
            renderers[i].join();
        }
        System.out.println("Render time: " + (System.currentTimeMillis() - time));
        System.out.println("Brightest: " + brightest);

        final double scale = 100000.0 / brightest;

        // Create BufferedImage and retrieve raw buffer
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final byte[] actualPixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        time = System.currentTimeMillis();
        int rgb, p;

        // Draw histogram on the image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                p = width * y + x;
                rgb = 255 - (int) (pixels[p] * scale); // Map histogram to grayscale values
                if (rgb < 0) {
                    rgb = 0;
                }
                actualPixels[width * y + x] = (byte) rgb;
            }
        }
        System.out.println("Draw time: " + (System.currentTimeMillis() - time));
        ImageIO.write(img, "PNG", new File("Bifurc.png"));
    }

    public static class Renderer extends Thread {

        private final int offset;

        public Renderer(int offset) {
            this.offset = offset;
        }

        public void run() {
            // Set up staggered line-by-line rendering
            for (int x = offset; x < width; x += threads) {
                double re = -2.0 / width * x, z = re, c = re;
                // Escape condition is never met on [-2+0i|0], so it is omitted
                for (int i = 0; i < iterations; i++) {
                    z = z * z + c;
                    // Increment the appropriate point on the histogram and update the brightness scale factor.
                    if (pixels[(int) ((z - 2) * height / -4) * width + x]++ > brightest) {
                        brightest++;
                    }
                }
                if (x % 25 == 0) {
                    System.out.println("Thread " + offset + ": " + x);
                }
            }
        }
    }
}
