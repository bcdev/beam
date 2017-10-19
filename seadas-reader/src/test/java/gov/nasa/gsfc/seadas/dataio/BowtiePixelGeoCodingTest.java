/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.framework.dataio.ProductFlipper;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class BowtiePixelGeoCodingTest {

    @Test
    public void testTransferGeoCoding() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));
        assertTrue(product.getGeoCoding() instanceof BowtiePixelGeoCoding);

        Product targetProduct = new Product("name", "type", product.getSceneRasterWidth(), product.getSceneRasterHeight());

        assertNull(targetProduct.getGeoCoding());
        ProductUtils.copyGeoCoding(product, targetProduct);

        assertNotNull(targetProduct.getGeoCoding());
        assertTrue(targetProduct.getGeoCoding() instanceof BowtiePixelGeoCoding);
    }

    @Test
    public void testScanLineOffset() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));

        // latitude values increasing
        BowtiePixelGeoCoding bowtiePixelGeoCoding = (BowtiePixelGeoCoding) product.getGeoCoding();
        assertEquals(0, bowtiePixelGeoCoding.getScanlineOffset());

        // flipped product, latitude values decreasing
        Product flippedProduct = ProductFlipper.createFlippedProduct(product, ProductFlipper.FLIP_BOTH, "f", "f");
        bowtiePixelGeoCoding = (BowtiePixelGeoCoding) flippedProduct.getGeoCoding();
        assertEquals(0, bowtiePixelGeoCoding.getScanlineOffset());

        // small product, just one scan (10 lines)
        testScanlineOffsetOnSubset(product, 0, 10, 0);
        // other small products, with different offsets
        testScanlineOffsetOnSubset(product, 0, 30, 0);
        testScanlineOffsetOnSubset(product, 1, 30, 9);
        testScanlineOffsetOnSubset(product, 2, 30, 8);
        testScanlineOffsetOnSubset(product, 3, 30, 7);
        testScanlineOffsetOnSubset(product, 4, 30, 6);
        testScanlineOffsetOnSubset(product, 5, 30, 5);
        testScanlineOffsetOnSubset(product, 6, 30, 4);
        testScanlineOffsetOnSubset(product, 7, 30, 3);
        testScanlineOffsetOnSubset(product, 8, 30, 2);
        testScanlineOffsetOnSubset(product, 9, 30, 1);
        testScanlineOffsetOnSubset(product, 10, 30, 0);
    }

    private static void testScanlineOffsetOnSubset(Product product, int yStart, int heigth, int scanlineOffset) throws IOException {
        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(0, yStart, product.getSceneRasterWidth(), heigth);
        Product subsetProduct = ProductSubsetBuilder.createProductSubset(product, subsetDef, "s", "s");
        BowtiePixelGeoCoding bowtiePixelGeoCoding = (BowtiePixelGeoCoding) subsetProduct.getGeoCoding();
        assertEquals("for y=" + yStart + " scanlineOffset", scanlineOffset, bowtiePixelGeoCoding.getScanlineOffset());
    }

}
