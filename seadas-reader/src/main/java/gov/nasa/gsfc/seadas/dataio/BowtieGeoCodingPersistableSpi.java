/*
 * Copyright (C) 2017 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.dataio.dimap.spi.DimapPersistable;
import org.esa.beam.dataio.dimap.spi.DimapPersistableSpi;
import org.jdom.Element;

public class BowtieGeoCodingPersistableSpi implements DimapPersistableSpi {

    @Override
    public boolean canDecode(Element element) {
        boolean bowtiePixel = element.getChild(BowtieGeoCodingPersistable.BOWTIE_PIXEL_GEO_CODING_TAG) != null;
        boolean bowtieTiepoint = element.getChild(BowtieGeoCodingPersistable.BOWTIE_TIEPOINT_GEO_CODING_TAG) != null;
        return bowtiePixel || bowtieTiepoint;
    }

    @Override
    public boolean canPersist(Object object) {
        boolean bowtiePixel = object instanceof BowtiePixelGeoCoding;
        boolean bowtieTiepoint = object instanceof BowtieTiePointGeoCoding;
        return bowtiePixel || bowtieTiepoint;
    }

    @Override
    public DimapPersistable createPersistable() {
        return new BowtieGeoCodingPersistable();
    }
}
