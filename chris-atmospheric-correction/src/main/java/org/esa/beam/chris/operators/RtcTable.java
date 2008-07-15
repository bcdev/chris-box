package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class RtcTable {
    private final double[] wavelengths;
    private final double[] lpw;
    private final double[] egl;
    private final double[] sab;
    private final double[] rat;

    RtcTable(double[] wavelengths, double[] lpw, double[] egl, double[] sab, double[] rat) {
        this.wavelengths = wavelengths;

        this.lpw = lpw;
        this.egl = egl;
        this.sab = sab;
        this.rat = rat;
    }

    public final double[] getWavelengths() {
        return wavelengths;
    }

    public final int getWavelengthCount() {
        return wavelengths.length;
    }

    public final double[] getLpw() {
        return lpw;
    }

    public final double[] getEgl() {
        return egl;
    }

    public final double[] getSab() {
        return sab;
    }

    public double[] getRat() {
        return rat;
    }

    public final double getLpw(int i) {
        return lpw[i];
    }

    public final double getEgl(int i) {
        return egl[i];
    }

    public final double getSab(int i) {
        return sab[i];
    }

    public double getRat(int i) {
        return rat[i];
    }
}
