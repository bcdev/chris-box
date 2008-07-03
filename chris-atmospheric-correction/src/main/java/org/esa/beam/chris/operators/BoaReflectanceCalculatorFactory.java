package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class BoaReflectanceCalculatorFactory {

    private final double[] rtmWavelengths;
    private final double[][] rtmTable;
    private final int day;

    public BoaReflectanceCalculatorFactory(double[] rtmWavelengths, double[][] rtmTable, int day) {
        this.rtmWavelengths = rtmWavelengths;
        this.rtmTable = rtmTable;
        this.day = day;
    }

    public BoaReflectanceCalculator createCalculator(double[] wavelengths, double[] bandwidths) {
        return createCalculator(wavelengths, bandwidths, 0.0);
    }

    public BoaReflectanceCalculator createCalculator(double[] wavelengths, double[] bandwidths, double shift) {
        final Resampler resampler = new Resampler(rtmWavelengths, wavelengths, bandwidths, shift);

        final double[] lpw = new double[wavelengths.length];
        final double[] egl = new double[wavelengths.length];
        final double[] sab = new double[wavelengths.length];

        resampler.resample(rtmTable[ModtranLookupTable.LPW], lpw);
        resampler.resample(rtmTable[ModtranLookupTable.EGL], egl);
        resampler.resample(rtmTable[ModtranLookupTable.SAB], sab);

        return new BoaReflectanceCalculator(lpw, egl, sab, 1.0E-3 / solarIrradianceCorrectionFactor(day));
    }

    public BoaReflectanceCalculator createCalculator(double[] wavelengths, double[] bandwidths, double[] lpwCor) {
        final Resampler resampler = new Resampler(rtmWavelengths, wavelengths, bandwidths);

        final double[] lpw = new double[wavelengths.length];
        final double[] egl = new double[wavelengths.length];
        final double[] sab = new double[wavelengths.length];

        resampler.resample(rtmTable[ModtranLookupTable.LPW], lpw);
        resampler.resample(rtmTable[ModtranLookupTable.EGL], egl);
        resampler.resample(rtmTable[ModtranLookupTable.SAB], sab);

        for (int i = 0; i < lpw.length; i++) {
            lpw[i] -= lpwCor[i];
        }

        return new BoaReflectanceCalculator(lpw, egl, sab, 1.0E-3 / solarIrradianceCorrectionFactor(day));
    }

    /**
     * Returns the correction factor for the solar irradiance due to the elliptical
     * orbit of the Sun.
     *
     * @param day the day of year.
     *
     * @return the correction factor.
     */
    private static double solarIrradianceCorrectionFactor(int day) {
        final double d = 1.0 - 0.01673 * Math.cos(Math.toRadians(0.9856 * (day - 4)));

        return 1.0 / (d * d);
    }
}
