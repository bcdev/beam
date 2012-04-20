/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.rgb;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.io.FileUtils;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public class ImageProductReader extends AbstractProductReader {

    public static final float[] RGB_WAVELENGTHS = new float[]{700.0F, 546.1F, 435.8F};
    public static final String[] RGBA_BAND_NAMES = new String[]{"red", "green", "blue", "alpha"};
    public static final String GRAY_BAND_NAME = "gray";

    private static boolean profilesRegistered;
    private RenderedOp sourceImage;

    public ImageProductReader(ImageProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);

        if (!profilesRegistered) {
            profilesRegistered = true;
            RGBImageProfileManager.getInstance().addProfile(new RGBImageProfile("RGBA",
                                                                                RGBA_BAND_NAMES));
            RGBImageProfileManager.getInstance().addProfile(new RGBImageProfile("RGB",
                                                                                new String[]{
                                                                                        RGBA_BAND_NAMES[0],
                                                                                        RGBA_BAND_NAMES[1],
                                                                                        RGBA_BAND_NAMES[2]}));
        }
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File file = ImageProductReaderPlugIn.getFile(getInput());
        sourceImage = FileLoadDescriptor.create(file.getPath(), null, true, null);
        ColorModel colorModel = sourceImage.getColorModel();
        ColorSpace colorSpace = colorModel.getColorSpace();

        SampleModel sampleModel = sourceImage.getSampleModel();
        Product product = new Product(FileUtils.getFilenameWithoutExtension(file), ImageProductReaderPlugIn.FORMAT_NAME,
                                      sourceImage.getWidth(), sourceImage.getHeight());
        product.setDescription(String.format("Image (%s, %s)", colorSpace.getClass().getSimpleName(), sampleModel.getClass().getSimpleName()));
        int numBands = sourceImage.getNumBands();
        for (int i = 0; i < numBands; i++) {
            RenderedOp bandImage = BandSelectDescriptor.create(sourceImage, new int[]{i}, null);
            String bandName;
            if (numBands == 1) {
                bandName = GRAY_BAND_NAME;
            } else if (numBands == 3 || numBands == 4) {
                bandName = RGBA_BAND_NAMES[i];
            } else {
                bandName = "b" + (i + 1);
            }
            Band band = product.addBand(bandName, ImageManager.getProductDataType(bandImage.getSampleModel().getDataType()));
            band.setSourceImage(bandImage);
            band.setUnit("dl");
            band.setDescription("Image component #" + (i + 1));
            if ((numBands == 3 || numBands == 4) && i < 3) {
                band.setSpectralBandIndex(i);
                band.setSpectralWavelength(RGB_WAVELENGTHS[i]);
            }
            if (bandImage.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
                band.setScalingFactor(1.0 / 255.0);
            } else if (bandImage.getSampleModel().getDataType() == DataBuffer.TYPE_USHORT) {
                band.setScalingFactor(1.0 / (Short.MAX_VALUE - Short.MIN_VALUE));
            } else if (bandImage.getSampleModel().getDataType() == DataBuffer.TYPE_SHORT) {
                band.setScalingFactor(1.0 / (Short.MAX_VALUE - Short.MIN_VALUE));
                band.setScalingOffset(-Short.MIN_VALUE / (Short.MAX_VALUE - Short.MIN_VALUE));
            }
        }

        if (numBands == 3 || numBands == 4) {
            product.addBand(GRAY_BAND_NAME,
                            String.format("0.3 * %s + 0.59 * %s + 0.11 * %s",
                                          RGBA_BAND_NAMES[0], RGBA_BAND_NAMES[1], RGBA_BAND_NAMES[2]),
                            ProductData.TYPE_FLOAT32);
        }

        // alpha channel exists
        if (numBands == 4) {
            String validPixelExpression = String.format("%s > 0", RGBA_BAND_NAMES[3]);
            for (int i = 0; i < 3; i++) {
                product.getBandAt(i).setValidPixelExpression(validPixelExpression);
            }
            product.getBand(GRAY_BAND_NAME).setValidPixelExpression(validPixelExpression);
        }

        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        // do nothing
    }

    @Override
    public void close() throws IOException {
        super.close();
        sourceImage.dispose();
    }

}
