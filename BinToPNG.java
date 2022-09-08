import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class BinToPNG {

	public static File inFile = new File("domp.bin");
	public static File outFile;

	public static void main(String[] args) throws IOException {
		long size = inFile.length(); // Assume file is twice the size and use half height later
		int height = (int) Math.sqrt(size / 5L);
		int width = height * 5 / 4;

		if(args.length == 2) {
			width = Integer.parseInt(args[0]);
			height = Integer.parseInt(args[1]);
		}

		System.out.println("Width: " + width + " Height: " + height);

		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] actualPixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

		DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(inFile)));

		for(int i = 0; i < size / 4; i++) {
			actualPixels[i] = dis.readInt();
		}

		ImageIO.write(img, "PNG", new File("Brainip_qx_" + (long) (width * height / 1000000L) + "MP.png"));
	}
}