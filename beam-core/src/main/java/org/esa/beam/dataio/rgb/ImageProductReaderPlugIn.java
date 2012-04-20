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
package org.esa.beam.dataio.rgb;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.util.Locale;

/**
 * @author Norman Fomferra
 */
public class ImageProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "IMAGE";

    @Override
    public String[] getFormatNames() {
        return new String[]{"PNG", "GIF", "JPG", "BMP"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".png", ".gif", ".jpg", ".bmp"};
    }

    @Override
    public String getDescription(Locale name) {
        return "Image product reader";
    }

    @Override
    public DecodeQualification getDecodeQualification(Object object) {
        File file = getFile(object);
        String fileExt = FileUtils.getExtension(file);
        if (fileExt != null && StringUtils.contains(getDefaultFileExtensions(), fileExt.toLowerCase())) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }

    static File getFile(Object object) {
        File file = null;
        if (object instanceof String) {
            file = new File((String) object);
        } else if (object instanceof File) {
            file = (File) object;
        }
        return file;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new ImageProductReader(this);
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), "Image files");
    }
}
