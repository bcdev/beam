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
package org.esa.beam.dataio.dimap;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.geometry.VectorDataNodeIO;
import org.esa.beam.dataio.geometry.VectorDataNodeWriter;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FilterBand;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

/**
 * The product writer for the BEAM-DIMAP format.
 * <p/>
 * The BEAM-DIMAP version history is provided in the API doc of the {@link DimapProductWriterPlugIn}.
 *
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see DimapProductWriterPlugIn
 * @see DimapProductReaderPlugIn
 */
public class DimapProductWriter extends AbstractProductWriter {

    private File _outputDir;
    private File _outputFile;
    private Map<Band, ImageOutputStream> _bandOutputStreams;
    private File _dataOutputDir;
    private boolean _incremental = true;

    /**
     * Construct a new instance of a product writer for the given BEAM-DIMAP product writer plug-in.
     *
     * @param writerPlugIn the given BEAM-DIMAP product writer plug-in, must not be <code>null</code>
     */
    public DimapProductWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    /**
     * Returns the output directory of the product beeing written.
     */
    public File getOutputDir() {
        return _outputDir;
    }

    /**
     * Returns all band output streams opened so far.
     */
    public Map<Band, ImageOutputStream> getBandOutputStreams() {
        return _bandOutputStreams;
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws java.io.IOException      if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {
        final Object output = getOutput();

        File outputFile = null;
        if (output instanceof String) {
            outputFile = new File((String) output);
        } else if (output instanceof File) {
            outputFile = (File) output;
        }
        Debug.assertNotNull(outputFile); // super.writeProductNodes should have checked this already
        initDirs(outputFile);

        ensureNamingConvention();
        writeDimapDocument();
        writeVectorData();
        writeTiePointGrids();
        getSourceProduct().setProductWriter(this);
        deleteRemovedNodes();
    }

    /**
     * Initializes all the internal file and directory elements from the given output file. This method only must be
     * called if the product writer should write the given data to raw data files without calling of writeProductNodes.
     * This may be at the time when a dimap product was opened and the data should be continuously changed in the same
     * product file without an previous call to the saveProductNodes to this product writer.
     *
     * @param outputFile the dimap header file location.
     * @throws java.io.IOException if an I/O error occurs
     */
    public void initDirs(final File outputFile) throws IOException {
        _outputFile = FileUtils.ensureExtension(outputFile, DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
        Debug.assertNotNull(_outputFile); // super.writeProductNodes should have checked this already
        _outputDir = _outputFile.getParentFile();
        if (_outputDir == null) {
            _outputDir = new File(".");
        }
        _dataOutputDir = createDataOutputDir();
        _dataOutputDir.mkdirs();

        if (!_dataOutputDir.exists()) {
            throw new IOException("failed to create data output directory: " + _dataOutputDir.getPath()); /*I18N*/
        }
    }

    private File createDataOutputDir() {
        return new File(_outputDir,
                        FileUtils.getFilenameWithoutExtension(
                                _outputFile) + DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION);
    }

    private void ensureNamingConvention() {
        if (_outputFile != null) {
            getSourceProduct().setName(FileUtils.getFilenameWithoutExtension(_outputFile));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBandRasterData(Band sourceBand,
                                    int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight,
                                    ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("sourceBand", sourceBand);
        Guardian.assertNotNull("sourceBuffer", sourceBuffer);
        checkBufferSize(sourceWidth, sourceHeight, sourceBuffer);
        final long sourceBandWidth = sourceBand.getSceneRasterWidth();
        final long sourceBandHeight = sourceBand.getSceneRasterHeight();
        checkSourceRegionInsideBandRegion(sourceWidth, sourceBandWidth, sourceHeight, sourceBandHeight, sourceOffsetX,
                                          sourceOffsetY);
        final ImageOutputStream outputStream = getOrCreateImageOutputStream(sourceBand);
        long outputPos = (long)sourceOffsetY * sourceBandWidth + (long)sourceOffsetX;
        pm.beginTask("Writing band '" + sourceBand.getName() + "'...", sourceHeight);
        try {
            for (int sourcePos = 0; sourcePos < sourceHeight * sourceWidth; sourcePos += sourceWidth) {
                sourceBuffer.writeTo(sourcePos, sourceWidth, outputStream, outputPos);
                outputPos += sourceBandWidth;
                pm.worked(1);
                if (pm.isCanceled()) {
                    break;
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Deletes the physically representation of the product from the hard disk.
     */
    @Override
    public void deleteOutput() throws IOException {
        flush();
        close();
        if (_outputFile != null && _outputFile.exists() && _outputFile.isFile()) {
            _outputFile.delete();
        }

        if (_dataOutputDir != null && _dataOutputDir.exists() && _dataOutputDir.isDirectory()) {
            FileUtils.deleteTree(_dataOutputDir);
        }
    }

    private static void checkSourceRegionInsideBandRegion(int sourceWidth, final long sourceBandWidth, int sourceHeight,
                                                          final long sourceBandHeight, int sourceOffsetX,
                                                          int sourceOffsetY) {
        Guardian.assertWithinRange("sourceWidth", sourceWidth, 1, sourceBandWidth);
        Guardian.assertWithinRange("sourceHeight", sourceHeight, 1, sourceBandHeight);
        Guardian.assertWithinRange("sourceOffsetX", sourceOffsetX, 0, sourceBandWidth - sourceWidth);
        Guardian.assertWithinRange("sourceOffsetY", sourceOffsetY, 0, sourceBandHeight - sourceHeight);
    }

    private static void checkBufferSize(int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        final int expectedBufferSize = sourceWidth * sourceHeight;
        final int actualBufferSize = sourceBuffer.getNumElems();
        Guardian.assertEquals("sourceWidth * sourceHeight", actualBufferSize, expectedBufferSize);  /*I18N*/
    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws java.io.IOException on failure
     */
    @Override
    public synchronized void flush() throws IOException {
        if (_bandOutputStreams == null) {
            return;
        }
        for (ImageOutputStream imageOutputStream : _bandOutputStreams.values()) {
            imageOutputStream.flush();
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    @Override
    public synchronized void close() throws IOException {
        if (_bandOutputStreams == null) {
            return;
        }
        for (ImageOutputStream imageOutputStream : _bandOutputStreams.values()) {
            (imageOutputStream).close();
        }
        _bandOutputStreams.clear();
        _bandOutputStreams = null;
    }

    private void writeDimapDocument() throws IOException {
        final DimapHeaderWriter writer = new DimapHeaderWriter(getSourceProduct(), getOutputFile(),
                                                               _dataOutputDir.getName());
        writer.writeHeader();
        writer.close();
    }

    private File getOutputFile() {
        return _outputFile;
    }

    private void writeTiePointGrids() throws IOException {
        for (int i = 0; i < getSourceProduct().getNumTiePointGrids(); i++) {
            final TiePointGrid tiePointGrid = getSourceProduct().getTiePointGridAt(i);
            writeTiePointGrid(tiePointGrid);
        }
    }

    private void writeTiePointGrid(TiePointGrid tiePointGrid) throws IOException {
        ensureExistingTiePointGridDir();
        final ImageOutputStream outputStream = createImageOutputStream(tiePointGrid);
        tiePointGrid.getData().writeTo(outputStream);
        outputStream.close();
    }

    private void ensureExistingTiePointGridDir() {
        final File tiePointGridDir = new File(_dataOutputDir, DimapProductConstants.TIE_POINT_GRID_DIR_NAME);
        tiePointGridDir.mkdirs();
    }

    /*
     * Returns the data output stream associated with the given <code>Band</code>. If no stream exists, one is created
     * and fed into the hash map
     */

    private synchronized ImageOutputStream getOrCreateImageOutputStream(Band band) throws IOException {
        ImageOutputStream outputStream = getImageOutputStream(band);
        if (outputStream == null) {
            outputStream = createImageOutputStream(band);
            if (_bandOutputStreams == null) {
                _bandOutputStreams = new HashMap<Band, ImageOutputStream>();
            }
            _bandOutputStreams.put(band, outputStream);
        }
        return outputStream;
    }

    private synchronized ImageOutputStream getImageOutputStream(Band band) {
        if (_bandOutputStreams != null) {
            return _bandOutputStreams.get(band);
        }
        return null;
    }

    /*
     * Returns a file associated with the given <code>Band</code>. The method ensures that the file exists and have the
     * right size. Also ensures a recreate if the file not exists or the file have a different file size. A new envi
     * header file was written every call.
     */

    private File getValidImageFile(Band band) throws IOException {
        writeEnviHeader(band); // always (re-)write ENVI header
        final File file = getImageFile(band);
        if (file.exists()) {
            if (file.length() != getImageFileSize(band)) {
                createPhysicalImageFile(band, file);
            }
        } else {
            createPhysicalImageFile(band, file);
        }
        return file;
    }

    private File getValidImageFile(TiePointGrid tiePointGrid) throws IOException {
        writeEnviHeader(tiePointGrid); // always (re-)write ENVI header
        final File file = getImageFile(tiePointGrid);
        createPhysicalImageFile(tiePointGrid, file);
        return file;
    }

    private static void createPhysicalImageFile(Band band, File file) throws IOException {
        createPhysicalFile(file, getImageFileSize(band));
    }

    private static void createPhysicalImageFile(TiePointGrid tiePointGrid, File file) throws IOException {
        createPhysicalFile(file, getImageFileSize(tiePointGrid));
    }

    private void writeEnviHeader(Band band) throws IOException {
        EnviHeader.createPhysicalFile(getEnviHeaderFile(band),
                                      band,
                                      band.getRasterWidth(),
                                      band.getRasterHeight());
    }

    private void writeEnviHeader(TiePointGrid tiePointGrid) throws IOException {
        EnviHeader.createPhysicalFile(getEnviHeaderFile(tiePointGrid),
                                      tiePointGrid,
                                      tiePointGrid.getRasterWidth(),
                                      tiePointGrid.getRasterHeight());
    }

    private ImageOutputStream createImageOutputStream(Band band) throws IOException {
        return new FileImageOutputStream(getValidImageFile(band));
    }

    private ImageOutputStream createImageOutputStream(TiePointGrid tiePointGrid) throws IOException {
        return new FileImageOutputStream(getValidImageFile(tiePointGrid));
    }

    private static long getImageFileSize(RasterDataNode band) {
        return (long) ProductData.getElemSize(band.getDataType()) *
                (long) band.getRasterWidth() *
                (long) band.getRasterHeight();
    }

    private File getEnviHeaderFile(Band band) {
        return new File(_dataOutputDir, createEnviHeaderFilename(band));
    }

    private static String createEnviHeaderFilename(Band band) {
        return band.getName() + EnviHeader.FILE_EXTENSION;
    }

    private File getEnviHeaderFile(TiePointGrid tiePointGrid) {
        return new File(new File(_dataOutputDir, DimapProductConstants.TIE_POINT_GRID_DIR_NAME),
                        tiePointGrid.getName() + EnviHeader.FILE_EXTENSION);
    }

    private File getImageFile(Band band) {
        return new File(_dataOutputDir, createImageFilename(band));
    }

    private static String createImageFilename(Band band) {
        return band.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION;
    }

    private File getImageFile(TiePointGrid tiePointGrid) {
        return new File(new File(_dataOutputDir, DimapProductConstants.TIE_POINT_GRID_DIR_NAME),
                        tiePointGrid.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION);
    }

    private static void createPhysicalFile(File file, long fileSize) throws IOException {
        final File parentDir = file.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        try {
            randomAccessFile.setLength(fileSize);
        } finally {
            randomAccessFile.close();
        }
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        if (node instanceof VirtualBand) {
            return false;
        }
        if (node instanceof FilterBand) {
            return false;
        }
        if (node.isModified()) {
            return true;
        }
        if (!isIncrementalMode()) {
            return true;
        }
        if (!(node instanceof Band)) {
            return true;
        }
        final File imageFile = getImageFile((Band) node);
        return !(imageFile != null && imageFile.exists());
    }

    /**
     * Enables resp. disables incremental writing of this product writer. By default, a reader should enable progress
     * listening.
     *
     * @param enabled enables or disables progress listening.
     */
    @Override
    public void setIncrementalMode(boolean enabled) {
        _incremental = enabled;
    }

    /**
     * Returns whether this product writer writes only modified product nodes.
     *
     * @return <code>true</code> if so
     */
    @Override
    public boolean isIncrementalMode() {
        return _incremental;
    }

    private void deleteRemovedNodes() throws IOException {
        final Product product = getSourceProduct();
        final ProductReader productReader = product.getProductReader();
        if (productReader instanceof DimapProductReader) {
            final ProductNode[] removedNodes = product.getRemovedChildNodes();
            if (removedNodes.length > 0) {
                productReader.close();
                for (ProductNode removedNode : removedNodes) {
                    removedNode.removeFromFile(this);
                }
            }
        }
    }

    @Override
    public void removeBand(Band band) {
        if (band != null) {
            final String headerFilename = createEnviHeaderFilename(band);
            final String imageFilename = createImageFilename(band);
            File[] files = null;
            if (_dataOutputDir != null && _dataOutputDir.exists()) {
                files = _dataOutputDir.listFiles();
            }
            if (files == null) {
                return;
            }
            for (File file : files) {
                String name = file.getName();
                if (file.isFile() && (name.equals(headerFilename) || name.equals(imageFilename))) {
                    file.delete();
                }
            }
        }
    }

    private void writeVectorData() {
        Product product = getSourceProduct();
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();

        File vectorDataDir = new File(_dataOutputDir, "vector_data");
        if (vectorDataDir.exists()) {
            File[] files = vectorDataDir.listFiles();
            for (File file : files) {
                file.delete();
            }
        }

        if (vectorDataGroup.getNodeCount() > 0) {
            vectorDataDir.mkdirs();
            for (int i = 0; i < vectorDataGroup.getNodeCount(); i++) {
                VectorDataNode vectorDataNode = vectorDataGroup.get(i);
                writeVectorData(vectorDataDir, vectorDataNode);
            }
        }
    }

    private void writeVectorData(File vectorDataDir, VectorDataNode vectorDataNode) {
        try {
            VectorDataNodeWriter vectorDataNodeWriter = new VectorDataNodeWriter();
            vectorDataNodeWriter.write(vectorDataNode, new File(vectorDataDir,
                                                                vectorDataNode.getName() + VectorDataNodeIO.FILENAME_EXTENSION));
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().throwing("DimapProductWriter", "writeVectorData", e);
        }
    }
}
