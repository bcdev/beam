package org.esa.beam;

import com.bc.ceres.core.PrintWriterProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
public class N1toMultiNetcdfConverter {

    public static final String OUTPUT_FORMAT = "NetCDF4-BEAM";

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("At least source product and target directory must be given as parameter.");
        }
        Product sourceProduct = ProductIO.readProduct(args[0]);
        File targetDir = new File(args[1]);
        if (!targetDir.isDirectory()) {
            throw new IllegalArgumentException(String.format("Target path is not a directory '%s'.", args[1]));
        }
        File[] files = targetDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if(!file.delete()) {
                    Logger.getAnonymousLogger().warning("Could not delete file: " + file.getCanonicalPath());
                }
            }
        }

        System.out.println("Writing to: " + targetDir.getCanonicalPath());

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();

        Band[] bands = sourceProduct.getBands();
        TiePointGrid[] grids = sourceProduct.getTiePointGrids();
        RasterDataNode[] rasters = new RasterDataNode[bands.length + grids.length];
        System.arraycopy(bands, 0, rasters, 0, bands.length);
        System.arraycopy(grids, 0, rasters, bands.length, grids.length);

        int rasterIndex = 0;
        PrintWriterProgressMonitor pm = new PrintWriterProgressMonitor(System.out);
        pm.beginTask("Converting bands to " + OUTPUT_FORMAT, rasters.length);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            for (RasterDataNode raster : rasters) {
                String targetProductName = String.format("%s_%03d", FileUtils.getFilenameWithoutExtension(sourceProduct.getName()), rasterIndex);
                Product subset = subset(raster);
                subset.setName(targetProductName);
                if(rasterIndex == 0) {
                    ProductUtils.copyMetadata(sourceProduct, subset);
                    subset.setStartTime(sourceProduct.getStartTime());
                    subset.setEndTime(sourceProduct.getEndTime());
                }
                ProductIO.writeProduct(subset, new File(targetDir, targetProductName + ".nc"), OUTPUT_FORMAT, false, ProgressMonitor.NULL);
                subset.dispose();
                pm.worked(1);
                rasterIndex++;
            }
        } finally {
            pm.done();
        }
        stopWatch.stop();
        System.out.println("Duration: " + stopWatch.getTimeDiffString());

    }

    static Product subset(RasterDataNode raster) throws IOException {
        Product source = raster.getProduct();
        Product subset = new Product("subset", source.getProductType(), source.getSceneRasterWidth(), source.getSceneRasterHeight());
        if (raster instanceof Band) {
            ProductUtils.copyBand(raster.getName(), source, subset, true);
        }else {
            ProductUtils.copyTiePointGrid(raster.getName(), source, subset);
        }
        return subset;
    }

}
