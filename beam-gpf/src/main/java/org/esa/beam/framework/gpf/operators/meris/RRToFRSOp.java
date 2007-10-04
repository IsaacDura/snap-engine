/*
 * $Id: L3ToL1Op.java,v 1.1 2007/03/27 12:51:05 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.operators.meris;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import com.bc.ceres.core.ProgressMonitor;

import java.awt.Rectangle;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:05 $
 */
public class RRToFRSOp extends AbstractOperator {

    private GeoCoding rrGeoCoding;
    private GeoCoding frsGeoCoding;

    @SourceProduct(alias = "frs")
    private Product frsProduct;
    @SourceProduct(alias = "rr")
    private Product rrProduct;
    @TargetProduct
    private Product targetProduct;

    @Override
    protected Product initialize() throws OperatorException {
        rrGeoCoding = rrProduct.getGeoCoding();
        frsGeoCoding = frsProduct.getGeoCoding();

        final int width = frsProduct.getSceneRasterWidth();
        final int height = frsProduct.getSceneRasterHeight();
        targetProduct = new Product("L1", "L1", width, height);

        Band[] srcBands = rrProduct.getBands();
        for (Band sourceBand : srcBands) {
            Band targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            ProductUtils.copySpectralAttributes(sourceBand, targetBand);
            targetBand.setDescription(sourceBand.getDescription());
            targetBand.setUnit(sourceBand.getUnit());
            targetBand.setScalingFactor(sourceBand.getScalingFactor());
            targetBand.setScalingOffset(sourceBand.getScalingOffset());
            targetBand.setLog10Scaled(sourceBand.isLog10Scaled());
            targetBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
            targetBand.setNoDataValue(sourceBand.getNoDataValue());
            if (sourceBand.getFlagCoding() != null) {
                FlagCoding srcFlagCoding = sourceBand.getFlagCoding();
                ProductUtils.copyFlagCoding(srcFlagCoding, targetProduct);
                targetBand.setFlagCoding(targetProduct.getFlagCoding(srcFlagCoding.getName()));
            }
        }
        return targetProduct;
    }

    @Override
    public void computeTile(Band band, Tile targetTile) throws OperatorException {

        Rectangle frsRectangle = targetTile.getRectangle();
        Band rrSrcBand = rrProduct.getBand(band.getName());
        ProgressMonitor pm = createProgressMonitor();
        pm.beginTask("compute", frsRectangle.height);

        PixelPos frsPixelPos = new PixelPos(frsRectangle.x, frsRectangle.y);
        GeoPos geoPos = frsGeoCoding.getGeoPos(frsPixelPos, null);
        PixelPos rrPixelPos = rrGeoCoding.getPixelPos(geoPos, null);
        final int xStart = Math.round(rrPixelPos.x);
        final int yStart = Math.round(rrPixelPos.y);
        Rectangle rrRectangle = new Rectangle(xStart, yStart, frsRectangle.width / 4, frsRectangle.height / 4);
        rrRectangle.grow(4, 4);
        Rectangle sceneRectangle = new Rectangle(rrSrcBand.getSceneRasterWidth(), rrSrcBand.getSceneRasterHeight());
        rrRectangle = rrRectangle.intersection(sceneRectangle);

//            System.out.println("RR: "+rrRectangle.toString());
//            System.out.println("FRS:"+frsRectangle.toString());
        Tile srcTile = getSourceTile(rrSrcBand, rrRectangle);

        try {
            int rrY = yStart;
            int iy = 0;
            for (int y = frsRectangle.y; y < frsRectangle.y + frsRectangle.height; y++) {
                int rrX = xStart;
                int ix = 0;
                for (int x = frsRectangle.x; x < frsRectangle.x + frsRectangle.width; x++) {
                    double d = srcTile.getSampleDouble(rrX, rrY);
                    targetTile.setSample(x, y, d);
                    if (ix < 3) {
                        ix++;
                    } else {
                        ix = 0;
                        rrX++;
                    }
                }
                if (iy < 3) {
                    iy++;
                } else {
                    iy = 0;
                    rrY++;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }


    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(RRToFRSOp.class, "RRToFRS");
        }
    }
}