/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.awt.Color;
import java.text.MessageFormat;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
class LabelingFormModel {
    private final TableModel tableModel;
    private final PropertyContainer propertyContainer;

    LabelingFormModel(LabelingContext labelingContext) {
        tableModel = new TableModel(labelingContext);

        propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(new Property(new PropertyDescriptor("probabilistic", boolean.class),
                                               new DefaultPropertyAccessor()));
        propertyContainer.addProperty(new Property(new PropertyDescriptor("probabilisticEnabled", boolean.class),
                                               new DefaultPropertyAccessor()));

        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getColumn() == TableModel.CLOUDY_COLUMN) {
                    try {
                        if (isAnyCloudFlagSet()) {
                            propertyContainer.getProperty("probabilisticEnabled").setValue(true);
                        } else {
                            propertyContainer.getProperty("probabilisticEnabled").setValue(false);
                            propertyContainer.getProperty("probabilistic").setValue(false);
                        }
                    } catch (ValidationException ignored) {
                        // never happens
                    }
                }
            }

            private boolean isAnyCloudFlagSet() {
                for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); ++rowIndex) {
                    if (tableModel.isCloud(rowIndex)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    TableModel getTableModel() {
        return tableModel;
    }

    PropertyContainer getPropertyContainer() {
        return propertyContainer;
    }

    boolean[] getCloudyFlags() {
        return tableModel.getCloudyFlags();
    }

    boolean[] getIgnoreFlags() {
        return tableModel.getIgnoreFlags();
    }

    public boolean isProbabilistic() {
        return (Boolean) propertyContainer.getValue("probabilistic");
    }

    static class TableModel extends AbstractTableModel {

        static final int LABEL_COLUMN = 0;
        static final int COLOR_COLUMN = 1;
        static final int CLOUDY_COLUMN = 2;
        static final int IGNORE_COLUMN = 3;
        static final int BRIGHTNESS_COLUMN = 4;
        static final int OCCURRENCE_COLUMN = 5;

        private static final String[] COLUMN_NAMES = new String[]{
                "Label", "Colour", "Cloud", "Ignore", "Brightness", "Occurrence"
        };

        private static final Class<?>[] COLUMN_TYPES = new Class<?>[]{
                String.class, Color.class, Boolean.class, Boolean.class, Double.class, Double.class
        };

        private final LabelingContext context;
        private final int rowCount;

        private final boolean[] cloudyFlags;
        private final boolean[] ignoreFlags;
        private final double[] brightnessValues;
        private final double[] occurrenceValues;

        TableModel(final LabelingContext context) {
            this.context = context;
            rowCount = context.getClusterCount();

            cloudyFlags = new boolean[rowCount];
            ignoreFlags = new boolean[rowCount];
            brightnessValues = new double[rowCount];
            occurrenceValues = new double[rowCount];

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    context.computeBrightnessValues(brightnessValues, ignoreFlags);
                    context.computeOccurrenceValues(occurrenceValues);
                }
            });
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == LABEL_COLUMN
                    || columnIndex == COLOR_COLUMN
                    || columnIndex == CLOUDY_COLUMN
                    || columnIndex == IGNORE_COLUMN;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return COLUMN_TYPES[columnIndex];
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public int getRowCount() {
            return rowCount;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case LABEL_COLUMN:
                    return getLabel(rowIndex);
                case COLOR_COLUMN:
                    return getColor(rowIndex);
                case CLOUDY_COLUMN:
                    return isCloud(rowIndex);
                case IGNORE_COLUMN:
                    return isIgnored(rowIndex);
                case BRIGHTNESS_COLUMN:
                    return getBrightness(rowIndex);
                case OCCURRENCE_COLUMN:
                    return getOccurrence(rowIndex);
            }

            return null;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case LABEL_COLUMN:
                    setLabel(rowIndex, (String) value);
                    return;
                case COLOR_COLUMN:
                    setColor(rowIndex, (Color) value);
                    return;
                case CLOUDY_COLUMN:
                    setCloud(rowIndex, (Boolean) value);
                    return;
                case IGNORE_COLUMN:
                    setIgnored(rowIndex, (Boolean) value);
                    return;
            }

            throw new IllegalArgumentException(MessageFormat.format("Invalid column index [{0}]", columnIndex));
        }

        private String getLabel(int rowIndex) {
            return context.getLabel(rowIndex);
        }

        private void setLabel(int rowIndex, String label) {
            context.setLabel(rowIndex, label);
            fireTableCellUpdated(rowIndex, LABEL_COLUMN);
        }

        private Color getColor(int rowIndex) {
            return context.getColor(rowIndex);
        }

        private void setColor(int rowIndex, Color color) {
            context.setColor(rowIndex, color);
            fireTableCellUpdated(rowIndex, COLOR_COLUMN);
        }

        private boolean isCloud(int rowIndex) {
            return cloudyFlags[rowIndex];
        }

        private void setCloud(int rowIndex, boolean b) {
            cloudyFlags[rowIndex] = b;
            if (isCloud(rowIndex) && isIgnored(rowIndex)) {
                setIgnored(rowIndex, false);
            }
            fireTableCellUpdated(rowIndex, CLOUDY_COLUMN);
        }

        private boolean isIgnored(int rowIndex) {
            return ignoreFlags[rowIndex];
        }

        private void setIgnored(final int rowIndex, final boolean b) {
            ignoreFlags[rowIndex] = b;
            if (isCloud(rowIndex) && isIgnored(rowIndex)) {
                setCloud(rowIndex, false);
            }
            fireTableCellUpdated(rowIndex, IGNORE_COLUMN);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    context.regenerateClassView(ignoreFlags);
                    context.computeBrightnessValues(brightnessValues, ignoreFlags);
                    context.computeOccurrenceValues(occurrenceValues);
                    for (int k = 0; k < rowCount; ++k) {
                        fireTableCellUpdated(k, BRIGHTNESS_COLUMN);
                        fireTableCellUpdated(k, OCCURRENCE_COLUMN);
                    }
                }
            });
        }

        private double getBrightness(int rowIndex) {
            return brightnessValues[rowIndex];
        }

        private Object getOccurrence(int rowIndex) {
            return occurrenceValues[rowIndex];
        }

        private boolean[] getCloudyFlags() {
            return cloudyFlags.clone();
        }

        private boolean[] getIgnoreFlags() {
            return ignoreFlags.clone();
        }
    }
}
