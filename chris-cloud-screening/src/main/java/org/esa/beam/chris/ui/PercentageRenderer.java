package org.esa.beam.chris.ui;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JLabel;
import java.text.NumberFormat;

/**
 * User: Marco Peters
* Date: 15.05.2008
*/
class PercentageRenderer extends DefaultTableCellRenderer {

    private final NumberFormat formatter;

    PercentageRenderer() {
        setHorizontalAlignment(JLabel.RIGHT);
        formatter = NumberFormat.getPercentInstance();
        formatter.setMinimumFractionDigits(1);
        formatter.setMaximumFractionDigits(3);
    }

    @Override
    public void setValue(Object value) {
        setText(value == null ? "" : formatter.format(value));
    }
}
