package org.esa.beam;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdfReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;

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
        if (parentFile == null) {
            return null;
        }
        File[] files = parentFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String fileName = pathname.getName();
                return fileName.startsWith("MER_") && fileName.endsWith("_meta.nc");
            }
        });
        File metaFile = files[0];
        files = parentFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String fileName = pathname.getName();
                return fileName.startsWith("MER_") && fileName.endsWith(".nc") && !fileName.endsWith("_meta.nc");
            }
        });
        return createProduct(metaFile, files);
    }

    private Product createProduct(File metaFile, File[] files) throws IOException {

        ProductReaderPlugIn readerPlugin = getReaderPlugin();
        Product product = readerPlugin.createReaderInstance().readProductNodes(metaFile, null);
        for (File file : files) {
            ProductReader readerInstance = readerPlugin.createReaderInstance();
            Product bandProduct = readerInstance.readProductNodes(file, null);
            Band[] bands = bandProduct.getBands();
            if (bands.length > 0) {
                product.addBand(bands[0]);
            }
            TiePointGrid[] grid = bandProduct.getTiePointGrids();
            if (grid.length > 0) {
                product.addTiePointGrid(grid[0]);
            }
        }

        return product;
    }

    private ProductReaderPlugIn getReaderPlugin() {
        ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
        Iterator<ProductReaderPlugIn> readerPlugIns = ioPlugInManager.getReaderPlugIns("NetCDF4-BEAM");
        while (readerPlugIns.hasNext()) {
            ProductReaderPlugIn next = readerPlugIns.next();
            if (next instanceof BeamNetCdfReaderPlugIn) {
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
}
