package org.esa.beam.chris.ui;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for invoking the CHIRS/Proba cloud screening dialog.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class CloudScreeningAction extends AbstractVisatAction {
    static final String HELP_ID = "chrisCloudScreeningTools";

    private final AtomicReference<ModelessDialog> dialog;

    public CloudScreeningAction() {
        dialog = new AtomicReference<ModelessDialog>();
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        dialog.compareAndSet(null, new ScreeningDialog(getAppContext()));
        dialog.get().show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        setEnabled(selectedProduct == null || new CloudScreeningProductFilter().accept(selectedProduct));
    }

}
