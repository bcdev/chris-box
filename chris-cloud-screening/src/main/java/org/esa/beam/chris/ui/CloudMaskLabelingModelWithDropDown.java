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
    private final ClusterClass[] clusterClasses;

    public CloudMaskLabelingModelWithDropDown(ClusterClass[] clusterClasses) {
        super(columnNames, clusterClasses.length);
        this.clusterClasses = clusterClasses;
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
            return clusterClasses[rowIndex].getName();
        case 1:
            return clusterClasses[rowIndex].getColour();
        case 2:
            return clusterClasses[rowIndex].getCurrentProbability();
        case 3:
            return clusterClasses[rowIndex].getInitialProbability();
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {     
        switch(columnIndex) {
            case 0:
                clusterClasses[rowIndex].setName((String)value);
                break;
            case 1:
                clusterClasses[rowIndex].setColour((Color) value);
                break;
            default:
                return;
        }
        // ????? needed ???? TODO
        fireTableRowsUpdated(rowIndex, rowIndex);
    }
}