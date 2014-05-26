package org.esa.beam;

import org.esa.beam.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Marco Peters
 */
public class MultiNetcdfProductReaderPlugin implements ProductReaderPlugIn {

    private static final String FILE_EXTENSION = ".nc";
    private static final int NUM_NC_FILES = 35;
    public static final String FORMAT_NAME = "MULTI-NETCDF";
    public static final String DESCRIPTION = "MULT-NETCDF for Benchmarking";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        File inputFile = new File(input.toString());
        if (!inputFile.getName().endsWith("_meta" + FILE_EXTENSION)) {
            return DecodeQualification.UNABLE;
        }
        File parentFile = inputFile.getParentFile();
        File[] files = parentFile.listFiles();
        if (files == null) {
            return DecodeQualification.UNABLE;
        }
        if (files.length != NUM_NC_FILES) {
            return DecodeQualification.UNABLE;
        }
        for (File file : files) {
            if (!file.getName().startsWith("MER_")) {
                return DecodeQualification.UNABLE;
            }
        }
        try {
            NetcdfFile file = NetcdfFileOpener.open(inputFile);
            if (file != null) {
                file.close();
                return DecodeQualification.INTENDED;
            }
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new MultiNetcdfProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{FILE_EXTENSION};
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, FILE_EXTENSION, DESCRIPTION) {
            @Override
            public boolean accept(File file) {
                return super.accept(file) && file.getName().startsWith("MER_") && file.getName().endsWith("_meta" + FILE_EXTENSION);
            }
        };
    }
}
