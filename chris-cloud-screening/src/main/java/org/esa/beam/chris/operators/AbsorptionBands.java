package org.esa.beam.chris.operators;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
enum AbsorptionBands {
    LOW_1(590.0, 600.0, "Low atmospheric absorption"),
    LOW_2(630.0, 636.0, "Low atmospheric absorption"),
    LOW_3(648.0, 658.0, "Low atmospheric absorption"),
    LOW_4(686.0, 709.0, "Low atmospheric absorption"),
    LOW_5(716.0, 741.0, "Low atmospheric absorption"),
    LOW_6(792.0, 799.0, "Low atmospheric absorption"),
    OXYGEN(756.0, 775.0, "Oxygen"),
    WATER_VAPOUR_1(808.0, 840.0, "Water vapour"),
    WATER_VAPOUR_2(885.0, 985.0, "Water vapour"),
    SENSOR_NOISE(400.0, 440.0, "Sensor noise"),
    CALIBRATION_ERRORS(985.0, 1010.0, "Calibration errors");

    private double minWavelength;
    private double maxWavelength;
    private String description;

    private AbsorptionBands(double minWavelength, double maxWavelength, String description) {
        this.minWavelength = minWavelength;
        this.maxWavelength = maxWavelength;
        this.description = description;
    }

    public double getMinWavelength() {
        return minWavelength;
    }

    public double getMaxWavelength() {
        return maxWavelength;
    }

    public String getDescription() {
        return description;
    }
}
