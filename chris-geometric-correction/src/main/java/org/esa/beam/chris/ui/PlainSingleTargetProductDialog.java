/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.chris.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Plain single target product dialog.
 * <p/>
 * NOTE: This class has been cloned from {@code org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog},
 * simplified in some aspects, and refactored. // todo: rq/rq - merge with origin or move to package of origin
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
class PlainSingleTargetProductDialog extends SingleTargetProductDialog {

    private final String operatorName;
    private final List<SourceProductSelector> sourceProductSelectorList;
    private final Map<Field, SourceProductSelector> sourceProductSelectorMap;
    private final Map<String, Object> parameterMap;

    private JTabbedPane form;
    private String targetProductNameSuffix;
    private ProductSelectionChangeHandler productSelectionChangeHandler;

    PlainSingleTargetProductDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(appContext, title, helpID);
        this.operatorName = operatorName;
        this.targetProductNameSuffix = "";

        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        final OperatorSpi operatorSpi = registry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operatorName");
        }
        final Class<? extends Operator> operatorClass = operatorSpi.getOperatorClass();

        sourceProductSelectorList = new ArrayList<SourceProductSelector>(3);
        sourceProductSelectorMap = new HashMap<Field, SourceProductSelector>(3);

        initSourceProductSelectors(operatorSpi);
        if (!sourceProductSelectorList.isEmpty()) {
            setSourceProductSelectorLabels();
            setSourceProductSelectorToolTipTexts();
        }

        final PropertyDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        parameterMap = new HashMap<String, Object>(17);
        final PropertyContainer propertyContainer =
                PropertyContainer.createMapBacked(parameterMap, operatorClass, parameterDescriptorFactory);
        propertyContainer.setDefaultValues();
        initProperties(propertyContainer);

        form = new JTabbedPane();
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);

        final JPanel ioParametersPanel = new JPanel(tableLayout);
        for (SourceProductSelector selector : sourceProductSelectorList) {
            ioParametersPanel.add(selector.createDefaultPanel());
        }
        ioParametersPanel.add(getTargetProductSelector().createDefaultPanel());
        ioParametersPanel.add(tableLayout.createVerticalSpacer());
        form.add("I/O Parameters", ioParametersPanel);
        if (propertyContainer.getProperties().length > 0) {
            final JPanel processingParametersPanel = createParametersPanel(propertyContainer);
            processingParametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
            form.add("Processing Parameters", new JScrollPane(processingParametersPanel));
        }
        if (!sourceProductSelectorList.isEmpty()) {
            productSelectionChangeHandler = new ProductSelectionChangeHandler();
            sourceProductSelectorList.get(0).addSelectionChangeListener(productSelectionChangeHandler);
        }
    }

    // method not exists in {@code org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog}

    public Object getParameterValue(String name) {
        return parameterMap.get(name);
    }

    public String getTargetProductNameSuffix() {
        return targetProductNameSuffix;
    }

    public void setTargetProductNameSuffix(String suffix) {
        targetProductNameSuffix = suffix;
    }

    @Override
    public int show() {
        prepareSourceProductSelectors();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        productSelectionChangeHandler.releaseProduct();
        releaseSourceProductSelectors();
        super.hide();
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Map<String, Product> sourceProducts = createSourceProductMap();
        return GPF.createProduct(operatorName, parameterMap, sourceProducts);
    }

    // method not exists in {@code org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog}

    protected void initProperties(PropertySet propertySet) {
    }

    // method not exists in {@code org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog}

    protected PropertyEditor findPropertyEditor(PropertyDescriptor descriptor) {
        return PropertyEditorRegistry.getInstance().findPropertyEditor(descriptor);
    }

    private void initSourceProductSelectors(OperatorSpi operatorSpi) {
        for (final Field field : operatorSpi.getOperatorClass().getDeclaredFields()) {
            final SourceProduct annotation = field.getAnnotation(SourceProduct.class);
            if (annotation != null) {
                final ProductFilter productFilter = new AnnotatedSourceProductFilter(annotation);
                SourceProductSelector sourceProductSelector = new SourceProductSelector(getAppContext());
                sourceProductSelector.setProductFilter(productFilter);
                sourceProductSelectorList.add(sourceProductSelector);
                sourceProductSelectorMap.put(field, sourceProductSelector);
            }
        }
    }

    private void setSourceProductSelectorLabels() {
        for (final Field field : sourceProductSelectorMap.keySet()) {
            String label = null;
            final SourceProduct annotation = field.getAnnotation(SourceProduct.class);
            if (!annotation.label().isEmpty()) {
                label = annotation.label();
            }
            if (label == null && !annotation.alias().isEmpty()) {
                label = annotation.alias();
            }
            if (label == null) {
                final String name;
                if (annotation.alias().isEmpty()) {
                    name = field.getName();
                } else {
                    name = annotation.alias();
                }
                label = PropertyDescriptor.createDisplayName(name);
            }
            if (!label.endsWith(":")) {
                label += ":";
            }
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            selector.getProductNameLabel().setText(label);
        }
    }

    private void setSourceProductSelectorToolTipTexts() {
        for (final Field field : sourceProductSelectorMap.keySet()) {
            final SourceProduct annotation = field.getAnnotation(SourceProduct.class);
            final String description = annotation.description();
            if (!description.isEmpty()) {
                final SourceProductSelector selector = sourceProductSelectorMap.get(field);
                selector.getProductNameComboBox().setToolTipText(description);
            }
        }
    }

    // method not exists in {@code org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog}

    private JPanel createParametersPanel(PropertyContainer propertyContainer) {
        final BindingContext bindingContext = new BindingContext(propertyContainer);
        final Property[] properties = propertyContainer.getProperties();

        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(3, 3);
        final JPanel parameterPanel = new JPanel(layout);

        int rowIndex = 0;
        for (final Property property : properties) {
            final PropertyDescriptor descriptor = property.getDescriptor();
            final PropertyEditor propertyEditor = findPropertyEditor(descriptor);
            final JComponent[] components = propertyEditor.createComponents(descriptor, bindingContext);
            if (components.length == 2) {
                layout.setCellWeightX(rowIndex, 0, 0.0);
                parameterPanel.add(components[1], new TableLayout.Cell(rowIndex, 0));
                layout.setCellWeightX(rowIndex, 1, 1.0);
                parameterPanel.add(components[0], new TableLayout.Cell(rowIndex, 1));
            } else {
                layout.setCellColspan(rowIndex, 0, 2);
                layout.setCellWeightX(rowIndex, 0, 1.0);
                parameterPanel.add(components[0], new TableLayout.Cell(rowIndex, 0));
            }
            rowIndex++;
        }
        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellWeightX(rowIndex, 0, 1.0);
        layout.setCellWeightY(rowIndex, 0, 1.0);
        parameterPanel.add(new JPanel());

        return parameterPanel;
    }

    private void prepareSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.initProducts();
        }
    }

    private void releaseSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.releaseProducts();
        }
    }

    private Map<String, Product> createSourceProductMap() {
        final Map<String, Product> sourceProductMap = new HashMap<String, Product>(8);
        for (final Field field : sourceProductSelectorMap.keySet()) {
            final SourceProduct annotation = field.getAnnotation(SourceProduct.class);
            final String key;
            if (annotation.alias().isEmpty()) {
                key = field.getName();
            } else {
                key = annotation.alias();
            }
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            sourceProductMap.put(key, selector.getSelectedProduct());
        }
        return sourceProductMap;
    }

    // handler has less functionality than in {@code org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog}

    private class ProductSelectionChangeHandler extends AbstractSelectionChangeListener {

        private Product selectedProduct;

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            final Selection selection = event.getSelection();
            if (selection != null && !selection.isEmpty()) {
                final Product product = (Product) selection.getSelectedValue();
                if (product != selectedProduct) {
                    selectedProduct = product;
                    updateTargetProductName();
                }
            }
        }

        private void releaseProduct() {
            if (selectedProduct != null) {
                selectedProduct = null;
            }
        }

        private void updateTargetProductName() {
            final TargetProductSelectorModel targetProductSelectorModel = getTargetProductSelector().getModel();
            targetProductSelectorModel.setProductName(selectedProduct.getName() + getTargetProductNameSuffix());
        }
    }

    private static class AnnotatedSourceProductFilter implements ProductFilter {

        private final SourceProduct annotation;

        private AnnotatedSourceProductFilter(SourceProduct annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean accept(Product product) {
            if (!annotation.type().isEmpty() && !product.getProductType().matches(annotation.type())) {
                return false;
            }
            for (final String bandName : annotation.bands()) {
                if (!product.containsBand(bandName)) {
                    return false;
                }
            }
            return true;
        }
    }
}
