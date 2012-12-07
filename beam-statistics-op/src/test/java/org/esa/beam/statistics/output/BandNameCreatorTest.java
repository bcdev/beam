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

package org.esa.beam.statistics.output;

import org.esa.beam.util.logging.BeamLogManager;
import org.junit.Test;

import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class BandNameCreatorTest {

    @Test
    public void testCreateAttributeName() throws Exception {
        final int[] warningCount = new int[1];
        final Handler handler = new Handler() {

            @Override
            public void publish(LogRecord record) {
                assertEquals(Level.WARNING, record.getLevel());
                assertTrue(record.getMessage().contains("exceeds 10 characters in length. Shortened to"));
                warningCount[0]++;
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        BeamLogManager.getSystemLogger().addHandler(handler);

        BandNameCreator bandNameCreator = new BandNameCreator();
        String attributeName1 = bandNameCreator.createUniqueAttributeName("median", "radiance_12");
        String attributeName2 = bandNameCreator.createUniqueAttributeName("median", "radiance_13");
        String attributeName3 = bandNameCreator.createUniqueAttributeName("p90", "radiance_12");
        String attributeName4 = bandNameCreator.createUniqueAttributeName("p90", "radiance_13");
        String attributeName5 = bandNameCreator.createUniqueAttributeName("p90", "algal2");
        String attributeName6 = bandNameCreator.createUniqueAttributeName("p90", "algal1");
        String attributeName7 = bandNameCreator.createUniqueAttributeName("maximum", "algal1");
        String attributeName8 = bandNameCreator.createUniqueAttributeName("minimum", "algal1");
        String attributeName9 = bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index");
        String attributeName10 = bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_normalized");
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_a");
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_b");
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_c");
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_d");
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_e");
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_f");
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_g");
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_h");
        String attributeName11 = bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_xyz");

        assertEquals("mdn_rdnc12", attributeName1);
        assertEquals("mdn_rdnc13", attributeName2);
        assertEquals("p90_rdnc12", attributeName3);
        assertEquals("p90_rdnc13", attributeName4);
        assertEquals("p90_algal2", attributeName5);
        assertEquals("p90_algal1", attributeName6);
        assertEquals("mx_lgl1", attributeName7);
        assertEquals("mn_lgl1", attributeName8);
        assertEquals("mdn_0", attributeName9);
        assertEquals("mdn_1", attributeName10);
        assertEquals("mdn_10", attributeName11);

        assertEquals(17, warningCount[0]);

        BeamLogManager.getSystemLogger().removeHandler(handler);
    }

    @Test
    public void testMapping() throws Exception {
        final StringBuilder stringBuilder = new StringBuilder();
        final PrintStream stream = new PrintStream(new CsvStatisticsWriterTest.StringOutputStream(stringBuilder));
        final BandNameCreator bandNameCreator = new BandNameCreator(stream);
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_a");
        bandNameCreator.createUniqueAttributeName("median", "saharan_dust_index_b");
        bandNameCreator.createUniqueAttributeName("maximum", "far_too_long_band_name");
        bandNameCreator.createUniqueAttributeName("minimum", "far_too_long_band_name");

        assertEquals("mdn_0=median_saharan_dust_index_a\n" +
                     "mdn_1=median_saharan_dust_index_b\n" +
                     "mx_0=maximum_far_too_long_band_name\n" +
                     "mn_0=minimum_far_too_long_band_name\n",
                     stringBuilder.toString());
    }
}
