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
    
    private final double toaScaling;

    Calculator(double[] lpw, double[] egl, double[] sab, double toaScaling) {
        this.lpw = lpw;
        this.egl = egl;
        this.sab = sab;
        this.toaScaling = toaScaling;
    }

    public void calculateBoaReflectances(double[] toa, double[] boa) {
        calculateBoaReflectances(toa, boa, 0, toa.length);
    }

    public void calculateBoaReflectances(double[] toa, double[] boa, int from, int to) {
        for (int i = from; i < to; i++) {
            boa[i] = getBoaReflectance(i, toa[i]);
        }
    }

    public double getBoaReflectance(int i, double toa) {
        final double a = Math.PI * (toa * toaScaling - lpw[i]) / egl[i];

        return a / (1.0 + a * sab[i]);
    }

    public void calculateToaRadiances(double[] boa, double[] toa) {
        calculateToaRadiances(boa, toa, 0, boa.length);
    }

    public void calculateToaRadiances(double[] boa, double[] toa, int from, int to) {
        for (int i = from; i < to; i++) {
            toa[i] = getToaRadiance(i, boa[i]);
        }
    }

    public double getToaRadiance(int i, double boa) {
        return (lpw[i] + boa * (egl[i] / (Math.PI * (1.0 - sab[i] * boa)))) / toaScaling;
    }
}
