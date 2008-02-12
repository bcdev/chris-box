package org.esa.beam.chris.ui;

import javax.swing.table.DefaultTableModel;
import java.awt.Color;

/**
 * Created by Marco Peters.
*
* @author Marco Peters
* @version $Revision:$ $Date:$
*/
class CloudMaskLabelingModel extends DefaultTableModel {
    private static final String[] columnNames = new String[] {
            "Name",
            "Colour",
            "Cloud",
            "Background",
            "Curr. Probability",
            "Init. Probability"
    };
    private static final Class<?>[] columnClasses = new Class<?>[] {
            String.class,
            Color.class,
            Boolean.class,
            Boolean.class,
            Double.class,
            Double.class
    };
    private final CloudClass[] cloudClasses;

    public CloudMaskLabelingModel(CloudClass[] cloudClasses) {
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
        final CloudClass cloudClass = cloudClasses[rowIndex];
        switch(columnIndex) {
        case 0:
            return cloudClass.getName();
        case 1:
            return cloudClass.getColour();
        case 2:
            return cloudClass.isCloud();
        case 3:
            return cloudClass.isBackground();
        case 4:
            return cloudClass.getCurrentProbability();
        case 5:
            return cloudClass.getInitialProbability();
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        final CloudClass cloudClass = cloudClasses[rowIndex];
        switch(columnIndex) {
            case 0:
                cloudClass.setName((String)value);
                break;
            case 1:
                cloudClass.setColour((Color) value);
                break;
            case 2:
                cloudClass.setCloud((Boolean)value);
                break;
            case 3:
                cloudClass.setBackground((Boolean)value);
                break;
            default:
                return;
        }
        fireTableRowsUpdated(rowIndex, rowIndex);
    }
}
