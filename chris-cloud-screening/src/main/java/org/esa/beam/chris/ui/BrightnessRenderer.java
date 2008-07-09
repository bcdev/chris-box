package org.esa.beam.chris.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.text.NumberFormat;

/**
 * User: Marco Peters
 * Date: 15.05.2008
 */
class BrightnessRenderer extends DefaultTableCellRenderer {

    private final NumberFormat formatter;

    BrightnessRenderer() {
        setHorizontalAlignment(JLabel.RIGHT);
        formatter = NumberFormat.getInstance();
        formatter.setMinimumIntegerDigits(1);
        formatter.setMaximumIntegerDigits(1);
        formatter.setMinimumFractionDigits(3);
        formatter.setMaximumFractionDigits(3);
    }

    @Override
    public void setValue(Object value) {
        setText(value == null ? "" : formatter.format(value));
    }
}
