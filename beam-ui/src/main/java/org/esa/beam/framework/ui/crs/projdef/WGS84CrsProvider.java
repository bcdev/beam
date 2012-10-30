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

package org.esa.beam.framework.ui.crs.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.GeodeticDatum;

class WGS84CrsProvider extends AbstractCrsProvider {

    private static final String NAME = "Geographic Lat/Lon (WGS 84)";

    WGS84CrsProvider(GeodeticDatum wgs84Datum) {
        super(NAME, false, false, wgs84Datum);
    }

    @Override
    public ParameterValueGroup getParameter() {
        return ParameterGroup.EMPTY;
    }

    @Override
    public CoordinateReferenceSystem getCRS(final GeoPos referencePos, ParameterValueGroup parameter,
                                            GeodeticDatum datum) throws FactoryException {
        return DefaultGeographicCRS.WGS84;


//        final int greenwichLongitude = Math.round(referencePos.lon / 10) * 10;
//        final double minLon = greenwichLongitude - 180;
//        final double maxLon = greenwichLongitude + 180;
//
//        final DefaultPrimeMeridian primeMeridian = new DefaultPrimeMeridian("PRIMEM", greenwichLongitude);
//        final DefaultGeodeticDatum datum1 = new DefaultGeodeticDatum("WGS84", DefaultGeodeticDatum.WGS84.getEllipsoid(), primeMeridian);
//
//
//        final Map<String, Object> properties = new HashMap<String, Object>();
//        properties.put("name", "EAST");
//        final Unit<? extends Quantity> unit = Unit.valueOf("°");
//        final AxisDirection eastDir = AxisDirection.EAST;
//        final AxisDirection northDir = AxisDirection.NORTH;
//        final RangeMeaning meaning = RangeMeaning.WRAPAROUND;
//        final DefaultCoordinateSystemAxis axis0 = new DefaultCoordinateSystemAxis(properties, "EAST", eastDir, unit, minLon, maxLon, meaning);
//        final DefaultCoordinateSystemAxis axis1 = new DefaultCoordinateSystemAxis("NORTH", northDir, unit);
//        final DefaultEllipsoidalCS cs = new DefaultEllipsoidalCS("", axis0, axis1);

//        final DefaultGeographicCRS geographicCRS = new DefaultGeographicCRS("", DefaultGeodeticDatum.WGS84, cs);
//        final DefaultGeographicCRS geographicCRS = new DefaultGeographicCRS("", datum1, cs);
//        final DefaultGeographicCRS defaultGeographicCRS = DefaultGeographicCRS.WGS84;
//        System.out.println("geographicCRS:");
//        System.out.println(geographicCRS.toWKT());
//        System.out.println("defaultGeographicCRS:");
//        System.out.println(defaultGeographicCRS.toWKT());
//        return geographicCRS;
//        return defaultGeographicCRS;
    }
}
