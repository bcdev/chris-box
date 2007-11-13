package org.esa.beam.chris.operators;

/**
 * Atmospheric absorption bands.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
enum AbsorptionBands {

    BAND0(400.0, 440.0),
    BAND1(590.0, 600.0),
    BAND2(630.0, 636.0),
    BAND3(648.0, 658.0),
    BAND4(686.0, 709.0),
    BAND5(792.0, 799.0),
    BAND6(756.0, 775.0), // oxygen
    BAND7(808.0, 840.0), // water vapour
    BAND8(885.0, 985.0), // water vapour
    BAND9(985.0, 1010.0);

    private double minWavelength;
    private double maxWavelength;

    private AbsorptionBands(double minWavelength, double maxWavelength) {
        this.minWavelength = minWavelength;
        this.maxWavelength = maxWavelength;
    }

    public double getMinWavelength() {
        return minWavelength;
    }

    public double getMaxWavelength() {
        return maxWavelength;
    }

    public boolean contains(double wavelength) {
        return wavelength > minWavelength && wavelength < maxWavelength;
    }
}
