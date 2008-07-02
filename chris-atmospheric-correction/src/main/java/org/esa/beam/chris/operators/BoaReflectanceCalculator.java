package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class BoaReflectanceCalculator {

    private final double[] rtmWavelengths;
    private final double[][] rtmTable;
    private final double toaRadianceMultiplier;

    public BoaReflectanceCalculator(double[] rtmWavelengths, double[][] rtmTable) {
        this(rtmWavelengths, rtmTable, 1.0);
    }

    public BoaReflectanceCalculator(double[] rtmWavelengths, double[][] rtmTable, double toaRadianceMultiplier) {
        this.rtmWavelengths = rtmWavelengths;
        this.rtmTable = rtmTable;
        this.toaRadianceMultiplier = toaRadianceMultiplier;
    }

    public Resampler createResampler(double[] wavelenghts, double[] bandwidths) {
        return new Resampler(rtmWavelengths, wavelenghts, bandwidths, 0.0);
    }

    public Resampler createResampler(double[] wavelenghts, double[] bandwidths, double shift) {
        return new Resampler(rtmWavelengths, wavelenghts, bandwidths, shift);
    }

    public void calculateBoaReflectances(Resampler resampler, double[] toa, double[] boa) {
        calculateBoaReflectances(resampler, toa, boa, 0, toa.length);
    }

    public void calculateBoaReflectances(Resampler resampler, double[] toa, double[] boa, int from, int to) {
        final double[] lpw = new double[toa.length];
        final double[] egl = new double[toa.length];
        final double[] sab = new double[toa.length];

        resampler.resample(rtmTable[ModtranLookupTable.LPW], lpw);
        resampler.resample(rtmTable[ModtranLookupTable.EGL], egl);
        resampler.resample(rtmTable[ModtranLookupTable.SAB], sab);

        for (int i = from; i < to; i++) {
            final double a = Math.PI * (toa[i] * toaRadianceMultiplier - lpw[i]) / egl[i];

            boa[i] = a / (1.0 + a * sab[i]);
        }
    }
}
