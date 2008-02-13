package org.esa.beam.chris.ui;

import java.awt.Color;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class ClusterClass {

    private String name;
    private Color colour;
    private boolean cloud;
    private boolean background;
    private double currentProbability;


    private final double initialProbability;

    public ClusterClass(String name, Color colour, double initialProbability) {
        this.name = name;
        this.colour = colour;
        this.initialProbability = initialProbability;
        this.currentProbability = initialProbability;
        cloud = false;
        background = false;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setColour(Color colour) {
        this.colour = colour;
    }

    public Color getColour() {
        return colour;
    }

    public void setCloud(boolean cloud) {
        this.cloud = cloud;
        if (this.cloud) {
            background = false;
        }
    }

    public boolean isCloud() {
        return cloud;
    }

    public void setBackground(boolean background) {
        this.background = background;
        if (this.background) {
            cloud = false;
        }
    }

    public boolean isBackground() {
        return background;
    }

    public void setCurrentProbability(double currentProbability) {
        this.currentProbability = currentProbability;
    }

    public double getCurrentProbability() {
        return currentProbability;
    }

    public double getInitialProbability() {
        return initialProbability;
    }
}
