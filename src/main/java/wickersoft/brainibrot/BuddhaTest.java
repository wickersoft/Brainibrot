/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wickersoft.brainibrot;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

/**
 *
 * @author root
 */
public class BuddhaTest {

    static double[] aX = {1, 0, 0, 0};
    static double[] aY = {0, 1, 0, 0};
    static double[] aZ = {0, 0, 1, 0};
    static double[] aN = MathUtil.cross4d(aX, aY, aZ);
    private static final HashMap<Integer, double[]> trace = new HashMap<>();
    private static double[] omega;
    private static double[] dots = new double[6];

    public static void main(String[] args) throws Exception {
        int frameOffset = 0;
        int frameEnd = 100;
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
                double[] omega = {Double.parseDouble(l[1]), Double.parseDouble(l[2]), Double.parseDouble(l[3])};
                trace.put(ind, omega);
            }
        }
        System.out.println(trace.size() + " scenes loaded");

        for (int f = 0; f < frameEnd; f++) {
            if (trace.containsKey(f)) {
                omega = trace.get(f);
            }

            aX = MathUtil.rotateXY(aX, omega[0]);
            aY = MathUtil.rotateXY(aY, omega[0]);
            aZ = MathUtil.rotateXY(aZ, omega[0]);
            aN = MathUtil.rotateXY(aN, omega[0]);

            aX = MathUtil.rotateYZ(aX, omega[1]);
            aY = MathUtil.rotateYZ(aY, omega[1]);
            aZ = MathUtil.rotateYZ(aZ, omega[1]);
            aN = MathUtil.rotateYZ(aN, omega[1]);

            aX = MathUtil.rotateZW(aX, omega[2]);
            aY = MathUtil.rotateZW(aY, omega[2]);
            aZ = MathUtil.rotateZW(aZ, omega[2]);
            aN = MathUtil.rotateZW(aN, omega[2]);

            
            
            if((dots[0] = MathUtil.dotProduct(aX, aY)) > 1e-9) {
                System.out.println("Frame " + f + ": x * y = " + dots[0]);
            }
            if((dots[1] = MathUtil.dotProduct(aX, aZ)) > 1e-9) {
                System.out.println("Frame " + f + ": x * z = " + dots[1]);
            }
            if((dots[2] = MathUtil.dotProduct(aX, aN)) > 1e-9) {
                System.out.println("Frame " + f + ": x * n = " + dots[2]);
            }
            if((dots[3] = MathUtil.dotProduct(aY, aZ)) > 1e-9) {
                System.out.println("Frame " + f + ": y * z = " + dots[3]);
            }
            if((dots[4] = MathUtil.dotProduct(aY, aN)) > 1e-9) {
                System.out.println("Frame " + f + ": y * n = " + dots[4]);
            }
            if((dots[5] = MathUtil.dotProduct(aZ, aN)) > 1e-9) {
                System.out.println("Frame " + f + ": z * n = " + dots[5]);
                System.out.println("z = {" + aX[0] + ", " + aX[1] + ", " + aX[2] + ", " + aX[3] + "}");
                System.out.println("z = {" + aY[0] + ", " + aY[1] + ", " + aY[2] + ", " + aY[3] + "}");
                System.out.println("z = {" + aZ[0] + ", " + aZ[1] + ", " + aZ[2] + ", " + aZ[3] + "}");
                System.out.println("n = {" + aN[0] + ", " + aN[1] + ", " + aN[2] + ", " + aN[3] + "}");
            }
            
            
            
        }
    }
}
