package org.esa.beam.chris.ui;


import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.HashSet;
import java.util.Set;

/**
 * Action for masking clouds.
 *
 * @author Marco Peters
 * @author Marco Zuehlke
 */
public class CloudScreeningAction extends AbstractVisatAction {

    private final Set<Product> activeProductSet;

    public CloudScreeningAction() {
        activeProductSet = new HashSet<Product>(7);
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        // todo - helpId
        new CloudScreeningDialog(getAppContext(), "", activeProductSet).show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        final boolean enabled = selectedProduct == null ||
                !activeProductSet.contains(selectedProduct) && isValid(selectedProduct);
        setEnabled(enabled);
    }

    private static boolean isValid(Product product) {
        return product.getProductType().matches("CHRIS_M.*_NR") &&
                OpUtils.findBands(product, "radiance").length >= 18;
    }
}
