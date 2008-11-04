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

import com.bc.ceres.binding.swing.BindingContext;
import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
class LabelingForm extends JPanel {

    private final JTable table;
    private final JCheckBox checkBox;

    LabelingForm(LabelingFormModel formModel) {
        table = new JTable(formModel.getTableModel());

        table.setDefaultRenderer(Color.class, createColorRenderer());
        table.setDefaultEditor(Color.class, createColorEditor());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(LabelingFormModel.TableModel.BRIGHTNESS_COLUMN).setCellRenderer(
                createBrightnessRenderer());
        table.getColumnModel().getColumn(LabelingFormModel.TableModel.OCCURRENCE_COLUMN).setCellRenderer(
                createOccurrenceRenderer());

        final JScrollPane tablePane = new JScrollPane(table);
        tablePane.getViewport().setPreferredSize(table.getPreferredSize());
        final JPanel tablePanel = new JPanel(new BorderLayout(2, 2));
        tablePanel.add(tablePane, BorderLayout.CENTER);
        tablePanel.setBorder(BorderFactory.createTitledBorder("Cloud Labeling"));

        checkBox = new JCheckBox("Calculate probabilistic cloud mask", false);
        checkBox.setToolTipText("If selected, a probabilistic cloud mask is calculated");

        final BindingContext bc = new BindingContext(formModel.getValueContainer());
        bc.bind("probabilistic", checkBox);
        bc.bindEnabledState("probabilistic", true, "probabilisticEnabled", true);

        final JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkBoxPanel.add(checkBox);
        checkBoxPanel.setBorder(BorderFactory.createTitledBorder("Processing Parameters"));

        setLayout(new BorderLayout(4, 4));
        add(tablePanel, BorderLayout.CENTER);
        add(checkBoxPanel, BorderLayout.SOUTH);
    }

    void prepareHide() {
        // todo - implement
    }

    void prepareShow() {
        // todo - implement
    }

    JTable getTable() {
        return table;
    }

    JCheckBox getCheckBox() {
        return checkBox;
    }

    private static TableCellEditor createColorEditor() {
        return new ColorCellEditor();
    }

    private static TableCellRenderer createColorRenderer() {
        final ColorCellRenderer renderer = new ColorCellRenderer();
        renderer.setColorValueVisible(false);

        return renderer;
    }

    private static TableCellRenderer createBrightnessRenderer() {
        final NumberFormat numberFormat = NumberFormat.getInstance();

        numberFormat.setMinimumIntegerDigits(1);
        numberFormat.setMaximumIntegerDigits(1);
        numberFormat.setMinimumFractionDigits(3);
        numberFormat.setMaximumFractionDigits(3);

        return new FormattedNumberRenderer(numberFormat);
    }

    private static TableCellRenderer createOccurrenceRenderer() {
        final NumberFormat numberFormat = NumberFormat.getPercentInstance();

        numberFormat.setMinimumFractionDigits(1);
        numberFormat.setMaximumFractionDigits(3);

        return new FormattedNumberRenderer(numberFormat);
    }
}
