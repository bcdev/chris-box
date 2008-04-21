/*
 * some of linera algebra utilities
 */

package com.kutsyy.util;

//import Jama.*;
/**
 *  La class contains some helpful linear algebra functions that <BR>
 *  requre <A href="http://math.nist.gov/javanumerics/jama/">JAMA package</A>
 *  <BR>
 *  Created by <A href="http://www.kutsyy.com">Vadim Kutsyy</A> <BR>
 *
 *
 *@author     <A href="http://www.kutsyy.com">Vadim Kutsyy</A>
 *@created    December 2, 2000
 */
public final class La {

    /**
     *  Get a subarray. Creation date: (2/4/00 2:13:25 PM)
     *
     *@param  A       array
     *@param  iFirst  start of fist dimention ofsubarray
     *@param  iLast   end of fist dimention ofsubarray
     *@param  jFirst  start of second dimention ofsubarray
     *@param  jLast   end of second dimention ofsubarray
     *@return         double[][] A[i0:i1][j0:j1]
     */
    public static double[][] getArray(double[][] A, int iFirst, int iLast, int jFirst,
            int jLast) {
        double[][] X = new double[iLast - iFirst + 1][jLast - jFirst + 1];
        for (int i = iFirst; i <= iLast; i++) {
            for (int j = jFirst; j <= jLast; j++) {
                X[i - iFirst][j - jFirst] = A[i][j];
            }
        }
        return X;
    }


    /**
     *  Get a subarray.
     *
     *@param  A       array
     *@param  iFirst  start of subarray
     *@param  iLast   end of subarray
     *@return         double A[i0:i1][i0:i1]
     */
    public static double[][] getArray(double[][] A, int iFirst, int iLast) {
        return getArray(A, iFirst, iLast, iFirst, iLast);
    }


    /**
     *  Get a subarray.
     *
     *@param  A       array
     *@param  iFirst  start of subarray
     *@param  iLast   end of subarray
     *@return         A[i0:i1]
     */
    public static double[] getArray(double[] A, int iFirst, int iLast) {
        double[] X = new double[iLast - iFirst + 1];
        for (int i = iFirst; i <= iLast; i++) {
            X[i - iFirst] = A[i];
        }
        return X;
    }


    /**
     *  Get sub array
     *
     *@param  A      array
     *@param  index  index of fields for subarray
     *@return        A[index]
     */
    public static double[] getArray(double[] A, int[] index) {
        double[] B = new double[index.length];
        for (int k = 0; k < index.length; k++) {
            B[k] = A[index[k]];
        }
        return B;
    }


    /**
     *  Returns vector b[]=a[], except for x[i]=x
     *
     *@param  a  input vector
     *@param  i  position of the change
     *@param  x  new value for replasment
     *@return    output vector
     */
    public static double[] changeI(double[] a, int i, double x) {
        double[] b = (double[]) a.clone();
        b[i] = x;
        return b;
    }


    /**
     *  Returns vector b[]=a[], except for x[i]=x
     *
     *@param  a  input vector
     *@param  i  position of the change
     *@param  x  new value for replasment
     *@return    output vector
     */
    public static int[] changeI(int[] a, int i, int x) {
        int[] b = (int[]) a.clone();
        b[i] = x;
        return b;
    }


    /**
     *  Cholestky decomposition.
     *
     *@param  A  Symetric Positive def matrix
     *@return    lower deomposed matrix
     *@see       #choldc(double[][], double[])
     */
    public static double[][] choldc(double[][] A) {
        double[][] a = new double[A.length][];
        for (int i = 0; i < A.length; i++) {
            a[i] = (double[]) A[i].clone();
        }
        double[] p = new double[a.length];
        choldc(a, p);
        for (int i = 0; i < a.length; i++) {
            a[i][i] = p[i];
            for (int j = i + 1; j < a.length; j++) {
                a[i][j] = 0;
            }
        }
        ;
        return a;
    }


    /**
     *  Inverse of Cholestky decomposition.
     *
     *@param  A  Symetric Positive def matrix
     *@return    inverse of lower deomposed matrix
     *@see       #choldc(double[][], double[])
     */
    public static double[][] choldcsl(double[][] A) {
        double[][] a = new double[A.length][];
        for (int i = 0; i < A.length; i++) {
            a[i] = (double[]) A[i].clone();
        }
        double p[] = new double[A.length];
        choldc(a, p);
        for (int i = 0; i < a.length; i++) {
            a[i][i] = 1 / p[i];
            for (int j = i + 1; j < a.length; j++) {
                double sum = 0;
                for (int k = i; k < j; k++) {
                    sum -= a[j][k] * a[k][i];
                }
                a[j][i] = sum / p[j];
            }
        }
        return a;
    }


    /**
     *  Computation of Determinant of the matrix using Cholevsky decomposition
     *
     *@param  a  A
     *@return    det(A)
     *@see       #choldc(double[][], double[])
     */
    public static double choldet(double[][] a) {
        double c[][] = choldc(a);
        double d = 1;
        for (int i = 0; i < a.length; i++) {
            d *= c[i][i];
        }
        return d * d;
    }


    /**
     *  Computation of Determinant of the matrix using Cholevsky decomposition,
     *  and Cholevsky decomposition
     *
     *@param  a  A
     *@param  c  Cholevsky decomposition of A
     *@return    det(A)
     *@see       #choldc(double[][], double[])
     */
    public static double choldet(double[][] a, double[][] c) {
        c = choldc(a);
        double d = 1;
        for (int i = 0; i < a.length; i++) {
            d *= c[i][i];
        }
        return d * d;
    }


    /**
     *  Matrix inverse using Cholevsky decomposition
     *
     *@param  A  Symetric Positive def matrix
     *@return    inverse of A
     *@see       #choldc(double[][], double[])
     */
    public static double[][] cholsl(double[][] A) {
        double a[][] = choldcsl(A);
        for (int i = 0; i < a.length; i++) {
            for (int j = i + 1; j < a.length; j++) {
                a[i][j] = 0;
            }
        }
        for (int i = 0; i < a.length; i++) {
            a[i][i] *= a[i][i];
            for (int k = i + 1; k < a.length; k++) {
                a[i][i] += a[k][i] * a[k][i];
            }
            for (int j = i + 1; j < a.length; j++) {
                for (int k = j; k < a.length; k++) {
                    a[i][j] += a[k][i] * a[k][j];
                }
            }
        }
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < i; j++) {
                a[i][j] = a[j][i];
            }
        }
        return a;
    }


    /**
     *  Solves Ax=b, using Cholesky decomposition
     *
     *@param  A  A
     *@param  b  b
     *@return    x
     *@see       #choldc(double[][], double[])
     */
    public static double[] cholsl(double[][] A, double[] b) {
        if (A.length != b.length || A[0].length != b.length) {
            throw new IllegalArgumentException("Matrix and Vector dimentions must agree.");
        }
        ;
        double[][] a = new double[A.length][];
        for (int i = 0; i < A.length; i++) {
            a[i] = (double[]) A[i].clone();
        }
        double p[] = new double[A.length];
        choldc(a, p);
        return cholsl(a, p, b);
    }


    /**
     *  Insert element x at the position i;
     *
     *@param  a  input vector [n]
     *@param  i  position
     *@param  x  value of inserted element
     *@return    output vector (a[0:i-1],x,a[i:a.length])
     */
    public static double[] insertI(double[] a, int i, double x) {
        int n = a.length;
        double[] b = new double[n + 1];
        for (int j = 0; j < i; j++) {
            b[j] = a[j];
        }
        for (int j = i; j < n; j++) {
            b[j + 1] = a[j];
        }
        b[i] = x;
        return b;
    }


    /**
     *  Insert element x at the position i;
     *
     *@param  a  input vector [n]
     *@param  i  position
     *@param  x  value of inserted element
     *@return    output vector (a[0:i-1],x,a[i:a.length])
     */
    public static int[] insertI(int[] a, int i, int x) {
        int[] b;
        if (a != null) {
            int n = a.length;
            b = new int[n + 1];
            for (int j = 0; j < i; j++) {
                b[j] = a[j];
            }
            for (int j = i; j < n; j++) {
                b[j + 1] = a[j];
            }
            b[i] = x;
        }
        else {
            b = new int[1];
            b[0] = x;
        }
        return b;
    }


    /**
     *  Find maximum value of the vector
     *
     *@param  a  a
     *@return    max(a)
     */
    public static double max(double[] a) {
        double x = a[0];
        for (int i = 1; i < a.length; i++) {
            if (a[i] > x) {
                x = a[i];
            }
        }
        return x;
    }


    /**
     *  Find minimum value of the vector
     *
     *@param  a  a
     *@return    min(a)
     */
    public static double min(double[] a) {
        double x = a[0];
        for (int i = 1; i < a.length; i++) {
            if (a[i] < x) {
                x = a[i];
            }
        }
        return x;
    }


    /**
     *  Return x[i][j]=a[i][j]-b[i][j] for i,j=0..(n-1)
     *
     *@param  a  a
     *@param  b  b
     *@return    x
     */
    public static double[][] minus(double[][] a, double b[][]) {
        if (a.length != b.length && a[0].length != b[0].length) {
            throw new IllegalArgumentException("Matrox dimentions must agree");
        }
        //double x[][] = (double[][]) a.clone();
        double x[][] = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                x[i][j] = a[i][j];
            }
        }
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                x[i][j] -= b[i][j];
            }
        }
        return x;
    }


    /**
     *  Return x[i]=a[i]-b[i] for i=0..(n-1)
     *
     *@param  a  a
     *@param  b  b
     *@return    x
     */
    public static double[] minus(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Matrox dimentions must agree");
        }
        double x[] = (double[]) a.clone();
        for (int i = 0; i < a.length; i++) {
            x[i] -= b[i];
        }
        return x;
    }


    /**
     *  Returns x=a, exceept for x[i][j]=-a[i][j], i!=j;
     *
     *@param  a  a
     *@param  i  index
     *@return    x
     */
    public final static double[][] negativeI(double[][] a, int i) {
        //double[][] b = (double[][]) a.clone();
        double[][] b = new double[a.length][a[0].length];
        for (int ii = 0; ii < a.length; ii++) {
            for (int j = 0; j < a[0].length; j++) {
                b[ii][j] = a[ii][j];
            }
        }
        for (int j = 0; j < b.length; j++) {
            b[i][j] = -b[i][j];
            b[j][i] = -b[j][i];
        }
        return b;
    }


    /**
     *  Returns x=a, except for x[i]=-a[i];
     *
     *@param  a  a
     *@param  i  index
     *@return    x
     */
    public static double[] negativeI(double[] a, int i) {
        double[] b = (double[]) a.clone();
        b[i] = -b[i];
        return b;
    }


    /**
     *  Return x[i][j]=a[i][j]+b[i][j] for i,j=1..(n-1)
     *
     *@param  a  a
     *@param  b  b
     *@return    x
     */
    public static double[][] plus(double[][] a, double b[][]) {
        if (a.length != b.length && a[0].length != b[0].length) {
            throw new IllegalArgumentException("Matrox dimentions must agree");
        }
        //double x[][] = (double[][]) a.clone();
        double x[][] = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                x[i][j] = a[i][j];
            }
        }
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                x[i][j] += b[i][j];
            }
        }
        return x;
    }


    /**
     *  Return x[i]=a[i]+b[i] for i=1..(n-1)
     *
     *@param  a  a
     *@param  b  b
     *@return    x
     */
    public static double[] plus(double[] a, double b[]) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Matrox dimentions must agree");
        }
        double x[] = (double[]) a.clone();
        for (int i = 0; i < a.length; i++) {
            x[i] += b[i];
        }
        return x;
    }


    /**
     *  Return x[i]=a[i]+b for i=1..(n-1)
     *
     *@param  a  a
     *@param  b  b
     *@return    x
     */
    public static double[] plus(double[] a, double b) {
        double x[] = (double[]) a.clone();
        for (int i = 0; i < a.length; i++) {
            x[i] += b;
        }
        return x;
    }


    /**
     *  Returns x, such that x[j][k]=a[j][k], j,k=i if j,k less then i, ir i+1
     *
     *@param  a  a
     *@param  i  index
     *@return    x
     */
    public static double[][] removeI(double[][] a, int i) {
        if (a.length != a[0].length) {
            throw new IllegalArgumentException("A is not square matrix");
        }
        int n = a.length;
        double[][] b = new double[n - 1][n - 1];
        for (int j = 0; j < i; j++) {
            for (int k = 0; k < i; k++) {
                b[j][k] = a[j][k];
            }
        }
        for (int j = 0; j < i; j++) {
            for (int k = i + 1; k < n; k++) {
                b[j][k - 1] = a[j][k];
                b[k - 1][j] = a[k][j];
            }
        }
        for (int j = i + 1; j < n; j++) {
            for (int k = i + 1; k < n; k++) {
                b[j - 1][k - 1] = a[j][k];
            }
        }
        return b;
    }


    /**
     *  Returns x such that x[j]=a[j], j <i;, x[j]=a[j+1], j <i;
     *
     *@param  a  a
     *@param  i  index
     *@return    x
     */
    public static double[] removeI(double[] a, int i) {
        int n = a.length;
        double[] b = new double[n - 1];
        for (int j = 0; j < i; j++) {
            b[j] = a[j];
        }
        for (int j = i + 1; j < n; j++) {
            b[j - 1] = a[j];
        }
        return b;
    }


    /**
     *  return x, such that x[j]=a[j], j <i;, x[j]=a[j+1], j <i;
     *
     *@param  a  a
     *@param  i  index
     *@return    x
     */
    public static int[] removeI(int[] a, int i) {
        int n = a.length;
        int[] b = new int[n - 1];
        for (int j = 0; j < i; j++) {
            b[j] = a[j];
        }
        for (int j = i + 1; j < n; j++) {
            b[j - 1] = a[j];
        }
        return b;
    }


    /**
     *  return inverse of the matrix. Use <A
     *  href="http://math.nist.gov/javanumerics/jama/">JAMA package</A>
     *
     *@param  A  Matrix
     *@return    Inverse
     */
    public static double[][] solve(double[][] A) {
        //return (new Jama.Matrix(A)).inverse().getArray();
        return (new cern.colt.matrix.linalg.LUDecomposition(new cern.colt.matrix.impl.DenseDoubleMatrix2D(A))).solve(cern.colt.matrix.DoubleFactory2D.dense.identity(A.length)).toArray();
    }


    /**
     *  Solves Ax=b for general A, using <A
     *  href="http://math.nist.gov/javanumerics/jama/">JAMA package</A> ;
     *
     *@param  A  A
     *@param  b  b
     *@return    x
     */
    public static double[] solve(double[][] A, double[] b) {
        return ((new Jama.Matrix(A)).solve(new Jama.Matrix(b, b.length))).getColumnPackedCopy();
    }


    /**
     *  Finds Least square solution of y=b*x
     *
     *@param  x  x
     *@param  y  y
     *@return    b
     */
    public static double[] solveLS(double[][] x, double[] y) {
        double[][] tx = La.t(x);
        return La.times(La.cholsl(La.times(tx, x)), La.times(tx, y));
    }


    /**
     *  Get subarray
     *
     *@param  x    array
     *@param  end  end of subarray
     *@return      x[1:end]
     */
    public static double[] sub(double[] x, int end) {
        return sub(x, 0, end);
    }


    /**
     *  Get subarray
     *
     *@param  x      array
     *@param  start  start of subarray
     *@param  end    end of subarray
     *@return        x[start:end]
     */
    public static double[] sub(double[] x, int start, int end) {
        double[] tmp = new double[end - start + 1];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = x[start + i];
        }
        return tmp;
    }


    /**
     *  Return transpose of the matrix
     *
     *@param  A  A
     *@return    transpose(A)
     */
    public static double[][] t(double[][] A) {
        double X[][] = new double[A[0].length][A.length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < X.length; j++) {
                X[j][i] = A[i][j];
            }
        }
        return X;
    }


    /**
     *  Return transpose of the matrix
     *
     *@param  A  A
     *@return    transpose(A)
     */
    public static int[][] t(int[][] A) {
        int X[][] = new int[A[0].length][A.length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < X.length; j++) {
                X[j][i] = A[i][j];
            }
        }
        return X;
    }


    /**
     *  Linear algebraic matrix multiplication, C = A * A
     *
     *@param  A  matrix
     *@return    product, A * A
     */
    public static double[][] times(double A[][]) {
        //if (A == null)
        //return null;
        //if (A[0].length != A.length)
        //throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        //double X[][] = new double[A.length][A[0].length];
        //for (int i = 0; i < A.length; i++)
        //for (int j = 0; j < A[0].length; j++) {
        //X[i][j] = 0;
        //for (int k = 0; k < A[0].length; k++)
        //X[i][j] += A[i][k] * A[j][k];
        //}
        return times(A, A);
    }


    /**
     *  Linear algebraic matrix multiplication, C = A * B
     *
     *@param  A  Description of Parameter
     *@param  B  Description of Parameter
     *@return    product, A * B
     */
    public static double[][] times(double A[][], double B[][]) {
        //if (A == null || B == null)
        //return null;
        //if (A[0].length != B.length)
        //throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        double X[][] = new double[A.length][B[0].length];
        //for (int i = 0; i < A.length; i++)
        //for (int j = 0; j < B[0].length; j++) {
        //X[i][j] = 0;
        //for (int k = 0; k < A[0].length; k++)
        //X[i][j] += A[i][k] * B[k][j];
        //}
        mult(A, B, X, 1, 0);
        return X;
    }


    /**
     *  Linear algebraic matrix multiplication, C = A * b
     *
     *@param  A  Description of Parameter
     *@param  B  Description of Parameter
     *@return    product, A * b
     */
    public static double[] times(double A[][], double B[]) {
        if (A[0].length != B.length) {
            throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        }
        double X[] = new double[A.length];
        for (int i = 0; i < A.length; i++) {
            X[i] = 0;
            for (int k = 0; k < A[0].length; k++) {
                X[i] += A[i][k] * B[k];
            }
        }
        return X;
    }


    /**
     *  Linear algebraic matrix multiplication, C = A * b
     *
     *@param  a  A
     *@param  b  b
     *@return    product, A * b
     */
    public static double[][] times(double a[][], double b) {
        //double X[][] = (double[][]) A.clone();
        if (a == null) {
            return null;
        }
        double x[][] = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                x[i][j] = a[i][j] * b;
            }
        }
        return x;
    }


    /**
     *  Return A'A, where A is n dimentional vector;
     *
     *@param  A  n dimentional vector;
     *@return    A'A
     */
    public static double times(double[] A) {
        if (A == null || A.length == 0) {
            return 0;
        }
        double t = 0;
        for (int i = 0; i < A.length; i++) {
            t += A[i] * A[i];
        }
        return t;
    }


    /**
     *  Linear algebraic matrix multiplication, C = A * B
     *
     *@param  A  matrix
     *@param  B  matrix
     *@return    product, A * b
     */
    public static double times(double A[], double B[]) {
        if (A.length != B.length) {
            throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        }
        double X = 0;
        for (int i = 0; i < A.length; i++) {
            X += A[i] * B[i];
        }
        return X;
    }


    /**
     *  Linear algebraic matrix multiplication, C = A * b
     *
     *@param  A  matrix
     *@param  b  parameter
     *@return    product, A * b
     */
    public static double[] times(double A[], double b) {
        double X[] = (double[]) A.clone();
        for (int i = 0; i < A.length; i++) {
            X[i] *= b;
        }
        return X;
    }


    /**
     *  Linear algebraic matrix-matrix multiplication; <tt>C = alpha * A x B +
     *  beta*C</tt> . <tt>C[i,j] = alpha*Sum(A[i,k] * B[k,j]) + beta*C[i,j],
     *  k=0..n-1</tt> . <br>
     *  Matrix shapes: <tt>A(m x n), B(n x p), C(m x p)</tt> . <br>
     *  Creation date: (11/22/2000 3:08:04 PM)
     *
     *@param  A                          first source matrix.
     *@param  B                          the second source matrix.
     *@param  C                          the matrix where results are to be
     *      stored. Set this parameter to <tt>null</tt> to indicate that a new
     *      result matrix shall be constructed.
     *@param  alpha                      double
     *@param  beta                       double
     *@throws  IllegalArgumentException  if <tt>B.length != A[0].length</tt> .
     *@throws  IllegalArgumentException  if <tt>C.length != A.length ||
     *      C[0].length != B[0].length</tt> .
     *@throws  IllegalArgumentException  if <tt>A == C || B == C</tt> .
     */
    public final static void mult(double[][] A, double[][] B, double[][] C,
            double alpha, double beta) {
        int m = A.length;
        int n = A[0].length;
        int p = B[0].length;
        if (C == null) {
            C = new double[m][p];
        }
        if (B.length != n) {
            throw new IllegalArgumentException("Matrix2D inner dimensions must agree:");
        }
        if (C.length != m || C[0].length != p) {
            throw new IllegalArgumentException("Incompatibel result matrix: ");
        }
        if (A == C || B == C) {
            throw new IllegalArgumentException("Matrices must not be identical");
        }
        for (int j = p; --j >= 0; ) {
            for (int i = m; --i >= 0; ) {
                double s = 0;
                for (int k = n; --k >= 0; ) {
                    s += A[i][k] * B[k][j];
                }
                C[i][j] = alpha * s + beta * C[i][j];
            }
        }
    }


    /**
     *  Returns sum of array
     *
     *@param  x  array
     *@return    sum(x)
     */
    public static double sum(double[] x) {
        double s = 0;
        if (x != null) {
            for (int i = 0; i < x.length; i++) {
                s += x[i];
            }
        }
        return s;
    }


    /**
     *  main method for Cholestky decomposition.
     *
     *@param  a   matrix
     *@param  p   vector of resulting diag of a
     *@author:    <Vadum Kutsyy, kutsyy@hotmail.com>
     */
    private static void choldc(double[][] a, double[] p) {
        int n = a.length;
        if (a[0].length != n) {
            throw new IllegalArgumentException("a is not square matrix");
        }
        if (n != p.length) {
            throw new IllegalArgumentException(" Matrix dimentions must agree");
        }
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                double sum = a[i][j];
                for (int k = i - 1; k >= 0; k--) {
                    sum -= a[i][k] * a[j][k];
                }
                if (i == j) {
                    if (sum <= 0) {
                        throw new IllegalArgumentException("a is not positive definite");
                    }
                    p[i] = Math.sqrt(sum);
                }
                else {
                    a[j][i] = sum / p[i];
                }
            }
        }
    }


    /**
     *  Internal method for Cholestky decomposition.
     *
     *@param  b   double[]
     *@param  a   double[]
     *@param  p   double[]
     *@return     double[]
     *@author:    <Vadum Kutsyy, kutsyy@hotmail.com>
     */
    private static double[] cholsl(double[][] a, double[] p, double[] b) {
        if (a.length != b.length && a.length != p.length) {
            throw new IllegalArgumentException("Matrix and Vector dimentions must agree.");
        }
        ;
        double x[] = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            double sum = b[i];
            for (int k = i - 1; k >= 0; k--) {
                sum -= a[i][k] * x[k];
            }
            x[i] = sum / p[i];
        }
        for (int i = (a.length - 1); i >= 0; i--) {
            double sum = x[i];
            for (int k = i + 1; k < a.length; k++) {
                sum -= a[k][i] * x[k];
            }
            x[i] = sum / p[i];
        }
        return x;
    }
}

