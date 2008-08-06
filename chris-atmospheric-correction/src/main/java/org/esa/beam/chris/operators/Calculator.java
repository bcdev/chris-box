package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class Calculator {

    private final double[] lpw;
    private final double[] egl;
    private final double[] sab;
    private final double[] rat;
    
    private final double toaScaling;

    Calculator(double[] lpw, double[] egl, double[] sab, double[] rat, double toaScaling) {
        this.lpw = lpw;
        this.egl = egl;
        this.sab = sab;
        this.rat = rat;
        this.toaScaling = toaScaling;
    }

    public void calculateBoaReflectances(double[] toa, double[] rho) {
        calculateBoaReflectances(toa, rho, 0, toa.length);
    }

    public void calculateBoaReflectances(double[] toa, double[] rho, int from, int to) {
        for (int i = from; i < to; ++i) {
            rho[i] = getBoaReflectance(i, toa[i]);
        }
    }

    public double getBoaReflectance(int i, double toa) {
        final double a = Math.PI * (toa * toaScaling - lpw[i]) / egl[i];

        return a / (1.0 + a * sab[i]);
    }

    public void calculateToaRadiances(double[] rho, double[] toa) {
        calculateToaRadiances(rho, toa, 0, rho.length);
    }

    public void calculateToaRadiances(double[] rho, double[] toa, int from, int to) {
        for (int i = from; i < to; ++i) {
            toa[i] = getToaRadiance(i, rho[i]);
        }
    }

    public double getToaRadiance(int i, double rho) {
        return (lpw[i] + rho * (egl[i] / (Math.PI * (1.0 - sab[i] * rho)))) / toaScaling;
    }

    public double getAdjacencyCorrection(int i, double rho, double ave) {
        return (rho - ave) * rat[i];
    }
}
