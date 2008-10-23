package org.esa.beam.chris.ui;

import org.esa.beam.chris.operators.internal.Clusterer;
import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.cluster.ProbabilityCalculator;

import javax.media.jai.Histogram;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.HistogramDescriptor;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.text.MessageFormat;
import java.util.Arrays;

class LabelingTableModel extends AbstractTableModel {

    static final int LABEL_COLUMN = 0;
    static final int COLOR_COLUMN = 1;
    static final int CLOUD_COLUMN = 2;
    static final int IGNORED_COLUMN = 3;
    static final int BRIGHTNESS_COLUMN = 4;
    static final int OCCURRENCE_COLUMN = 5;

    private static final String[] COLUMN_NAMES = new String[]{
            "Label", "Colour", "Cloud", "Ignore", "Brightness", "Occurrence"
    };

    private static final Class<?>[] COLUMN_TYPES = new Class<?>[]{
            String.class, Color.class, Boolean.class, Boolean.class, Double.class, Double.class
    };

    private final LabelingContext context;
    private final double[] brightness;
    private final double[] occurrence;
    private final int rowCount;

    LabelingTableModel(LabelingContext context) {
        this.context = context;
        rowCount = context.getClusters().length;

        brightness = new double[rowCount];
        occurrence = new double[rowCount];

        new Thread(new Runnable() {
            @Override
            public void run() {
                resetInfoColumns();
            }
        }).start();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == LABEL_COLUMN
                || columnIndex == COLOR_COLUMN
                || columnIndex == CLOUD_COLUMN
                || columnIndex == IGNORED_COLUMN;
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
            case CLOUD_COLUMN:
                return isCloud(rowIndex);
            case IGNORED_COLUMN:
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
            case CLOUD_COLUMN:
                setCloud(rowIndex, (Boolean) value);
                return;
            case IGNORED_COLUMN:
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
        return context.isCloud(rowIndex);
    }

    private void setCloud(int rowIndex, boolean cloud) {
        context.setCloud(rowIndex, cloud);
        if (context.isCloud(rowIndex) && context.isIgnored(rowIndex)) {
            setIgnored(rowIndex, false);
        }
        fireTableCellUpdated(rowIndex, CLOUD_COLUMN);
    }

    private boolean isIgnored(int rowIndex) {
        return context.isIgnored(rowIndex);
    }

    private void setIgnored(final int rowIndex, final boolean invalid) {
        context.setIgnored(rowIndex, invalid);
        if (context.isCloud(rowIndex) && context.isIgnored(rowIndex)) {
            setCloud(rowIndex, false);
        }
        fireTableCellUpdated(rowIndex, IGNORED_COLUMN);

        new Thread(new Runnable() {
            @Override
            public void run() {
                context.recomputeClassificationImage();
                resetInfoColumns();
            }
        }).start();
    }

    private double getBrightness(int rowIndex) {
        return brightness[rowIndex];
    }

    private Object getOccurrence(int rowIndex) {
        return occurrence[rowIndex];
    }

    private void resetInfoColumns() {
        recomputeBrightnessColumn();
        recomputeOccurrenceColumn();
        for (int k = 0; k < rowCount; ++k) {
            fireTableCellUpdated(k, BRIGHTNESS_COLUMN);
            fireTableCellUpdated(k, OCCURRENCE_COLUMN);
        }
    }

    private void recomputeBrightnessColumn() {
        final IndexFilter indexFilter = new IndexFilter() {
            @Override
            public boolean accept(int index) {
                return !context.isIgnored(index);
            }
        };

        final EMCluster[] clusters = context.getClusters();
        final double[] sums = new double[clusters.length];
        final ProbabilityCalculator pc = Clusterer.createProbabilityCalculator(clusters);
        Arrays.fill(brightness, 0.0);

        for (final EMCluster cluster : clusters) {
            final double[] posteriors = new double[clusters.length];
            pc.calculate(cluster.getMean(), posteriors, indexFilter);

            for (int k = 0; k < clusters.length; ++k) {
                brightness[k] += cluster.getMean(0) * posteriors[k];
                sums[k] += posteriors[k];
            }
        }
        for (int k = 0; k < clusters.length; ++k) {
            if (sums[k] > 0.0) {
                brightness[k] /= sums[k];
            }
        }
    }

    private void recomputeOccurrenceColumn() {
        final RenderedImage image = context.getClassificationImage();
        final double[] min = {0.0};
        final double[] max = {rowCount};
        final int[] binCount = {rowCount};
        final RenderedOp op = HistogramDescriptor.create(image, null, 2, 2, binCount, min, max, null);
        final Histogram histogram = (Histogram) op.getProperty("histogram");

        final int totalCounts = image.getWidth() * image.getHeight();
        final int[] histogramCounts = histogram.getBins(0);

        for (int k = 0; k < rowCount; ++k) {
            occurrence[k] = (4.0 * histogramCounts[k]) / totalCounts;
        }
    }
}
