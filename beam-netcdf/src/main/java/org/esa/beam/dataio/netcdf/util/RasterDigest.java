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

package org.esa.beam.dataio.netcdf.util;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an extract of all variables that could be converted to bands.
 */
public class RasterDigest {

    private final DimKey rasterDim;
    private final Variable[] variables;


    public RasterDigest(DimKey rasterDim, Variable[] variables) {
        this.rasterDim = rasterDim;
        this.variables = variables;
    }

    public DimKey getRasterDim() {
        return rasterDim;
    }

    public Variable[] getRasterVariables() {
        return variables;
    }

    public static RasterDigest createRasterDigest(final Group group) {
        Map<DimKey, List<Variable>> variableListMap = getVariableListMap(group);
        if (variableListMap.isEmpty()) {
            return null;
        }
        final DimKey rasterDim = getBestRasterDim(variableListMap);
        final Variable[] rasterVariables = getRasterVariables(variableListMap, rasterDim);
        return new RasterDigest(rasterDim, rasterVariables);
    }

    static Variable[] getRasterVariables(Map<DimKey, List<Variable>> variableLists,
                                         DimKey rasterDim) {
        final List<Variable> list = variableLists.get(rasterDim);
        return list.toArray(new Variable[list.size()]);
    }

    static DimKey getBestRasterDim(Map<DimKey, List<Variable>> variableListMap) {
        final Set<DimKey> ncRasterDims = variableListMap.keySet();
        if (ncRasterDims.size() == 0) {
            return null;
        }

        DimKey bestRasterDim = null;
        List<Variable> bestVarList = null;
        for (DimKey rasterDim : ncRasterDims) {
            if (rasterDim.isTypicalRasterDim()) {
                return rasterDim;
            }
            // Otherwise, we assume the best is the one which holds the most variables
            final List<Variable> varList = variableListMap.get(rasterDim);
            if (bestVarList == null || varList.size() > bestVarList.size()) {
                bestRasterDim = rasterDim;
                bestVarList = varList;
            }
        }

        return bestRasterDim;
    }

    static Map<DimKey, List<Variable>> getVariableListMap(final Group group) {
        Map<DimKey, List<Variable>> variableLists = new HashMap<DimKey, List<Variable>>();
        collectVariableLists(group, variableLists);
        return variableLists;
    }

    static void collectVariableLists(Group group, Map<DimKey, List<Variable>> variableLists) {
        final List<Variable> variables = group.getVariables();
        for (final Variable variable : variables) {
            final int rank = variable.getRank();
            if (rank >= 2 && (DataTypeUtils.isValidRasterDataType(variable.getDataType()) || variable.getDataType() == DataType.LONG)) {
                final Dimension dimX = variable.getDimension(rank - 1);
                final Dimension dimY = variable.getDimension(rank - 2);
                if (dimX.getLength() > 1 && dimY.getLength() > 1) {
                    DimKey rasterDim = new DimKey(variable.getDimensions().toArray(new Dimension[variable.getDimensions().size()]));
                    List<Variable> list = variableLists.get(rasterDim);
                    if (list == null) {
                        list = new ArrayList<Variable>();
                        variableLists.put(rasterDim, list);
                    }
                    list.add(variable);
                }
            }
        }
        final List<Group> subGroups = group.getGroups();
        for (final Group subGroup : subGroups) {
            collectVariableLists(subGroup, variableLists);
        }
    }
}
