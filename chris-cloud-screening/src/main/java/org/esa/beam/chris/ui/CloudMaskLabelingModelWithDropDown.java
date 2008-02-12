package org.esa.beam.chris.ui;

import javax.swing.table.DefaultTableModel;
import java.awt.Color;

/**
 * Created by Marco Peters.
*
* @author Marco Peters
* @version $Revision:$ $Date:$
*/
class CloudMaskLabelingModelWithDropDown extends DefaultTableModel {
    private static final String[] columnNames = new String[] {
            "Name",
            "Colour",
            "Curr. Probability",
            "Init. Probability"
    };
    private static final Class<?>[] columnClasses = new Class<?>[] {
            String.class,
            Color.class,
            Double.class,
            Double.class
    };
    private final CloudClass[] cloudClasses;

    public CloudMaskLabelingModelWithDropDown(CloudClass[] cloudClasses) {
        super(columnNames, cloudClasses.length);
        this.cloudClasses = cloudClasses;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // not the last two columns
        return columnIndex < getColumnCount() - 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch(columnIndex) {
        case 0:
            return cloudClasses[rowIndex].getName();
        case 1:
            return cloudClasses[rowIndex].getColour();
        case 2:
            return cloudClasses[rowIndex].getCurrentProbability();
        case 3:
            return cloudClasses[rowIndex].getInitialProbability();
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {     
        switch(columnIndex) {
            case 0:
                cloudClasses[rowIndex].setName((String)value);
                break;
            case 1:
                cloudClasses[rowIndex].setColour((Color) value);
                break;
            default:
                return;
        }
        // ????? needed ???? TODO
        fireTableRowsUpdated(rowIndex, rowIndex);
    }
}