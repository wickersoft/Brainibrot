/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wickersoft.brainibrot;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 *
 * @author root
 */
public class WhiteBalanceMaster {

    private static final int width = 1920, height = 1080;
    private static final double[] xRange = {-2.75, 2.75}, yRange = {-1.546875, 1.546875};
    private static final int[] itsteps = {1000, 5000, 50000};
    private static final int density = 8;

    private static double[] aX = {1, 0, 0, 0};
    private static double[] aY = {0, 1, 0, 0};
    private static double[] aZ = {0, 0, 1, 0};
    private static double[] aW = {0, 0, 0, 1};
    private static final HashMap<Integer, double[]> trace = new HashMap<>();
    private static final double[] omega = {0, 0, 0, 0, 0, 0};
    private static double[] targetOmega = {0.01, 0.01, 0.01, 0.01, 0.01, 0.01};
    private static int frameEnd = 100, f = 0;

    public static void main(String[] args) throws IOException {
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

        ServerSocket ss = new ServerSocket(666);
        
        while (f < frameEnd) { // Dispatch consecutive render jobs
            try {
                if (!new File("frames/Buddha" + f + ".png").exists()) {
                    Socket s = ss.accept();
                    DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                    sendParameters(dos);
                    dos.close();
                    s.close();
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

                rotate(omega);
                f++;
            } catch (IOException ex) {
            }
        }
    }

    private static void sendParameters(DataOutputStream dos) throws IOException {
        dos.writeInt(f);
        dos.writeInt(width);
        dos.writeInt(height);
        dos.writeDouble(xRange[0]);
        dos.writeDouble(xRange[1]);
        dos.writeDouble(yRange[0]);
        dos.writeDouble(yRange[1]);
        dos.writeInt(itsteps[0]);
        dos.writeInt(itsteps[1]);
        dos.writeInt(itsteps[2]);
        dos.writeInt(density);

        dos.writeDouble(aX[0]);
        dos.writeDouble(aX[1]);
        dos.writeDouble(aX[2]);
        dos.writeDouble(aX[3]);

        dos.writeDouble(aY[0]);
        dos.writeDouble(aY[1]);
        dos.writeDouble(aY[2]);
        dos.writeDouble(aY[3]);

        dos.writeDouble(aZ[0]);
        dos.writeDouble(aZ[1]);
        dos.writeDouble(aZ[2]);
        dos.writeDouble(aZ[3]);

        dos.writeDouble(aW[0]);
        dos.writeDouble(aW[1]);
        dos.writeDouble(aW[2]);
        dos.writeDouble(aW[3]);
    }

    private static void rotate(double[] omega) {
        aX = MathUtil.rotateXY(aX, omega[0]);
        aY = MathUtil.rotateXY(aY, omega[0]);
        aZ = MathUtil.rotateXY(aZ, omega[0]);
        aW = MathUtil.rotateXY(aW, omega[0]);

        aX = MathUtil.rotateXZ(aX, omega[1]);
        aY = MathUtil.rotateXZ(aY, omega[1]);
        aZ = MathUtil.rotateXZ(aZ, omega[1]);
        aW = MathUtil.rotateXZ(aW, omega[1]);

        aX = MathUtil.rotateXW(aX, omega[2]);
        aY = MathUtil.rotateXW(aY, omega[2]);
        aZ = MathUtil.rotateXW(aZ, omega[2]);
        aW = MathUtil.rotateXW(aW, omega[2]);

        aX = MathUtil.rotateYZ(aX, omega[3]);
        aY = MathUtil.rotateYZ(aY, omega[3]);
        aZ = MathUtil.rotateYZ(aZ, omega[3]);
        aW = MathUtil.rotateYZ(aW, omega[3]);

        aX = MathUtil.rotateYW(aX, omega[4]);
        aY = MathUtil.rotateYW(aY, omega[4]);
        aZ = MathUtil.rotateYW(aZ, omega[4]);
        aW = MathUtil.rotateYW(aW, omega[4]);

        aX = MathUtil.rotateZW(aX, omega[5]);
        aY = MathUtil.rotateZW(aY, omega[5]);
        aZ = MathUtil.rotateZW(aZ, omega[5]);
        aW = MathUtil.rotateZW(aW, omega[5]);
    }
}
