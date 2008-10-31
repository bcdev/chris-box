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
import org.esa.beam.cluster.EMCluster;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.product.ProductSceneView;

import java.awt.*;
import java.awt.image.RenderedImage;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
interface LabelingContext {

    String getRadianceProductName();

    String getLabel(int index);

    void setLabel(int index, String label);

    Color getColor(int index);

    void setColor(int index, Color color);

    boolean isCloud(int index);

    void setCloud(int index, boolean b);

    boolean isIgnored(int index);

    void setIgnored(int index, boolean b);

    EMCluster[] getClusters();

    RenderedImage getClassificationImage();

    int getClassIndex(int x, int y, int currentLevel);

    void recomputeClassificationImage();

    ProductSceneView getRgbView();

    ProductSceneView getClassView();

    boolean isAnyCloudFlagSet();

    Band performCloudMaskCreation(ProgressMonitor pm);
}
