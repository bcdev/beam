/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.gsfc.seadas.dataio;

import static gov.nasa.gsfc.seadas.dataio.SeadasFileReader.getProductDataType;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos.HdfEosUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.jdom2.Element;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class L1BViirsFileReader extends SeadasFileReader {

    private static final int[] MODIS_WVL = new int[]{645, 859, 469, 555, 1240, 1640, 2130, 412, 443, 488, 531, 547, 667, 678,
            748, 869, 905, 936, 940, 3750, 3959, 3959, 4050, 4465, 4515, 1375, 6715, 7325, 8550, 9730, 11030, 12020,
            13335, 13635, 13935, 14235};

    protected String title;
    protected int scanMultiplier = 16;
    protected int numPixels = 0;
    protected int numScans = 0;
    protected int numLines = 0;

    L1BViirsFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        String productName = getStringAttribute("product_name");
        numPixels = getDimension("number_of_pixels");
        numScans = getDimension("number_of_scans");
        numLines = getDimension("number_of_lines");
        title = getStringAttribute("title");
        if (title.contains("I-band")) {
            scanMultiplier = 32;
        } 

        mustFlipX = mustFlipY = mustFlipViirs();

        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), numPixels, numLines);
        product.setDescription(productName);

        ProductData.UTC utcStart = getUTCAttribute("time_coverage_start");
        if (utcStart != null) {
            if (mustFlipY) {
                product.setEndTime(utcStart);
            } else {
                product.setStartTime(utcStart);
            }
        }
        ProductData.UTC utcEnd = getUTCAttribute("time_coverage_end");
        if (utcEnd != null) {
            if (mustFlipY) {
                product.setStartTime(utcEnd);
            } else {
                product.setEndTime(utcEnd);
            }
        }

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        addScientificMetadata(product);

        variableMap = addBands(product, ncFile.getVariables());

        addGeocoding(product);

        // todo - think about maybe possibly sometime creating a flag for questionable data
        addFlagsAndMasks(product);
        product.setAutoGrouping("RefSB:Emissive");

        return product;

    }
//
//    private float getScaleFactor(Variable variable) {
//        float scale_factor = 1.f;
//        final Attribute scale_factor_attribute = findAttribute("scale_factor", variable.getAttributes());
//        if (scale_factor_attribute != null) {
//            scale_factor = scale_factor_attribute.getNumericValue().floatValue();
//        }
//        return scale_factor;
//    }

    public void addGeocoding(final Product product) throws ProductIOException {

        // read latitudes and longitudes

        boolean externalGeo = false;
        NetcdfFile geoNcFile = null;
        Variable lats = null;
        Variable lons = null;
        int subSample = 1;
        float offsetX = 0f;
        float offsetY = 0f;
        try {
            File inputFile = productReader.getInputFile();
            String geoFileName = inputFile.getName().replaceAll("L1B", "GEO");
            String path = inputFile.getParent();
            File geocheck = new File(path, geoFileName);
            if (geocheck.exists()) {
                externalGeo = true;
                geoNcFile = NetcdfFileOpener.open(geocheck.getPath());
            
            
                lats = geoNcFile.findVariable("geolocation_data/latitude");
                lons = geoNcFile.findVariable("geolocation_data/longitude");


                //Use lat/lon with TiePointGeoCoding
                int[] dims = lats.getShape();
                float[] latTiePoints;
                float[] lonTiePoints;
                Array latarr = lats.read();
                Array lonarr = lons.read();
                if (mustFlipX && mustFlipY) {
                    latTiePoints = (float[]) latarr.flip(0).flip(1).copyTo1DJavaArray();
                    lonTiePoints = (float[]) lonarr.flip(0).flip(1).copyTo1DJavaArray();
                } else {
                    latTiePoints = (float[]) latarr.getStorage();
                    lonTiePoints = (float[]) lonarr.getStorage();
                }

                final TiePointGrid latGrid = new TiePointGrid("latitude", dims[1], dims[0], offsetX, offsetY,
                        subSample, subSample, latTiePoints);
                product.addTiePointGrid(latGrid);

                final TiePointGrid lonGrid = new TiePointGrid("longitude", dims[1], dims[0], offsetX, offsetY,
                        subSample, subSample, lonTiePoints, TiePointGrid.DISCONT_AT_180);
                product.addTiePointGrid(lonGrid);

                product.setGeoCoding(new BowtieTiePointGeoCoding(latGrid, lonGrid, scanMultiplier));

                geoNcFile.close();

            }

        } catch (Exception e) {
            throw new ProductIOException(e.getMessage());
        }

    }

    public boolean mustFlipViirs() throws ProductIOException {
        String startDirection = getStringAttribute("startDirection");
        String endDirection = getStringAttribute("endDirection");

        boolean startNodeAscending = false;
        boolean endNodeAscending = false;
        
        if (startDirection.equals("Ascending")) {
            startNodeAscending = true;
        }
        if (endDirection.equals("Ascending")) {
            endNodeAscending = true;
        }

        return (startNodeAscending && endNodeAscending);
    }

        private int getDimension(String dimensionName) {
        final List<Dimension> dimensions = ncFile.getDimensions();
        for (Dimension dimension : dimensions) {
            if (dimension.getShortName().equals(dimensionName)) {
                return dimension.getLength();
            }
        }
        return -1;
    }
}
