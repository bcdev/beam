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
package org.esa.beam.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.GeoTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import com.sun.media.imageio.plugins.tiff.TIFFTag;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageMetadata;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.imageioimpl.plugins.tiff.TIFFRenderedImage;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.dataio.geotiff.internal.GeoKeyEntry;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.FilterBand;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.GcpGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.geotiff.EPSGCodes;
import org.esa.beam.util.geotiff.GeoTIFFCodes;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.jai.JAIUtils;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffMetadata2CRSAdapter;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class GeoTiffProductReader extends AbstractProductReader {

    private static final int FIRST_IMAGE = 0;

    private ImageInputStream inputStream;
    private Map<Band, Integer> bandMap;

    private TIFFImageReader imageReader;

    public GeoTiffProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected synchronized Product readProductNodesImpl() throws IOException {
        File inputFile = null;
        Object input = getInput();
        if (input instanceof String) {
            input = new File((String) input);
        }
        if (input instanceof File) {
            inputFile = (File) input;
        }
        inputStream = ImageIO.createImageInputStream(input);
        return readGeoTIFFProduct(inputStream, inputFile);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final int destSize = destWidth * destHeight;
        pm.beginTask("Reading data...", 3);
        try {
            final Raster data = readRect(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                                         destOffsetX, destOffsetY, destWidth, destHeight);
            pm.worked(1);

            double[] dArray = new double[destSize];
            Integer bandIdx = bandMap.get(destBand);
            if (bandIdx == null) {
                bandIdx = 0;
            }
            final DataBuffer dataBuffer = data.getDataBuffer();
            final SampleModel sampleModel = data.getSampleModel();
            sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), bandIdx, dArray, dataBuffer);
            pm.worked(1);

            for (int i = 0; i < dArray.length; i++) {
                destBuffer.setElemDoubleAt(i, dArray[i]);
            }
            pm.worked(1);

        } finally {
            pm.done();
        }

    }

    private synchronized Raster readRect(int sourceOffsetX, int sourceOffsetY, int sourceStepX, int sourceStepY,
                                         int destOffsetX, int destOffsetY, int destWidth, int destHeight) throws
                                                                                                          IOException {
        ImageReadParam readParam = imageReader.getDefaultReadParam();
        int subsamplingXOffset = sourceOffsetX % sourceStepX;
        int subsamplingYOffset = sourceOffsetY % sourceStepY;
        readParam.setSourceSubsampling(sourceStepX, sourceStepY, subsamplingXOffset, subsamplingYOffset);
        RenderedImage subsampledImage = imageReader.readAsRenderedImage(FIRST_IMAGE, readParam);

        return subsampledImage.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        inputStream.close();
    }

    Product readGeoTIFFProduct(final ImageInputStream stream, final File inputFile) throws IOException {
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        while (imageReaders.hasNext()) {
            final ImageReader reader = imageReaders.next();
            if (reader instanceof TIFFImageReader) {
                imageReader = (TIFFImageReader) reader;
                break;
            }
        }
        if (imageReader == null) {
            throw new IOException("GeoTiff imageReader not found");
        }

        imageReader.setInput(stream);

        Product product = null;

        final TIFFImageMetadata imageMetadata = (TIFFImageMetadata) imageReader.getImageMetadata(FIRST_IMAGE);
        final TiffFileInfo tiffInfo = new TiffFileInfo(imageMetadata.getRootIFD());
        final TIFFField field = tiffInfo.getField(Utils.PRIVATE_BEAM_TIFF_TAG_NUMBER);
        if (field != null && field.getType() == TIFFTag.TIFF_ASCII) {
            final String s = field.getAsString(0).trim();
            if (s.contains("<Dimap_Document")) { // with DIMAP header
                InputStream is = null;
                try {
                    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    final DocumentBuilder builder = factory.newDocumentBuilder();
                    is = new ByteArrayInputStream(s.getBytes());
                    final Document document = new DOMBuilder().build(builder.parse(is));
                    product = DimapProductHelpers.createProduct(document);
                    removeGeoCodingAndTiePointGrids(product);
                    initBandsMap(product);
                } catch (ParserConfigurationException ignore) {
                    // ignore if it can not be read
                } catch (SAXException ignore) {
                    // ignore if it can not be read
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        }

        if (product == null) {            // without DIMAP header
            final String productName;
            if (tiffInfo.containsField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION)) {
                final TIFFField field1 = tiffInfo.getField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION);
                productName = field1.getAsString(0);
            } else if (inputFile != null) {
                productName = FileUtils.getFilenameWithoutExtension(inputFile);
            } else {
                productName = "geotiff";
            }
            final String productType = getReaderPlugIn().getFormatNames()[0];

            final int width = imageReader.getWidth(FIRST_IMAGE);
            final int height = imageReader.getHeight(FIRST_IMAGE);
            product = new Product(productName, productType, width, height, this);
            addBandsToProduct(tiffInfo, product);
        }

        if (tiffInfo.isGeotiff()) {
            applyGeoCoding(tiffInfo, imageMetadata, product);
        }

        TiffTagToMetadataConverter.addTiffTagsToMetadata(imageMetadata, tiffInfo, product.getMetadataRoot());

        if (inputFile != null) {
            initMetadata(product, inputFile);
            product.setFileLocation(inputFile);
        }
        setPreferredTiling(product);

        return product;
    }

    /**
     * Allow other metadata to be injected
     *
     * @param product   the Product
     * @param inputFile the source tiff file
     *
     * @throws IOException in case of an IO error
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void initMetadata(final Product product, final File inputFile) throws IOException {
        // don't remove
        // implemented by NEST
    }

    private void initBandsMap(Product product) {
        final Band[] bands = product.getBands();
        bandMap = new HashMap<Band, Integer>(bands.length);
        for (Band band : bands) {
            if (!(band instanceof VirtualBand || band instanceof FilterBand)) {
                bandMap.put(band, bandMap.size());
            }
        }
    }

    private static void removeGeoCodingAndTiePointGrids(Product product) {
        product.setGeoCoding(null);
        final TiePointGrid[] pointGrids = product.getTiePointGrids();
        for (TiePointGrid pointGrid : pointGrids) {
            product.removeTiePointGrid(pointGrid);
        }
    }

    private void addBandsToProduct(TiffFileInfo tiffInfo, Product product) throws
                                                                           IOException {
        final ImageReadParam readParam = imageReader.getDefaultReadParam();
        TIFFRenderedImage baseImage = (TIFFRenderedImage) imageReader.readAsRenderedImage(FIRST_IMAGE, readParam);
        SampleModel sampleModel = baseImage.getSampleModel();
        final int numBands = sampleModel.getNumBands();
        final int productDataType = ImageManager.getProductDataType(sampleModel.getDataType());
        bandMap = new HashMap<Band, Integer>(numBands);
        for (int i = 0; i < numBands; i++) {
            final String bandName = String.format("band_%d", i + 1);
            final Band band = product.addBand(bandName, productDataType);
            if (tiffInfo.containsField(
                    BaselineTIFFTagSet.TAG_COLOR_MAP) && baseImage.getColorModel() instanceof IndexColorModel) {
                band.setImageInfo(createIndexedImageInfo(product, baseImage, band));
            }
            bandMap.put(band, i);
        }
    }

    private void setPreferredTiling(Product product) throws IOException {
        final Dimension dimension;
        if (isBadTiling()) {
            dimension = JAIUtils.computePreferredTileSize(imageReader.getWidth(FIRST_IMAGE),
                                                          imageReader.getHeight(FIRST_IMAGE), 1);
        } else {
            dimension = new Dimension(imageReader.getTileWidth(FIRST_IMAGE), imageReader.getTileHeight(FIRST_IMAGE));
        }
        product.setPreferredTileSize(dimension);
    }

    private boolean isBadTiling() throws IOException {
        final int imageHeight = imageReader.getHeight(FIRST_IMAGE);
        final int tileHeight = imageReader.getTileHeight(FIRST_IMAGE);
        final int imageWidth = imageReader.getWidth(FIRST_IMAGE);
        final int tileWidth = imageReader.getTileWidth(FIRST_IMAGE);
        return tileWidth <= 1 || tileHeight <= 1 || imageWidth == tileWidth || imageHeight == tileHeight;
    }

    private static ImageInfo createIndexedImageInfo(Product product, TIFFRenderedImage baseImage, Band band) {
        final IndexColorModel colorModel = (IndexColorModel) baseImage.getColorModel();
        final IndexCoding indexCoding = new IndexCoding("color_map");
        final int colorCount = colorModel.getMapSize();
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[colorCount];
        for (int j = 0; j < colorCount; j++) {
            final String name = String.format("I%3d", j);
            indexCoding.addIndex(name, j, "");
            points[j] = new ColorPaletteDef.Point(j, new Color(colorModel.getRGB(j)), name);
        }
        product.getIndexCodingGroup().add(indexCoding);
        band.setSampleCoding(indexCoding);

        return new ImageInfo(new ColorPaletteDef(points, points.length));
    }

    private static void applyGeoCoding(TiffFileInfo info, TIFFImageMetadata metadata, Product product) {
        if (info.containsField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT)) {

            final double[] tiePoints = info.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT).getAsDoubles();

            if (canCreateTiePointGeoCoding(tiePoints)) {
                applyTiePointGeoCoding(info, tiePoints, product);
            } else if (canCreateGcpGeoCoding(tiePoints)) {
                applyGcpGeoCoding(info, tiePoints, product);
            }
        }

        if (product.getGeoCoding() == null) {
            try {
                applyGeoCodingFromGeoTiff(metadata, product);
            } catch (Exception ignored) {
            }
        }

    }

    private static void applyGeoCodingFromGeoTiff(TIFFImageMetadata metadata, Product product) throws Exception {
        final Rectangle imageBounds = new Rectangle(product.getSceneRasterWidth(),
                                                    product.getSceneRasterHeight());
        final GeoTiffIIOMetadataDecoder metadataDecoder = new GeoTiffIIOMetadataDecoder(metadata);
        final GeoTiffMetadata2CRSAdapter geoTiff2CRSAdapter = new GeoTiffMetadata2CRSAdapter(null);
        final MathTransform toModel = GeoTiffMetadata2CRSAdapter.getRasterToModel(metadataDecoder, false);
        CoordinateReferenceSystem crs;
        try {
            crs = geoTiff2CRSAdapter.createCoordinateSystem(metadataDecoder);
        } catch (UnsupportedOperationException e) {
            if (toModel == null) {
                throw e;
            } else {
                // ENVI falls back to WGS84, if no CRS is given in the GeoTIFF.
                crs = DefaultGeographicCRS.WGS84;
            }
        }
        final CrsGeoCoding geoCoding = new CrsGeoCoding(crs, imageBounds, (AffineTransform) toModel);
        product.setGeoCoding(geoCoding);
    }


    private static void applyTiePointGeoCoding(TiffFileInfo info, double[] tiePoints, Product product) {
        final SortedSet<Double> xSet = new TreeSet<Double>();
        final SortedSet<Double> ySet = new TreeSet<Double>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        final double xMin = xSet.first();
        final double xMax = xSet.last();
        final double xDiff = (xMax - xMin) / (xSet.size() - 1);
        final double yMin = ySet.first();
        final double yMax = ySet.last();
        final double yDiff = (yMax - yMin) / (ySet.size() - 1);

        final int width = xSet.size();
        final int height = ySet.size();

        int idx = 0;
        final Map<Double, Integer> xIdx = new HashMap<Double, Integer>();
        for (Double val : xSet) {
            xIdx.put(val, idx);
            idx++;
        }
        idx = 0;
        final Map<Double, Integer> yIdx = new HashMap<Double, Integer>();
        for (Double val : ySet) {
            yIdx.put(val, idx);
            idx++;
        }

        final float[] lats = new float[width * height];
        final float[] lons = new float[width * height];

        for (int i = 0; i < tiePoints.length; i += 6) {
            final int idxX = xIdx.get(tiePoints[i + 0]);
            final int idxY = yIdx.get(tiePoints[i + 1]);
            final int arrayIdx = idxY * width + idxX;
            lons[arrayIdx] = (float) tiePoints[i + 3];
            lats[arrayIdx] = (float) tiePoints[i + 4];
        }

        String[] names = Utils.findSuitableLatLonNames(product);
        final TiePointGrid latGrid = new TiePointGrid(
                names[0], width, height, (float) xMin, (float) yMin, (float) xDiff, (float) yDiff, lats);
        final TiePointGrid lonGrid = new TiePointGrid(
                names[1], width, height, (float) xMin, (float) yMin, (float) xDiff, (float) yDiff, lons);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, datum));
    }

    private static boolean canCreateGcpGeoCoding(final double[] tiePoints) {
        int numTiePoints = tiePoints.length / 6;

        if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL3.getTermCountP()) {
            return true;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL2.getTermCountP()) {
            return true;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL1.getTermCountP()) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean canCreateTiePointGeoCoding(final double[] tiePoints) {
        if ((tiePoints.length / 6) <= 1) {
            return false;
        }
        for (double tiePoint : tiePoints) {
            if (Double.isNaN(tiePoint)) {
                return false;
            }
        }
        final SortedSet<Double> xSet = new TreeSet<Double>();
        final SortedSet<Double> ySet = new TreeSet<Double>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        return isEquiDistance(xSet) && isEquiDistance(ySet);
    }

    private static boolean isEquiDistance(SortedSet<Double> set) {
        final double min = set.first();
        final double max = set.last();
        final double diff = (max - min) / (set.size() - 1);
        final double diff100000 = diff / 100000;
        final double maxDiff = diff + diff100000;
        final double minDiff = diff - diff100000;

        final Double[] values = set.toArray(new Double[set.size()]);
        for (int i = 1; i < values.length; i++) {
            final double currentDiff = values[i] - values[i - 1];
            if (currentDiff > maxDiff || currentDiff < minDiff) {
                return false;
            }
        }
        return true;
    }

    private static void applyGcpGeoCoding(final TiffFileInfo info,
                                          final double[] tiePoints,
                                          final Product product) {

        int numTiePoints = tiePoints.length / 6;

        final GcpGeoCoding.Method method;
        if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL3.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL3;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL2.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL2;
        } else if (numTiePoints >= GcpGeoCoding.Method.POLYNOMIAL1.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL1;
        } else {
            return; // not able to apply GCP geo coding; not enough tie points
        }

        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        final GcpDescriptor gcpDescriptor = GcpDescriptor.getInstance();
        final ProductNodeGroup<Placemark> gcpGroup = product.getGcpGroup();
        for (int i = 0; i < numTiePoints; i++) {
            final int offset = i * 6;

            final float x = (float) tiePoints[offset + 0];
            final float y = (float) tiePoints[offset + 1];
            final float lon = (float) tiePoints[offset + 3];
            final float lat = (float) tiePoints[offset + 4];

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(lon) || Double.isNaN(lat)) {
                continue;
            }
            final PixelPos pixelPos = new PixelPos(x, y);
            final GeoPos geoPos = new GeoPos(lat, lon);

            final String name = gcpDescriptor.getRoleName() + "_" + i;
            final String label = gcpDescriptor.getRoleLabel() + "_" + i;
            final Placemark gcp = Placemark.createPointPlacemark(gcpDescriptor, name, label, "", pixelPos, geoPos,
                                                                 product.getGeoCoding());
            gcpGroup.add(gcp);
        }

        final Placemark[] gcps = gcpGroup.toArray(new Placemark[gcpGroup.getNodeCount()]);
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        product.setGeoCoding(new GcpGeoCoding(method, gcps, width, height, datum));
    }

    private static Datum getDatum(Map<Integer, GeoKeyEntry> geoKeyEntries) {
        final Datum datum;
        if (geoKeyEntries.containsKey(GeoTIFFCodes.GeographicTypeGeoKey)) {
            final int value = geoKeyEntries.get(GeoTIFFCodes.GeographicTypeGeoKey).getIntValue();
            if (value == EPSGCodes.GCS_WGS_72) {
                datum = Datum.WGS_72;
            } else if (value == EPSGCodes.GCS_WGS_84) {
                datum = Datum.WGS_84;
            } else {
                //@todo if user defined ... make user defined datum
                datum = Datum.WGS_84;
            }
        } else {
            datum = Datum.WGS_84;
        }
        return datum;
    }

}
