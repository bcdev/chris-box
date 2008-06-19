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
package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

/**
 * Operator for performing the atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "chris.PerformAtmosphericCorrection",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "Computes surface reflectances from CHRIS/PROBA RCI sets with cloud product.")
public class PerformAtmosphericCorrectionOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false")
    private boolean performSpectralPolishing;

    @Parameter(defaultValue = "false")
    private boolean performAdjacencyCorrection;

    @Parameter(defaultValue = "0",
               interval = "[0.0, 1.0]",
               description = "Value of the aerosol optical thickness (AOT) at 550 nm. If nonzero, AOT retrieval is disabled.")
    private double aot550;

    @Parameter(defaultValue = "0.0",
               interval = "[0.0, 5.0]",
               description = "Initial water vapour (WV) column guess used for WV retrieval.")
    private double wvIni;

    @Parameter(defaultValue = "false",
               description = "If 'false' no water vapour map is generated for modes 1, 3 and 5.")
    private boolean wvMap;

    @Parameter(defaultValue = "0.05",
               description = "Threshold applicable to surface reflectance and WV retrieval.")
    private double cldReflThre;

    private transient ModtranLookupTable lut;

    @Override
    public void initialize() throws OperatorException {
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle,
                                 ProgressMonitor pm) throws OperatorException {
        // Read MODTRAN lookup table if not already done
        synchronized (this) {
            if (lut == null) {
                try {
                    lut = new ModtranLookupTableReader().readLookupTable();
                } catch (IOException e) {
                    throw new OperatorException(e.getMessage());
                }
            }
        }

    }

    @Override
    public void dispose() {
        lut = null;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PerformAtmosphericCorrectionOp.class);
        }
    }
}
