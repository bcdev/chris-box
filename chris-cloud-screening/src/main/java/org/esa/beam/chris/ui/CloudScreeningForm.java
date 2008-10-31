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

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.swing.BindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.ParametersPane;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
class CloudScreeningForm extends JPanel {

    private final SourceProductSelector sourceProductSelector;

    CloudScreeningForm(CloudScreeningFormModel formModel, AppContext appContext) {
        sourceProductSelector = new SourceProductSelector(appContext);

        // configure the source product selector
        final JComboBox comboBox = sourceProductSelector.getProductNameComboBox();
        comboBox.setPrototypeDisplayValue("[1] CHRIS_HH_HHHHHH_HHHH_HH_NR");
        final ValueContainer vc = formModel.getProductValueContainer();
        final BindingContext bc = new BindingContext(vc);
        bc.bind("radianceProduct", comboBox);

        setLayout(new BorderLayout(3, 3));
        add(sourceProductSelector.createDefaultPanel(), BorderLayout.NORTH);
        add(createParametersPanel(formModel), BorderLayout.CENTER);
    }

    void prepareHide() {
        sourceProductSelector.releaseProducts();
    }

    void prepareShow() {
        sourceProductSelector.initProducts();
        if (sourceProductSelector.getProductCount() > 0) {
            sourceProductSelector.setSelectedIndex(0);
        }
    }

    private static JPanel createParametersPanel(final CloudScreeningFormModel formModel) {
        final ValueContainer vc = formModel.getParameterValueContainer();
        final BindingContext bc = new BindingContext(vc);
        final ParametersPane pane = new ParametersPane(bc);
        final JPanel panel = pane.createPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Processing Parameters"));

        final JComponent wvCheckBox = bc.getBinding("useWv").getComponents()[0];
        final JComponent o2CheckBox = bc.getBinding("useO2").getComponents()[0];

        formModel.getProductValueContainer().addPropertyChangeListener("radianceProduct", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() instanceof Product) {
                    final boolean enabled = formModel.updateFeatureAvailability((Product) evt.getNewValue());
                    wvCheckBox.setEnabled(enabled);
                    o2CheckBox.setEnabled(enabled);
                }
            }
        });

        return panel;
    }
}
