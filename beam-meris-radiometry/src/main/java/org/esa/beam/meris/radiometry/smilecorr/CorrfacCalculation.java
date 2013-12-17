package org.esa.beam.meris.radiometry.smilecorr;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.StringUtils;

import java.awt.image.Raster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
 */
public class CorrfacCalculation {

    private static final float DEFAULT_AMF_VALUE = 5.0f;
    private static final int BAND_11_INDEX = 10;
    private static final List<SrfData> SRFDataList = new ArrayList<SrfData>();

    static {
        // wavelength -> spatial response function
        SRFDataList.add(new SrfData(757.617f, 5.15E-05f));
        SRFDataList.add(new SrfData(757.717f, 9.63E-05f));
        SRFDataList.add(new SrfData(757.817f, 1.76E-04f));
        SRFDataList.add(new SrfData(757.917f, 3.15E-04f));
        SRFDataList.add(new SrfData(758.017f, 5.53E-04f));
        SRFDataList.add(new SrfData(758.117f, 9.49E-04f));
        SRFDataList.add(new SrfData(758.217f, 1.59E-03f));
        SRFDataList.add(new SrfData(758.317f, 2.62E-03f));
        SRFDataList.add(new SrfData(758.417f, 4.23E-03f));
        SRFDataList.add(new SrfData(758.517f, 6.67E-03f));
        SRFDataList.add(new SrfData(758.617f, 1.03E-02f));
        SRFDataList.add(new SrfData(758.717f, 1.56E-02f));
        SRFDataList.add(new SrfData(758.817f, 2.31E-02f));
        SRFDataList.add(new SrfData(758.917f, 3.35E-02f));
        SRFDataList.add(new SrfData(759.017f, 4.75E-02f));
        SRFDataList.add(new SrfData(759.117f, 6.61E-02f));
        SRFDataList.add(new SrfData(759.217f, 9.00E-02f));
        SRFDataList.add(new SrfData(759.317f, 1.20E-01f));
        SRFDataList.add(new SrfData(759.417f, 1.57E-01f));
        SRFDataList.add(new SrfData(759.517f, 2.01E-01f));
        SRFDataList.add(new SrfData(759.617f, 2.52E-01f));
        SRFDataList.add(new SrfData(759.717f, 3.09E-01f));
        SRFDataList.add(new SrfData(759.817f, 3.73E-01f));
        SRFDataList.add(new SrfData(759.917f, 4.40E-01f));
        SRFDataList.add(new SrfData(760.017f, 5.10E-01f));
        SRFDataList.add(new SrfData(760.117f, 5.80E-01f));
        SRFDataList.add(new SrfData(760.217f, 6.48E-01f));
        SRFDataList.add(new SrfData(760.317f, 7.12E-01f));
        SRFDataList.add(new SrfData(760.417f, 7.69E-01f));
        SRFDataList.add(new SrfData(760.517f, 8.19E-01f));
        SRFDataList.add(new SrfData(760.617f, 8.61E-01f));
        SRFDataList.add(new SrfData(760.717f, 8.94E-01f));
        SRFDataList.add(new SrfData(760.817f, 9.20E-01f));
        SRFDataList.add(new SrfData(760.917f, 9.39E-01f));
        SRFDataList.add(new SrfData(761.017f, 9.54E-01f));
        SRFDataList.add(new SrfData(761.117f, 9.64E-01f));
        SRFDataList.add(new SrfData(761.217f, 9.72E-01f));
        SRFDataList.add(new SrfData(761.317f, 9.79E-01f));
        SRFDataList.add(new SrfData(761.417f, 9.85E-01f));
        SRFDataList.add(new SrfData(761.517f, 9.90E-01f));
        SRFDataList.add(new SrfData(761.617f, 9.95E-01f));
        SRFDataList.add(new SrfData(761.717f, 9.98E-01f));
        SRFDataList.add(new SrfData(761.817f, 1.00E+00f));
        SRFDataList.add(new SrfData(761.917f, 1.00E+00f));
        SRFDataList.add(new SrfData(762.017f, 9.98E-01f));
        SRFDataList.add(new SrfData(762.117f, 9.95E-01f));
        SRFDataList.add(new SrfData(762.217f, 9.91E-01f));
        SRFDataList.add(new SrfData(762.317f, 9.86E-01f));
        SRFDataList.add(new SrfData(762.417f, 9.80E-01f));
        SRFDataList.add(new SrfData(762.517f, 9.73E-01f));
        SRFDataList.add(new SrfData(762.617f, 9.65E-01f));
        SRFDataList.add(new SrfData(762.717f, 9.55E-01f));
        SRFDataList.add(new SrfData(762.817f, 9.42E-01f));
        SRFDataList.add(new SrfData(762.917f, 9.24E-01f));
        SRFDataList.add(new SrfData(763.017f, 8.99E-01f));
        SRFDataList.add(new SrfData(763.117f, 8.67E-01f));
        SRFDataList.add(new SrfData(763.217f, 8.26E-01f));
        SRFDataList.add(new SrfData(763.317f, 7.77E-01f));
        SRFDataList.add(new SrfData(763.417f, 7.21E-01f));
        SRFDataList.add(new SrfData(763.517f, 6.58E-01f));
        SRFDataList.add(new SrfData(763.617f, 5.91E-01f));
        SRFDataList.add(new SrfData(763.717f, 5.21E-01f));
        SRFDataList.add(new SrfData(763.817f, 4.51E-01f));
        SRFDataList.add(new SrfData(763.917f, 3.83E-01f));
        SRFDataList.add(new SrfData(764.017f, 3.19E-01f));
        SRFDataList.add(new SrfData(764.117f, 2.60E-01f));
        SRFDataList.add(new SrfData(764.217f, 2.08E-01f));
        SRFDataList.add(new SrfData(764.317f, 1.63E-01f));
        SRFDataList.add(new SrfData(764.417f, 1.25E-01f));
        SRFDataList.add(new SrfData(764.517f, 9.41E-02f));
        SRFDataList.add(new SrfData(764.617f, 6.93E-02f));
        SRFDataList.add(new SrfData(764.717f, 5.00E-02f));
        SRFDataList.add(new SrfData(764.817f, 3.53E-02f));
        SRFDataList.add(new SrfData(764.917f, 2.44E-02f));
        SRFDataList.add(new SrfData(765.017f, 1.65E-02f));
        SRFDataList.add(new SrfData(765.117f, 1.10E-02f));
        SRFDataList.add(new SrfData(765.217f, 7.12E-03f));
        SRFDataList.add(new SrfData(765.317f, 4.53E-03f));
        SRFDataList.add(new SrfData(765.417f, 2.82E-03f));
        SRFDataList.add(new SrfData(765.517f, 1.72E-03f));
        SRFDataList.add(new SrfData(765.617f, 1.02E-03f));
        SRFDataList.add(new SrfData(765.717f, 5.99E-04f));
        SRFDataList.add(new SrfData(765.817f, 3.42E-04f));
        SRFDataList.add(new SrfData(765.917f, 1.92E-04f));
    }

    public static final double WVL_STEP = 0.1;

    public static void main(String[] args) throws IOException {
        Product product = ProductIO.readProduct(args[0]);
        String productType = args[1];
        calculate(product, productType);

    }

    private static void calculate(Product product, String productType) throws IOException {
        Band transmissionBand = product.getBand("transmission");
        MetadataElement metadataRoot = product.getMetadataRoot();
        float[] amfValues = getElementValues(metadataRoot, "amf");
        float[] wvlValue = getElementValues(metadataRoot, "wvl");


        System.out.println("wvlValue = " + StringUtils.arrayToCsv(wvlValue));
        System.out.println("amfValues = " + StringUtils.arrayToCsv(amfValues));

        int indexForAmf = findIndexForAmf(amfValues, DEFAULT_AMF_VALUE);
        System.out.println("indexForAmf = " + indexForAmf);

        Raster transmissionRaster = transmissionBand.getSourceImage().getData();
        float[] transmissionData = new float[transmissionBand.getSceneRasterHeight()];
        transmissionRaster.getSamples(indexForAmf, 0, 1, transmissionBand.getSceneRasterHeight(), 0, transmissionData);

        SmileCorrectionAuxdata auxdata = SmileCorrectionAuxdata.loadAuxdata(productType);
        double nomWvl = auxdata.getTheoreticalWavelengths()[BAND_11_INDEX];
        System.out.println("nomWvl = " + nomWvl);

        float srfSum = 0;
        for (SrfData value : SRFDataList) {
            srfSum += value.srfValue;
        }
        srfSum *= WVL_STEP;
        System.out.println("srfSum = " + srfSum);

        float srfMax= Float.NEGATIVE_INFINITY;
        for (SrfData srfEntry : SRFDataList) {
            float normValue = srfEntry.srfValue / srfSum;
            srfEntry.srfValue = normValue;
            srfMax = Math.max(srfMax, normValue);
        }

        for (int i = 0; i < SRFDataList.size(); i++) {
            SrfData entry = SRFDataList.get(i);
            if (entry.srfValue == srfMax) {
                System.out.println("index = " + i);
                System.out.println("wavelength = " + entry.wavelength);
                System.out.println("srf = " + entry.srfValue);
            }
        }
    }

    private static int findIndexForAmf(float[] amfValues, float valueToFind) {
        for (int i = 0; i < amfValues.length; i++) {
            float amfValue = amfValues[i];
            if (amfValue == valueToFind) {
                return i;
            }
        }
        throw new IllegalArgumentException(String.format("Value '%f' not found in array.", valueToFind));
    }

    private static float[] getElementValues(MetadataElement metadataRoot, String elementName) {
        MetadataElement variableAttributes = metadataRoot.getElement("Variable_Attributes");
        MetadataElement amf = variableAttributes.getElement(elementName);
        MetadataElement values = amf.getElement("Values");
        MetadataAttribute data = values.getAttribute("data");
        return (float[]) data.getDataElems();
    }

    private static class SrfData {
        float wavelength;
        float srfValue;

        private SrfData(float wavelength, float srfValue) {
            this.wavelength = wavelength;
            this.srfValue = srfValue;
        }
    }
}
