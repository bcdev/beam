/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.binning.ObservationSlice;
import org.esa.beam.binning.SpatialBinner;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class which performs a spatial binning of single input products.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 */
public class SpatialProductBinner {

    /**
     * Processes a source product and generated spatial bins.
     *
     * @param product         The source product.
     * @param spatialBinner   The spatial binner to be used.
     * @param superSampling   The super-sampling rate.
     * @param addedBands      A container for the bands that are added during processing.
     * @param progressMonitor A progress monitor.
     *
     * @return The total number of observations processed.
     *
     * @throws IOException If an I/O error occurs.
     */
    public static long processProduct(Product product,
                                      SpatialBinner spatialBinner,
                                      Integer superSampling,
                                      Map<Product, List<Band>> addedBands,
                                      ProgressMonitor progressMonitor) throws IOException {
        if (product.getGeoCoding() == null) {
            throw new IllegalArgumentException("product.getGeoCoding() == null");
        }

        final float[] superSamplingSteps = getSuperSamplingSteps(superSampling);

        final VariableContext variableContext = spatialBinner.getBinningContext().getVariableContext();

        for (int i = 0; i < variableContext.getVariableCount(); i++) {
            String variableName = variableContext.getVariableName(i);
            String variableExpr = variableContext.getVariableExpression(i);
            if (variableExpr != null) {
                VirtualBand band = new VirtualBand(variableName,
                                                   ProductData.TYPE_FLOAT32,
                                                   product.getSceneRasterWidth(),
                                                   product.getSceneRasterHeight(),
                                                   variableExpr);
                band.setValidPixelExpression(variableContext.getValidMaskExpression());
                product.addBand(band);
                if (!addedBands.containsKey(product)) {
                    addedBands.put(product, new ArrayList<Band>());
                }
                addedBands.get(product).add(band);
            }
        }

        final String maskExpr = variableContext.getValidMaskExpression();
        final int sliceWidth = product.getSceneRasterWidth();
        Dimension preferredTileSize = product.getPreferredTileSize();
        int sliceHeight;
        if (preferredTileSize != null) {
            sliceHeight = preferredTileSize.height;
        }else {
            sliceHeight = ImageManager.getPreferredTileSize(product).height;
        }
        boolean hasFullWidthTiles = false;
        MultiLevelImage maskImage = null;
        if (StringUtils.isNotNullAndNotEmpty(maskExpr)) {
            maskImage = ImageManager.getInstance().getMaskImage(maskExpr, product);
            sliceHeight = maskImage.getTileHeight();
            hasFullWidthTiles = areTileSizesCompatible(maskImage, sliceWidth, sliceHeight);
        }

        final MultiLevelImage[] varImages = new MultiLevelImage[variableContext.getVariableCount()];
        for (int i = 0; i < variableContext.getVariableCount(); i++) {
            final String nodeName = variableContext.getVariableName(i);
            final RasterDataNode node = getRasterDataNode(product, nodeName);
            final MultiLevelImage varImage = node.getGeophysicalImage();
            hasFullWidthTiles = hasFullWidthTiles && areTileSizesCompatible(varImage, sliceWidth, sliceHeight);
            varImages[i] = varImage;
        }

        final GeoCoding geoCoding = product.getGeoCoding();
        long numObsTotal = 0;
        MultiLevelImage referenceImage = varImages[0];
        if (hasFullWidthTiles) {
            final Point[] tileIndices = referenceImage.getTileIndices(null);
            progressMonitor.beginTask("Spatially binning of " + product.getName(), tileIndices.length);
            for (Point tileIndex : tileIndices) {
                int currentTileIndex = referenceImage.getNumXTiles() * tileIndex.y + tileIndex.x;
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                final ObservationSlice observationSlice = createObservationSlice(geoCoding,
                                                                                 maskImage, varImages,
                                                                                 tileIndex,
                                                                                 superSamplingSteps);
                numObsTotal += spatialBinner.processObservationSlice(observationSlice);
                progressMonitor.worked(1);
                stopWatch.stopAndTrace(String.format("Processed tile %d of %d", currentTileIndex, tileIndices.length));
            }
            progressMonitor.done();
        } else {
            int sceneHeight = referenceImage.getHeight();
            int numSlices = MathUtils.ceilInt(sceneHeight / (double) sliceHeight);
            int currentSliceHeight = sliceHeight;
            progressMonitor.beginTask("Spatially binning of " + product.getName(), numSlices);
            for (int sliceIndex = 0; sliceIndex < numSlices; sliceIndex++) {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                int sliceY = sliceIndex * sliceHeight;
                if (sliceY + sliceHeight > sceneHeight) {
                    currentSliceHeight = sceneHeight - sliceY;
                }
                Rectangle sliceRect = new Rectangle(0, sliceIndex * sliceHeight, sliceWidth, currentSliceHeight);

                final Raster[] varTiles = new Raster[varImages.length];
                for (int i = 0; i < varImages.length; i++) {
                    varTiles[i] = varImages[i].getData(sliceRect);
                }
                Raster maskTile = maskImage != null ? maskImage.getData(sliceRect): null;
                final ObservationSlice observationSlice = createObservationSlice(geoCoding, maskTile, varTiles,
                                                                                 superSamplingSteps);
                numObsTotal += spatialBinner.processObservationSlice(observationSlice);
                progressMonitor.worked(1);
                stopWatch.stopAndTrace(String.format("Processed slice %d of %d", sliceIndex, numSlices));
            }
            progressMonitor.done();
        }

        spatialBinner.complete();
        return numObsTotal;
    }

    static float[] getSuperSamplingSteps(Integer superSampling) {
        if (superSampling == null || superSampling <= 1) {
            return new float[]{0.5f};
        } else {
            float[] samplingStep = new float[superSampling];
            for (int i = 0; i < samplingStep.length; i++) {
                samplingStep[i] = (i * 2.0F + 1.0F) / (2.0F * superSampling);
            }
            return samplingStep;
        }
    }


    private static ObservationSlice createObservationSlice(GeoCoding geoCoding,
                                                           RenderedImage maskImage,
                                                           RenderedImage[] varImages,
                                                           Point tileIndex,
                                                           float[] superSamplingSteps) {
        final Raster maskTile = maskImage.getTile(tileIndex.x, tileIndex.y);
        final Raster[] varTiles = new Raster[varImages.length];
        for (int i = 0; i < varImages.length; i++) {
            varTiles[i] = varImages[i].getTile(tileIndex.x, tileIndex.y);
        }
        return createObservationSlice(geoCoding, maskTile, varTiles, superSamplingSteps);
    }

    private static ObservationSlice createObservationSlice(GeoCoding geoCoding, Raster maskTile, Raster[] varTiles,
                                                           float[] superSamplingSteps) {

        return new ObservationSlice(varTiles, maskTile, geoCoding, superSamplingSteps);
    }

    private static RasterDataNode getRasterDataNode(Product product, String nodeName) {
        final RasterDataNode node = product.getRasterDataNode(nodeName);
        if (node == null) {
            throw new IllegalStateException(String.format("Can't find raster data node '%s' in product '%s'",
                                                          nodeName, product.getName()));
        }
        return node;
    }

    private static boolean areTileSizesCompatible(MultiLevelImage image, int sliceWidth, int sliceHeight) {
        return image.getTileWidth() == sliceWidth && image.getTileHeight() == sliceHeight;
    }
}
