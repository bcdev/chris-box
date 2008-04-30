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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.operators.common.WriteOp;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Noise reduction swing worker.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class NoiseReductionSwingWorker extends ProgressMonitorSwingWorker<Object, Product> {

    private final Map<Product, File> targetFileMap;
    private final Product[] destripingSourceProducts;
    private final Map<String, Object> destripingParameterMap;
    private final Map<String, Object> dropoutCorrectionParameterMap;

    private final String targetFormatName;

    private final boolean openInApp;
    private final AppContext appContext;

    public NoiseReductionSwingWorker(Product[] destripingSourceProducts,
                                     Map<Product, File> targetFileMap,
                                     Map<String, Object> destripingParameterMap,
                                     Map<String, Object> dropoutCorrectionParameterMap,
                                     String targetFormatName,
                                     AppContext appContext, boolean openInApp) {
        super(appContext.getApplicationWindow(), "Performing Noise Reduction");
        this.destripingSourceProducts = destripingSourceProducts;
        this.targetFileMap = targetFileMap;
        this.destripingParameterMap = destripingParameterMap;
        this.dropoutCorrectionParameterMap = dropoutCorrectionParameterMap;
        this.targetFormatName = targetFormatName;
        this.openInApp = openInApp;
        this.appContext = appContext;
    }

    @Override
    protected Object doInBackground(ProgressMonitor pm) throws Exception {
        Product factorsProduct = null;

        try {
            pm.beginTask("Performing noise reduction...", 50 + targetFileMap.size() * 10);
            factorsProduct = GPF.createProduct("chris.ComputeDestripingFactors",
                    destripingParameterMap,
                    destripingSourceProducts);
            final File file = createDestripingFactorsFile();
            writeProduct(factorsProduct, file, false, SubProgressMonitor.create(pm, 50));

            try {
                factorsProduct = ProductIO.readProduct(file, null);
            } catch (IOException e) {
                throw new OperatorException(MessageFormat.format("Could not read file ''{0}''.", file.getPath()), e);
            }

            for (Map.Entry<Product, File> entry : targetFileMap.entrySet()) {
                performNoiseReduction(
                        entry.getKey(),
                        factorsProduct,
                        entry.getValue(),
                        SubProgressMonitor.create(pm, 10));
            }
        } finally {
            if (factorsProduct != null) {
                factorsProduct.dispose();
            }
            pm.done();
        }

        return null;
    }

    private File createDestripingFactorsFile() {
        final File noiseReduceTargetFile = targetFileMap.values().iterator().next(); //toArray(new File[targetFileMap.size()])[0];
        final String basename = FileUtils.getFilenameWithoutExtension(noiseReduceTargetFile);
        final String extension = FileUtils.getExtension(noiseReduceTargetFile);

        return new File(noiseReduceTargetFile.getParentFile(), basename + "_VSC" + extension);
    }

    @Override
    protected void process(List<Product> products) {
        if (openInApp) {
            for (Product product : products) {
                appContext.addProduct(product);
            }
        }
    }

    @Override
    protected void done() {
        try {
            get();
        } catch (InterruptedException e) {
            // ignore
        } catch (ExecutionException e) {
            appContext.handleError(e.getCause());
        }
    }

    private void performNoiseReduction(Product sourceProduct, Product factorProduct, File targetFile,
                                       ProgressMonitor pm) throws IOException {
        final HashMap<String, Product> sourceProductMap = new HashMap<String, Product>(5);
        sourceProductMap.put("input", sourceProduct);
        sourceProductMap.put("factors", factorProduct);

        Product destripedProduct = null;

        try {
            destripedProduct = GPF.createProduct("chris.ApplyDestripingFactors",
                    new HashMap<String, Object>(0), sourceProductMap);
            final Product targetProduct = GPF.createProduct("chris.CorrectDropouts", dropoutCorrectionParameterMap,
                    destripedProduct);

            targetProduct.setName(FileUtils.getFilenameWithoutExtension(targetFile));
            writeProduct(targetProduct, targetFile, openInApp, pm);
        } finally {
            if (destripedProduct != null) {
                destripedProduct.dispose();
            }
        }
    }

    private void writeProduct(final Product targetProduct, final File targetFile, boolean openInApp,
                              ProgressMonitor pm) throws IOException {
        Product product = null;

        try {
            pm.beginTask("Writing " + targetProduct.getName() + "...", openInApp ? 100 : 95);
            targetProduct.setFileLocation(targetFile);
            WriteOp.writeProduct(targetProduct, targetFile, targetFormatName, SubProgressMonitor.create(pm, 95));

            if (openInApp) {
                product = ProductIO.readProduct(targetFile, null);
                if (product != null) {
                    publish(product);
                } else {
                    publish(targetProduct);
                }
                pm.worked(5);
            }
        } finally {
            if (product != null) {
                targetProduct.dispose();
            }
            pm.done();
        }
    }
}
