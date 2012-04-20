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
package org.esa.beam.dataio.netcdf.metadata.profiles.beam;

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.*;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;

public class BeamStxPart extends ProfilePartIO {

    public final String STATISTICS = "statistics";
    public final String SAMPLE_FREQUENCIES = "sample_frequencies";
    public final int INDEX_SCALED_MIN = 0;
    public final int INDEX_SCALED_MAX = 1;
    public final int INDEX_MEAN = 2;
    public final int INDEX_STANDARD_DEVIATION = 3;

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        for (Band band : p.getBands()) {
            String variableName = ReaderUtils.getVariableName(band);
            final Variable variable = ctx.getNetcdfFile().getRootGroup().findVariable(variableName);

            final Attribute statistics = variable.findAttributeIgnoreCase(STATISTICS);
            final Attribute sampleFrequencies = variable.findAttributeIgnoreCase(SAMPLE_FREQUENCIES);

            if (statistics != null && sampleFrequencies != null && statistics.getLength() >= 2) {
                final double min = statistics.getNumericValue(INDEX_SCALED_MIN).doubleValue();
                final double max = statistics.getNumericValue(INDEX_SCALED_MAX).doubleValue();

                final Number meanNumber = statistics.getNumericValue(INDEX_MEAN);
                final double mean = meanNumber != null ? meanNumber.doubleValue() : Double.NaN;

                final Number stdDevNumber = statistics.getNumericValue(INDEX_STANDARD_DEVIATION);
                final double stdDev = stdDevNumber != null ? stdDevNumber.doubleValue() : Double.NaN;

                final boolean intHistogram = !ProductData.isFloatingPointType(band.getGeophysicalDataType());

                final int[] frequencies = new int[sampleFrequencies.getLength()];
                for (int i = 0; i < frequencies.length; i++) {
                    final Number fNumber = sampleFrequencies.getNumericValue(i);
                    frequencies[i] = fNumber != null ? fNumber.intValue() : 0;
                }
                final int resolutionLevel = 0;

                Stx stx = new StxFactory()
                        .withMinimum(min)
                        .withMaximum(max)
                        .withMean(mean)
                        .withStandardDeviation(stdDev)
                        .withIntHistogram(intHistogram)
                        .withHistogramBins(frequencies)
                        .withResolutionLevel(resolutionLevel)
                        .create();

                band.setStx(stx);
            }
        }
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        for (Band band : p.getBands()) {
            if (band.isStxSet()) {
                String variableName = ReaderUtils.getVariableName(band);
                final Stx stx = band.getStx();
                final NVariable variable = ctx.getNetcdfFileWriteable().findVariable(variableName);
                final double[] statistics = new double[4];
                statistics[INDEX_SCALED_MIN] = stx.getMinimum();
                statistics[INDEX_SCALED_MAX] = stx.getMaximum();
                statistics[INDEX_MEAN] = stx.getMean();
                statistics[INDEX_STANDARD_DEVIATION] = stx.getStandardDeviation();
                variable.addAttribute(STATISTICS, Array.factory(statistics));
                variable.addAttribute(SAMPLE_FREQUENCIES, Array.factory(stx.getHistogramBins()));
            }
        }
    }
}
