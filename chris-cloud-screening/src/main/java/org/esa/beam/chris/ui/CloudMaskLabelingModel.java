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
    private final ClusterClass[] clusterClasses;

    public CloudMaskLabelingModel(ClusterClass[] clusterClasses) {
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
        final ClusterClass clusterClass = clusterClasses[rowIndex];
        switch(columnIndex) {
        case 0:
            return clusterClass.getName();
        case 1:
            return clusterClass.getColour();
        case 2:
            return clusterClass.isCloud();
        case 3:
            return clusterClass.isBackground();
        case 4:
            return clusterClass.getCurrentProbability();
        case 5:
            return clusterClass.getInitialProbability();
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        final ClusterClass clusterClass = clusterClasses[rowIndex];
        switch(columnIndex) {
            case 0:
                clusterClass.setName((String)value);
                break;
            case 1:
                clusterClass.setColour((Color) value);
                break;
            case 2:
                clusterClass.setCloud((Boolean)value);
                break;
            case 3:
                clusterClass.setBackground((Boolean)value);
                break;
            default:
                return;
        }
        fireTableRowsUpdated(rowIndex, rowIndex);
    }
}
