package org.esa.beam;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdfReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BasicPixelGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoCodingFactory;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.ProductUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Marco Peters
 */
public class MultiNetcdfProductReader extends AbstractProductReader {

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected MultiNetcdfProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File inputFile = new File(getInput().toString());
        File parentFile = inputFile.getParentFile();
        if(parentFile == null) {
            return null;
        }
        File[] files = parentFile.listFiles(new NetcdfFileFilter());
        return createProduct(files);
    }

    private Product createProduct(File[] files) throws IOException {

        ProductReaderPlugIn readerPlugin = getReaderPlugin();
        Product product = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            ProductReader readerInstance = readerPlugin.createReaderInstance();
            Product bandProduct = readerInstance.readProductNodes(file, null);
            if (i == 0) {
                String name = bandProduct.getName();
                product = new Product(name.substring(0, name.length() - 4), bandProduct.getProductType(),
                                      bandProduct.getSceneRasterWidth(), bandProduct.getSceneRasterHeight());
            }
            Band[] bands = bandProduct.getBands();
            if(bands.length > 0) {
                ProductUtils.copyBand(bands[0].getName(), bandProduct, product, true);
            }
            TiePointGrid[] grids = bandProduct.getTiePointGrids();
            if(grids.length > 0) {
                ProductUtils.copyTiePointGrid(grids[0].getName(), bandProduct, product);
            }
            MetadataElement metadataRoot = bandProduct.getMetadataRoot();
            if(metadataRoot != null) {
                metadataRoot.removeElement(metadataRoot.getElement("Global_Attributes"));
                metadataRoot.removeElement(metadataRoot.getElement("Variable_Attributes"));
                ProductUtils.copyMetadata(bandProduct, product);
            }
        }

        addGeoCoding(product);
        return product;
    }

    private void addGeoCoding(Product product) {
        TiePointGrid latGrid = product.getTiePointGrid("latitude");
        TiePointGrid lonGrid = product.getTiePointGrid("longitude");
        if (latGrid != null && lonGrid != null) {
            product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        }

        Band latBand = product.getBand("corr_latitude");
        Band lonBand = product.getBand("corr_longitude");
        GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, "NOT l1_flags.INVALID", 6);
        product.setGeoCoding(pixelGeoCoding);
    }

    private ProductReaderPlugIn getReaderPlugin() {
        ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
        Iterator<ProductReaderPlugIn> readerPlugIns = ioPlugInManager.getReaderPlugIns("NetCDF4-BEAM");
        while (readerPlugIns.hasNext()) {
            ProductReaderPlugIn next = readerPlugIns.next();
            if(next instanceof BeamNetCdfReaderPlugIn) {
                return next;
            }

        }
        throw new IllegalStateException("BeamNetCdfReaderPlugIn not found");
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

    }

    private class NetcdfFileFilter implements FileFilter {

        private String suffix = getReaderPlugin().getDefaultFileExtensions()[0];

        @Override
        public boolean accept(File pathname) {
            String fileName = pathname.getName();
            return fileName.endsWith(suffix);
        }
    }
}
