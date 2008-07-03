package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class BoaReflectanceCalculator {

    private final double[] lpw;
    private final double[] egl;
    private final double[] sab;
    private final double toaRadianceMultiplier;

    BoaReflectanceCalculator(double[] lpw, double[] egl, double[] sab, double toaRadianceMultiplier) {
        this.lpw = lpw;
        this.egl = egl;
        this.sab = sab;
        this.toaRadianceMultiplier = toaRadianceMultiplier;
    }

    public void calculate(double[] toa, double[] boa) {
        calculate(toa, boa, 0, toa.length);
    }

    public void calculate(double[] toa, double[] boa, int from, int to) {
        for (int i = from; i < to; i++) {
            boa[i] = calculate(i, toa[i]);
        }
    }

    public double calculate(int i, double toa) {
        final double a = Math.PI * (toa * toaRadianceMultiplier - lpw[i]) / egl[i];

        return a / (1.0 + a * sab[i]);
    }
}
