package org.esa.beam.chris.ui;

import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import org.esa.beam.chris.operators.ExtractEndmembersOp;
import org.esa.beam.chris.operators.MakeClusterMapOp;
import org.esa.beam.chris.operators.internal.BandFilter;
import org.esa.beam.chris.operators.internal.InclusiveMultiBandFilter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.operators.common.BandArithmeticOp;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.unmixing.Endmember;
import org.esa.beam.unmixing.SpectralUnmixingOp;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.AbstractAction;
import javax.swing.JInternalFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;

/**
 * Action for masking clouds.
 *
 * @author Marco Peters
 * @author Marco Zühlke
 */
public class CloudMaskAction extends AbstractVisatAction {

    private static List<String> CHRIS_TYPES;

    // todo - move
    private Product reflectanceProduct;
    private Product featureProduct;
    private Product clusterProduct;
    private Product clusterMapProduct;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES,
                "CHRIS_M1_REFL",
                "CHRIS_M2_REFL",
                "CHRIS_M3_REFL",
                "CHRIS_M30_REFL",
                "CHRIS_M3A_REFL",
                "CHRIS_M4_REFL",
                "CHRIS_M5_REFL");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final VisatApp visatApp = VisatApp.getApp();
        try {
            reflectanceProduct = visatApp.getSelectedProduct();
                       
            visatApp.openProductSceneViewRGB(reflectanceProduct, "");
            processStepOne();
            final Band membershipBand = clusterMapProduct.getBand("membership_mask");
            visatApp.openProductSceneView(membershipBand, "");
            final DockingManager dockingManager = visatApp.getMainFrame().getDockingManager();
            final DockableFrame dockableFrame = new DockableFrame("Cluster Labeling");

            final ImageInfo imageInfo = membershipBand.getImageInfo();
            final ClusterLabelingToolView clusterLabelingToolView = new ClusterLabelingToolView(imageInfo);

            final ActionListener labelingAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final ClusterLabelingToolView.LabelTableModel labelTableModel = clusterLabelingToolView.getTableModel();
                    membershipBand.setImageInfo(labelTableModel.getImageInfo().createDeepCopy());
                    processLabelingStep(labelTableModel.getBackgroundIndexes());
                    final JInternalFrame internalFrame = visatApp.findInternalFrame(membershipBand);
                    final ProductSceneView productSceneView = (ProductSceneView) internalFrame.getContentPane();
                    visatApp.updateImage(productSceneView);
                }
            };
            clusterLabelingToolView.setLabelingAction(labelingAction);

            final ActionListener continueProcessingAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dockingManager.removeFrame(dockableFrame.getKey());
                    final JInternalFrame internalFrame = visatApp.findInternalFrame(membershipBand);
                    visatApp.getDesktopPane().closeFrame(internalFrame);

                    final ClusterLabelingToolView.LabelTableModel labelTableModel = clusterLabelingToolView.getTableModel();
                    processLabelingStep(labelTableModel.getBackgroundIndexes());
                    final int[] cloudClusterIndexes = labelTableModel.getCloudIndexes();
                    final int[] surfaceClusterIndexes = labelTableModel.getSurfaceIndexes();
                    final Product cloudMaskProduct = processStepTwo(cloudClusterIndexes, surfaceClusterIndexes);
                    visatApp.addProduct(cloudMaskProduct);
                }
            };
            clusterLabelingToolView.setComputeAction(continueProcessingAction);

            final AbstractAction closeAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    dockingManager.removeFrame(dockableFrame.getKey());
                }
            };
            dockableFrame.setCloseAction(closeAction);
            dockableFrame.setContentPane(clusterLabelingToolView.getControl());
            dockingManager.addFrame(dockableFrame);
            dockingManager.showFrame(dockableFrame.getKey());
        } catch (OperatorException e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
            VisatApp.getApp().getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct != null
                && CHRIS_TYPES.contains(selectedProduct.getProductType());

        setEnabled(enabled);
    }


    private void processStepOne() throws OperatorException {
        // 1. Extract features
        featureProduct = createFeatureProduct(reflectanceProduct);

        // 2. Find clusters
        clusterProduct = createClusterProduct(featureProduct);

        // 3. Cluster labeling
        final int[] backgroundIndexes = new int[0];
        clusterMapProduct = createClusterMapProduct(clusterProduct, backgroundIndexes);
    }

    private void processLabelingStep(int[] backgroundIndexes) throws OperatorException {
        for (Band band : clusterMapProduct.getBands()) {
            if (band.getName().startsWith("prob")) {
                final MakeClusterMapOp.ProbabilityImageBand probBand = (MakeClusterMapOp.ProbabilityImageBand) band;
                probBand.update(backgroundIndexes);
            }
        }
        final MakeClusterMapOp.MembershipImageBand membershipBand = (MakeClusterMapOp.MembershipImageBand) clusterMapProduct.getBand("membership_mask");
        membershipBand.update();
    }

    private Product processStepTwo(int[] cloudClusterIndexes, int[] surfaceClusterIndexes) throws OperatorException {

        // 4. Cluster probabilities
        final Product cloudProbabilityProduct = createCloudProbabilityProduct(cloudClusterIndexes, clusterMapProduct);

        // 5. Endmember extraction
        final ExtractEndmembersOp endmemberOp = new ExtractEndmembersOp(reflectanceProduct, featureProduct, clusterMapProduct, cloudClusterIndexes,
                surfaceClusterIndexes);
        final Endmember[] endmembers = (Endmember[]) endmemberOp.getTargetProperty("endmembers");

        // 6. Cloud abundances
        final Product cloudAbundancesProduct = createCloudAbundancesProduct(reflectanceProduct, endmembers);

        // 7. Cloud probability * cloud abundance

        return createCloudMaskProduct(cloudProbabilityProduct, cloudAbundancesProduct);
    }

    private Product createCloudMaskProduct(Product cloudProbabilityProduct, Product cloudAbundancesProduct) {
        BandArithmeticOp.BandDescriptor[] bandDescriptors = new BandArithmeticOp.BandDescriptor[1];
        bandDescriptors[0] = new BandArithmeticOp.BandDescriptor();
        bandDescriptors[0].name = "cloud_mask";
        bandDescriptors[0].expression = "$probability.cloud_probability * $abundance.cloud_abundance";
        bandDescriptors[0].type = ProductData.TYPESTRING_FLOAT32;
        final Map<String, Object> cloudMaskParameterMap = new HashMap<String, Object>();
        cloudMaskParameterMap.put("targetBandDescriptors", bandDescriptors);
        final Map<String, Product> cloudMaskSourceMap = new HashMap<String, Product>();
        cloudMaskSourceMap.put("probability", cloudProbabilityProduct);
        cloudMaskSourceMap.put("abundance", cloudAbundancesProduct);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(BandArithmeticOp.class),
                cloudMaskParameterMap, cloudMaskSourceMap);
    }

    private Product createCloudAbundancesProduct(Product reflectanceProduct, Endmember[] endmembers) {
        final String[] reflBands = findBandNames(reflectanceProduct, "reflectance_");
        final Map<String, Object> unmixingParameterMap = new HashMap<String, Object>();
        unmixingParameterMap.put("sourceBandNames", reflBands);
        unmixingParameterMap.put("endmembers", endmembers);
        unmixingParameterMap.put("unmixingModelName", "Fully Constrained LSU");

        return GPF.createProduct(OperatorSpi.getOperatorAlias(SpectralUnmixingOp.class),
                unmixingParameterMap, reflectanceProduct);
    }

    private Product createCloudProbabilityProduct(int[] cloudClusterIndexes, Product clusterMapProduct) {
        final Map<String, Object> accumulateOpParameterMap = new HashMap<String, Object>();

        final String[] bandNames = this.clusterMapProduct.getBandNames();
        final List<String> bandNameList = new ArrayList<String>();
        for (int i = 0; i < bandNames.length; ++i) {
            if (bandNames[i].startsWith("prob")) {
                if (isContained(i, cloudClusterIndexes)) {
                    bandNameList.add(bandNames[i]);
                }
            }
        }
        final String[] cloudProbabilityBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        accumulateOpParameterMap.put("sourceBands", cloudProbabilityBandNames);
        accumulateOpParameterMap.put("targetBand", "cloud_probability");
        // todo - product name and type

        return GPF.createProduct("chris.Accumulate", accumulateOpParameterMap, clusterMapProduct);
    }

    private Product createClusterMapProduct(Product clusterProduct, int[] backgroundIndexes) {
        final Map<String, Object> clusterMapParameter = new HashMap<String, Object>();
        clusterMapParameter.put("backgroundClusterIndexes", backgroundIndexes);
        return GPF.createProduct("chris.MakeClusterMap", clusterMapParameter, clusterProduct);
    }

    private Product createClusterProduct(Product featureProduct) {
        final Map<String, Object> findClustersOpParameterMap = new HashMap<String, Object>();
        findClustersOpParameterMap.put("sourceBandNames", new String[]{"brightness_vis",
                "brightness_nir",
                "whiteness_vis",
                "whiteness_nir",
                "wv"});
        findClustersOpParameterMap.put("clusterCount", 14);
        findClustersOpParameterMap.put("iterationCount", 40);

        return GPF.createProduct("chris.FindClusters",
                findClustersOpParameterMap,
                featureProduct);
    }

    private static String[] findBandNames(Product product, String prefix) {
        final BandFilter bandFilter = new InclusiveMultiBandFilter(new double[][]{
                {400.0, 440.0},
                {590.0, 600.0},
                {630.0, 636.0},
                {648.0, 658.0},
                {686.0, 709.0},
                {792.0, 799.0},
                {756.0, 775.0},
                {808.0, 840.0},
                {885.0, 985.0},
                {985.0, 1010.0}});
        final List<String> nameList = new ArrayList<String>();

        for (final Band band : product.getBands()) {
            if (band.getName().startsWith(prefix) && !bandFilter.accept(band)) {
                nameList.add(band.getName());
            }
        }

        return nameList.toArray(new String[nameList.size()]);
    }

    private Product createFeatureProduct(Product reflectanceProduct) {
        final HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        return GPF.createProduct("chris.ExtractFeatures",
                parameterMap,
                reflectanceProduct);
    }

//    private static String[] findBandNames(Product product, String prefix) {
//        final List<String> bandList = new ArrayList<String>();
//
//        for (final String bandName : product.getBandNames()) {
//            if (bandName.startsWith(prefix)) {
//                bandList.add(bandName);
//            }
//        }
//        return bandList.toArray(new String[bandList.size()]);
//    }

    private static boolean isContained(int index, int[] indexes) {
        for (int i : indexes) {
            if (index == i) {
                return true;
            }
        }

        return false;
    }

}
