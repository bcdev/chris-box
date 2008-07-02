package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class BoaReflectanceCalculatorOld {
    private final double toarMultiplier;

    public BoaReflectanceCalculatorOld(double toaRadianceMultiplier) {
        this.toarMultiplier = toaRadianceMultiplier;
    }

    public double calculateBoaReflectance(double toaRadiance, double lpw, double egl, double sab) {
        final double a = Math.PI * (toaRadiance * toarMultiplier - lpw) / egl;

        return a / (1.0 + a * sab);
    }
}
