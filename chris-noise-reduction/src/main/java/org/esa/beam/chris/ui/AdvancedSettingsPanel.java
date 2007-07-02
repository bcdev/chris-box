package org.esa.beam.chris.ui;

import com.bc.ceres.binding.swing.SwingBindingContext;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

class AdvancedSettingsPanel extends JPanel {

    private javax.swing.JCheckBox applySlitCheckBox;
    private javax.swing.JSpinner smoothingOrderSpinner;

    private javax.swing.JSpinner numBandsComboBox;
    private javax.swing.JComboBox neighbourhoodTypeComboBox;

    public AdvancedSettingsPanel(AdvancedSettingsPresenter presenter) {
        initComponents();
        bindComponents(presenter);
    }

    private void bindComponents(AdvancedSettingsPresenter presenter) {
        SwingBindingContext smoothingBinding = new SwingBindingContext(presenter.getDestripingContainer());
        smoothingBinding.bind(smoothingOrderSpinner,  "smoothingOrder");
        smoothingBinding.bind(applySlitCheckBox, "slitCorrection");
        SwingBindingContext dropoutBinding = new SwingBindingContext(presenter.getDropOutCorrectionContainer());
        dropoutBinding.bind(numBandsComboBox,  "includeSpectralBandsCount");
        dropoutBinding.bind(neighbourhoodTypeComboBox,  "neighbourhoodType");
        neighbourhoodTypeComboBox.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                                   boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if(component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    label.setText(value + "-Connected");
                }
                return component;
            }

        }
        );

    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        setLayout(new BorderLayout(5, 5));

        applySlitCheckBox = new JCheckBox();

        smoothingOrderSpinner = new JSpinner();
        numBandsComboBox = new JSpinner();
        JLabel neighbourhoodLabel = new JLabel();
        neighbourhoodTypeComboBox = new JComboBox();

        JPanel advancedSettingsPanel = new JPanel(new GridBagLayout());

        JPanel verticalStripingPanel = new JPanel(new GridBagLayout());

        verticalStripingPanel.setBorder(BorderFactory.createTitledBorder(null,
                                                                         "Vertical Striping Correction",
                                                                         TitledBorder.DEFAULT_JUSTIFICATION,
                                                                         TitledBorder.DEFAULT_POSITION,
                                                                         new Font("Tahoma", 0, 11),
                                                                         new Color(0, 70, 213)));
        applySlitCheckBox.setSelected(true);
        applySlitCheckBox.setText("Apply Slit Correction");
        applySlitCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        applySlitCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        applySlitCheckBox.setMargin(new Insets(0, 0, 0, 0));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(0, 3, 0, 10);
        verticalStripingPanel.add(applySlitCheckBox, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(0, 10, 0, 3);
        verticalStripingPanel.add(new JLabel("Smoothing Order:"), gridBagConstraints);

        Dimension preferredSize = smoothingOrderSpinner.getPreferredSize();
        preferredSize.width = 50;
        smoothingOrderSpinner.setPreferredSize(preferredSize);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.6;
        gridBagConstraints.insets = new Insets(0, 0, 0, 3);
        verticalStripingPanel.add(smoothingOrderSpinner, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        verticalStripingPanel.add(new JLabel("pixels"), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        advancedSettingsPanel.add(verticalStripingPanel, gridBagConstraints);

        JPanel dropoutPanel = new JPanel(new GridBagLayout());

        dropoutPanel.setBorder(BorderFactory.createTitledBorder(null, "Dropout Correction",
                                                                TitledBorder.DEFAULT_JUSTIFICATION,
                                                                TitledBorder.DEFAULT_POSITION,
                                                                new Font("Tahoma", 0, 11),
                                                                new Color(0, 70, 213)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(0, 3, 3, 10);
        dropoutPanel.add(new JLabel("Number of Spectral Bands:"), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.6;
        gridBagConstraints.insets = new Insets(0, 0, 3, 3);
        dropoutPanel.add(numBandsComboBox, gridBagConstraints);

        neighbourhoodLabel.setText("Neighbourhood Type:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(0, 3, 0, 10);
        dropoutPanel.add(neighbourhoodLabel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.6;
        gridBagConstraints.insets = new Insets(0, 0, 0, 3);
        dropoutPanel.add(neighbourhoodTypeComboBox, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        advancedSettingsPanel.add(dropoutPanel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);

        add(advancedSettingsPanel, BorderLayout.CENTER);

    }


}
