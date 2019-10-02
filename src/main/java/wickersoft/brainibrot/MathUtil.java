/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wickersoft.brainibrot;

/**
 *
 * @author root
 */
public class MathUtil {

    private static double[] h = new double[6], co = new double[2];
    private static final double[][] matrixD = {{}, {}, {}, {}}, matrixX = {{}, {}, {}, {}}, matrixY = {{}, {}, {}, {}};

    public static double[] cross4d(double[] i, double[] j, double[] k) {
        double[] l = new double[4];
        h[0] = i[0] * j[1] - i[1] * j[0];
        h[1] = i[0] * j[2] - i[2] * j[0];
        h[2] = i[0] * j[3] - i[3] * j[0];
        h[3] = i[1] * j[2] - i[2] * j[1];
        h[4] = i[1] * j[3] - i[3] * j[1];
        h[5] = i[2] * j[3] - i[3] * j[2];

        l[3] = h[0] * k[2] - h[1] * k[1] + h[3] * k[0];
        l[2] = h[0] * k[3] - h[2] * k[1] + h[4] * k[0];
        l[1] = h[1] * k[3] - h[2] * k[2] + h[5] * k[0];
        l[0] = h[3] * k[3] - h[4] * k[2] + h[5] * k[1];
        return l;
    }

    public static double det(double[][] matrix) { //method sig. takes a matrix (two dimensional array), returns determinant.            
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
                        - (h[0] * matrix[2][3] - h[2] * matrix[2][1] + h[4] * matrix[2][0]) * matrix[3][2]
                        + (h[1] * matrix[2][3] - h[2] * matrix[2][2] + h[5] * matrix[2][0]) * matrix[3][1]
                        - (h[3] * matrix[2][3] - h[4] * matrix[2][2] + h[5] * matrix[2][1]) * matrix[3][0];

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

    public static double[] project(double[] p, double[] i, double[] j, double[] k, double[] n) {
        double r = -(p[0] * n[0] + p[1] * n[1] + p[2] * n[2] + p[3] * n[3]) / (n[0] * n[0] + n[1] * n[1] + n[2] * n[2] + n[3] * n[3]);
        double[] p1 = {p[0] + r * n[0], p[1] + r * n[1], p[2] + r * n[2], p[3] + r * n[3]};
        r = -(p1[0] * k[0] + p1[1] * k[1] + p1[2] * k[2] + p1[3] * k[3]) / (k[0] * k[0] + k[1] * k[1] + k[2] * k[2] + k[3] * k[3]);
        p1[0] = p1[0] + r * k[0];
        p1[1] = p1[1] + r * k[1];
        p1[2] = p1[2] + r * k[2];
        p1[3] = p1[3] + r * k[3];
        return p1;
    }

    public static double dotProduct(double[] v1, double[] v2) {
        return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3] * v2[3];
    }

    public static double[] parametrize(double[] p, double[] x, double[] y, double[] k, double[] n) {
        matrixX[0] = p;
        matrixX[1] = y;
        matrixX[2] = k;
        matrixX[3] = n;

        matrixY[0] = x;
        matrixY[1] = p;
        matrixY[2] = k;
        matrixY[3] = n;
        double kx = det(matrixX);
        double ky = det(matrixY);
        return new double[] {kx, ky};
    }

    public static double[] rotateXY(double[] v, double phi) {
        return new double[]{v[0] * Math.cos(phi) + v[1] * Math.sin(phi),
            v[0] * -Math.sin(phi) + v[1] * Math.cos(phi),
            v[2], v[3]};
    }

    public static double[] rotateXZ(double[] v, double phi) {
        return new double[]{v[0] * Math.cos(phi) - v[2] * Math.sin(phi),
            v[1],
            v[0] * Math.sin(phi) + v[2] * Math.cos(phi),
            v[3]};
    }

    public static double[] rotateXW(double[] v, double phi) {
        return new double[]{v[0] * Math.cos(phi) + v[3] * Math.sin(phi),
            v[1], v[2],
            v[0] * -Math.sin(phi) + v[3] * Math.cos(phi)};
    }

    public static double[] rotateYZ(double[] v, double phi) {
        return new double[]{v[0],
            v[1] * Math.cos(phi) + v[2] * Math.sin(phi),
            v[1] * -Math.sin(phi) + v[2] * Math.cos(phi),
            v[3]};
    }

    public static double[] rotateYW(double[] v, double phi) {
        return new double[]{v[0],
            v[1] * Math.cos(phi) + v[3] * Math.sin(phi),
            v[2],
            v[1] * -Math.sin(phi) + v[3] * Math.cos(phi)};
    }

    public static double[] rotateZW(double[] v, double phi) {
        return new double[]{v[0], v[1],
            v[2] * Math.cos(phi) - v[3] * Math.sin(phi),
            v[2] * Math.sin(phi) + v[3] * Math.cos(phi)};
    }
}
