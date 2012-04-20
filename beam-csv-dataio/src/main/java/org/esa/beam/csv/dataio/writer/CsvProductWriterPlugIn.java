/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.csv.dataio.writer;

import org.esa.beam.util.io.Constants;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.io.Writer;
import java.util.Locale;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductWriterPlugIn implements ProductWriterPlugIn {

    private Writer writer;
    private final int config;

    public CsvProductWriterPlugIn() {
        this(null, CsvProductWriter.WRITE_FEATURES | CsvProductWriter.WRITE_PROPERTIES);
    }

    public CsvProductWriterPlugIn(Writer writer, int config) {
        this.writer = writer;
        this.config = config;
    }

    @Override
    public Class[] getOutputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new CsvProductWriter(this, config, writer);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{Constants.FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".csv"};
    }

    @Override
    public String getDescription(Locale locale) {
        return Constants.DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}
