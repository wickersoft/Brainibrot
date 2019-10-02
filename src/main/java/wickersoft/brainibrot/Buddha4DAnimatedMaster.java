/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wickersoft.brainibrot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

/**
 *
 * @author root
 */
public class Buddha4DAnimatedMaster {

    private static int avgSize = 16;
    private static double[] brightness = new double[avgSize];

    private static double[] aX = {1, 0, 0, 0};
    private static double[] aY = {0, 1, 0, 0};
    private static double[] aZ = {0, 0, 1, 0};
    private static double[] aN = {0, 0, 0, 1};
    private static final HashMap<Integer, double[]> trace = new HashMap<>();
    private static double[] omega = {0, 0, 0, 0, 0, 0};
    private static double[] targetOmega = {0.01, 0.01, 0.01, 0.01, 0.01, 0.01};
    private static int frameEnd = 100;

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < avgSize; i++) {
            brightness[i] = 2000;
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
        ServerSocket ss = new ServerSocket(666);
        for (int f = 0; f < frameEnd; f++) {
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
}
