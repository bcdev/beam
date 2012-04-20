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
package org.esa.beam.gpf.operators.standard;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.CRS;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BandMathsOpTest extends TestCase {

    private OperatorSpi bandMathsSpi;
    private ReadOp.Spi readSpi;

    @Override
    protected void setUp() throws Exception {
        readSpi = new ReadOp.Spi();
        bandMathsSpi = new BandMathsOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(readSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(bandMathsSpi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(bandMathsSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(readSpi);
    }

    public void testSimplestCase() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[1];
        bandDescriptors[0] = createBandDescription("aBandName", "1.0", ProductData.TYPESTRING_FLOAT32, "bigUnits");
        parameters.put("targetBands", bandDescriptors);
        Product sourceProduct = createTestProduct(4, 4);
        Product targetProduct = GPF.createProduct("BandMaths", parameters, sourceProduct);

        assertNotNull(targetProduct);
        Band band = targetProduct.getBand("aBandName");
        assertNotNull(band);
        assertEquals("aDescription", band.getDescription());
        assertEquals("bigUnits", band.getUnit());
        assertEquals(ProductData.TYPE_FLOAT32, band.getDataType());

        float[] floatValues = new float[16];
        band.readPixels(0, 0, 4, 4, floatValues, ProgressMonitor.NULL);
        float[] expectedValues = new float[16];
        Arrays.fill(expectedValues, 1.0f);
        assertTrue(Arrays.equals(expectedValues, floatValues));
    }

    public void testGeoCodingIsCopied() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[1];
        bandDescriptors[0] = createBandDescription("aBandName", "1.0", ProductData.TYPESTRING_UINT8, "simpleUnits");
        parameters.put("targetBands", bandDescriptors);
        Product sourceProduct = createTestProduct(4, 4);
        final GeoCoding geoCoding = new CrsGeoCoding(CRS.decode("EPSG:32632"), new Rectangle(4, 4),
                                                     new AffineTransform());
        sourceProduct.setGeoCoding(geoCoding);
        Product targetProduct = GPF.createProduct("BandMaths", parameters, sourceProduct);

        assertNotNull(targetProduct);
        assertNotNull(targetProduct.getGeoCoding());
    }

    public void testSimplestCaseWithFactoryMethod() throws Exception {
        Product sourceProduct = createTestProduct(4, 4);

        BandMathsOp bandMathsOp = BandMathsOp.createBooleanExpressionBand("band1 > 0", sourceProduct);
        assertNotNull(bandMathsOp);

        Product targetProduct = bandMathsOp.getTargetProduct();
        assertNotNull(targetProduct);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);
        assertEquals(ProductData.TYPE_INT8, band.getDataType());

        int[] intValues = new int[16];
        band.readPixels(0, 0, 4, 4, intValues, ProgressMonitor.NULL);
        int[] expectedValues = new int[16];
        Arrays.fill(expectedValues, 1);
        assertTrue(Arrays.equals(expectedValues, intValues));
    }

    public void testScaledInputBand() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[1];
        bandDescriptors[0] = createBandDescription("aBandName", "band3", ProductData.TYPESTRING_FLOAT32, "milliUnits");
        parameters.put("targetBands", bandDescriptors);
        Product sourceProduct = createTestProduct(4, 4);
        Product targetProduct = GPF.createProduct("BandMaths", parameters, sourceProduct);

        assertNotNull(targetProduct);
        Band band = targetProduct.getBand("aBandName");
        assertNotNull(band);
        assertEquals("aDescription", band.getDescription());
        assertEquals("milliUnits", band.getUnit());
        assertEquals(ProductData.TYPE_FLOAT32, band.getDataType());

        float[] floatValues = new float[16];
        band.readPixels(0, 0, 4, 4, floatValues, ProgressMonitor.NULL);
        float[] expectedValues = new float[16];
        Arrays.fill(expectedValues, 3.0f);
        assertTrue(Arrays.equals(expectedValues, floatValues));
    }

    public void testTwoSourceBandsOneTargetBand() throws Exception {
        Product sourceProduct = createTestProduct(4, 4);
        Map<String, Object> parameters = new HashMap<String, Object>();
        BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[1];
        bandDescriptors[0] = createBandDescription("aBandName", "band1 + band2", ProductData.TYPESTRING_FLOAT32, "");
        parameters.put("targetBands", bandDescriptors);

        Product targetProduct = GPF.createProduct("BandMaths", parameters, sourceProduct);
        Band band = targetProduct.getBand("aBandName");

        float[] actualValues = new float[16];
        band.readPixels(0, 0, 4, 4, actualValues, ProgressMonitor.NULL);
        float[] expectedValues = new float[16];
        Arrays.fill(expectedValues, 3.5f);
        assertTrue(Arrays.equals(expectedValues, actualValues));
    }

    public void testTwoSourceBandsTwoTargetBands() throws Exception {
        Product sourceProduct = createTestProduct(4, 4);
        Map<String, Object> parameters = new HashMap<String, Object>();
        BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[2];
        bandDescriptors[0] = createBandDescription("b1", "band1 + band2 < 3.0", ProductData.TYPESTRING_INT8, "milliUnit");
        bandDescriptors[1] = createBandDescription("b2", "band1 + band2 + 2.5", ProductData.TYPESTRING_INT32, "maxiUnit");
        parameters.put("targetBands", bandDescriptors);

        Product targetProduct = GPF.createProduct("BandMaths", parameters, sourceProduct);
        Band b1 = targetProduct.getBand("b1");

        assertEquals("milliUnit", b1.getUnit());
        b1.readRasterDataFully(ProgressMonitor.NULL);
        assertTrue(b1.getRasterData().getElems() instanceof byte[]);
        byte[] actualBooleanValues = (byte[]) b1.getRasterData().getElems();
        byte[] expectedBooleanValues = new byte[16];
        Arrays.fill(expectedBooleanValues, (byte) 0);
        assertTrue(Arrays.equals(expectedBooleanValues, actualBooleanValues));

        Band b2 = targetProduct.getBand("b2");

        assertEquals("maxiUnit", b2.getUnit());
        b2.readRasterDataFully(ProgressMonitor.NULL);
        assertTrue(b2.getRasterData().getElems() instanceof int[]);
        int[] actualIntValues = (int[]) b2.getRasterData().getElems();
        int[] expectedIntValues = new int[16];
        Arrays.fill(expectedIntValues, 6);
        assertTrue(Arrays.equals(expectedIntValues, actualIntValues));
    }

    public void testTwoSourceProductsOneTargetBand() throws Exception {
        Product sourceProduct1 = createTestProduct(4, 4);
        Product sourceProduct2 = createTestProduct(4, 4);
        Map<String, Object> parameters = new HashMap<String, Object>();
        BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[1];
        bandDescriptors[0] = createBandDescription("aBandName", "$sourceProduct.1.band1 + $sourceProduct.2.band2",
                                                   ProductData.TYPESTRING_FLOAT32, "milliUnit");
        parameters.put("targetBands", bandDescriptors);

        Product targetProduct = GPF.createProduct("BandMaths", parameters,
                                                  sourceProduct1, sourceProduct2);
        Band band = targetProduct.getBand("aBandName");

        float[] actualValues = new float[16];
        band.readPixels(0, 0, 4, 4, actualValues, ProgressMonitor.NULL);
        float[] expectedValues = new float[16];
        Arrays.fill(expectedValues, 3.5f);
        assertTrue(Arrays.equals(expectedValues, actualValues));
    }

    public void testGraph() throws Exception {
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        Class<BandMathsOp> opType = BandMathsOp.class;

        Graph graph = GraphIO.read(new InputStreamReader(getClass().getResourceAsStream("BandMathsOpTest.xml")));
        Node bandMathsNode = graph.getNode("bandMathsNode");
        DomElement configurationDomElement = bandMathsNode.getConfiguration();
        ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();

        PropertySet parameterSet = PropertyContainer.createMapBacked(parameterMap, opType, parameterDescriptorFactory);
        assertNotNull(parameterSet.getProperty("targetBands"));
        assertNull(parameterSet.getProperty("targetBands").getValue());
        assertNotNull(parameterSet.getProperty("variables"));
        assertNull(parameterSet.getProperty("variables").getValue());

        DefaultDomConverter domConverter = new DefaultDomConverter(opType, parameterDescriptorFactory);
        domConverter.convertDomToValue(configurationDomElement, parameterSet);
        assertNotNull(parameterSet.getProperty("targetBands"));
        assertNotNull(parameterSet.getProperty("targetBands").getValue());
        assertNotNull(parameterSet.getProperty("variables"));
        assertNotNull(parameterSet.getProperty("variables").getValue());

        Object targetBandsObj = parameterSet.getProperty("targetBands").getValue();
        assertTrue(targetBandsObj instanceof BandMathsOp.BandDescriptor[]);
        BandMathsOp.BandDescriptor[] targetBands = (BandMathsOp.BandDescriptor[]) targetBandsObj;
        assertEquals(2, targetBands.length);
        assertEquals("reflec_13", targetBands[0].name);
        assertEquals("reflec_14", targetBands[1].name);

        Object variablesObj = parameterSet.getProperty("variables").getValue();
        assertTrue(variablesObj instanceof BandMathsOp.Variable[]);
        BandMathsOp.Variable[] variables = (BandMathsOp.Variable[]) variablesObj;
        assertEquals(3, variables.length);
        assertEquals("SOLAR_FLUX_13", variables[0].name);
        assertEquals("SOLAR_FLUX_14", variables[1].name);
        assertEquals("PI", variables[2].name);

        InputStreamReader inputStreamReader = new InputStreamReader(getClass().getResourceAsStream("BandMathsOpParameters.xml"));
        String expectedXML = FileUtils.readText(inputStreamReader).trim();
        DefaultDomElement bibo = new DefaultDomElement("bibo");
        domConverter.convertValueToDom(parameterSet, bibo);
        assertEquals(expectedXML, bibo.toXml().trim());
    }

    private static BandMathsOp.BandDescriptor createBandDescription(String bandName, String expression, String type, String unit) {
        BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
        bandDescriptor.name = bandName;
        bandDescriptor.description = "aDescription";
        bandDescriptor.expression = expression;
        bandDescriptor.type = type;
        bandDescriptor.unit = unit;
        return bandDescriptor;
    }


    private static Product createTestProduct(int w, int h) {
        Product testProduct = new Product("p", "t", w, h);
        Band band1 = testProduct.addBand("band1", ProductData.TYPE_INT32);
        int[] intValues = new int[w * h];
        Arrays.fill(intValues, 1);
        band1.setData(ProductData.createInstance(intValues));

        Band band2 = testProduct.addBand("band2", ProductData.TYPE_FLOAT32);
        float[] floatValues = new float[w * h];
        Arrays.fill(floatValues, 2.5f);
        band2.setData(ProductData.createInstance(floatValues));

        Band band3 = testProduct.addBand("band3", ProductData.TYPE_INT16);
        band3.setScalingFactor(0.5);
        short[] shortValues = new short[w * h];
        Arrays.fill(shortValues, (short) 6);
        band3.setData(ProductData.createInstance(shortValues));
        return testProduct;
    }
}
