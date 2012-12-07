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

package org.esa.beam.binning.reader;

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class BinnedProductReaderTest {

    @Test
    public void testExtractStartTime() throws Exception {
        final URL resource = getClass().getResource("test.nc");
        final NetcdfFile netcdfFile = NetcdfFile.openInMemory(resource.toURI());
        ProductData.UTC startTime = BinnedProductReader.extractStartTime(netcdfFile);
        assertEquals(ProductData.UTC.parse("20030101:0000", "yyyyMMdd:HHmm").getAsDate().getTime(), startTime.getAsDate().getTime());
    }

    @Test
    public void testExtractEndTime() throws Exception {
        final URL resource = getClass().getResource("test.nc");
        final NetcdfFile netcdfFile = NetcdfFile.openInMemory(resource.toURI());
        ProductData.UTC startTime = BinnedProductReader.extractEndTime(netcdfFile);
        assertEquals(ProductData.UTC.parse("20030101:0000", "yyyyMMdd:HHmm").getAsDate().getTime(), startTime.getAsDate().getTime());
    }

    @Test
    public void testExtractStartTime_NoTimeInfo() throws Exception {
        final URL resource = getClass().getResource("test_without_time_info.nc");
        final NetcdfFile netcdfFile = NetcdfFile.openInMemory(resource.toURI());
        ProductData.UTC startTime = BinnedProductReader.extractStartTime(netcdfFile);
        assertNull(startTime);
    }

    @Test
    public void testExtractEndTime_NoTimeInfo() throws Exception {
        final URL resource = getClass().getResource("test_without_time_info.nc");
        final NetcdfFile netcdfFile = NetcdfFile.openInMemory(resource.toURI());
        ProductData.UTC endTime = BinnedProductReader.extractEndTime(netcdfFile);
        assertNull(endTime);
    }
}
