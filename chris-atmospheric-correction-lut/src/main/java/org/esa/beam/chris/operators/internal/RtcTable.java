package org.esa.beam.chris.operators.internal;

/**
 * Table storing radiative transfer calculations.
 *
 * @author Ralf Quast
 * @version $Revision: 2703 $ $Date: 2008-07-15 11:42:48 +0200 (Di, 15 Jul 2008) $
 * @since BEAM 4.2
 */
public class RtcTable {
    private final double[] lpw;
    private final double[] egl;
    private final double[] sab;
    private final double[] rat;

    public RtcTable(double[] lpw, double[] egl, double[] sab, double[] rat) {
        this.lpw = lpw;
        this.egl = egl;
        this.sab = sab;
        this.rat = rat;
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

    public final double getRat(int i) {
        return rat[i];
    }
}
