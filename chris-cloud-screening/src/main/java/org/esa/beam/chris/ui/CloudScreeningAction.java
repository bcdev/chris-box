package org.esa.beam.chris.ui;


import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for screening clouds.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class CloudScreeningAction extends AbstractVisatAction {

    private final AtomicReference<ModelessDialog> dialog;

    public CloudScreeningAction() {
        dialog = new AtomicReference<ModelessDialog>();
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        // todo - helpId
        dialog.compareAndSet(null, new CloudScreeningDialog(getAppContext(), ""));
        dialog.get().show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        final boolean enabled = selectedProduct == null || isValid(selectedProduct);
        setEnabled(enabled);
    }

    private static boolean isValid(Product product) {
        return product.getProductType().matches("CHRIS_M.*_NR") &&
                OpUtils.findBands(product, "radiance").length >= 18;
    }
}
