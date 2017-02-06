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

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.spi.DimapPersistable;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.jdom.Element;

public class BowtieGeoCodingPersistable implements DimapPersistable {

    static final String BOWTIE_PIXEL_GEO_CODING_TAG = "BowtiePixelGeoCoding";
    static final String BOWTIE_TIEPOINT_GEO_CODING_TAG = "BowtieTiePointGeoCoding";
    static final String BOWTIE_SCANLINE_HEIGHT_TAG = "BowtieScanlineHeight";

    @Override
    public Object createObjectFromXml(Element element, Product product) {
        Element pixelElement = element.getChild(BOWTIE_PIXEL_GEO_CODING_TAG);
        Element tiepointElement = element.getChild(BOWTIE_TIEPOINT_GEO_CODING_TAG);
        if (pixelElement != null) {
            String nameLat = pixelElement.getChildTextTrim(DimapProductConstants.TAG_LATITUDE_BAND);
            String nameLon = pixelElement.getChildTextTrim(DimapProductConstants.TAG_LONGITUDE_BAND);
            int scanlineHeight = Integer.parseInt(pixelElement.getChildTextTrim(BOWTIE_SCANLINE_HEIGHT_TAG));

            if (nameLat != null && nameLon != null) {
                Band latBand = product.getBand(nameLat);
                Band lonBand = product.getBand(nameLon);
                if (latBand != null && lonBand != null) {
                    return new BowtiePixelGeoCoding(latBand, lonBand, scanlineHeight);
                }
            }
        } else if (tiepointElement != null) {
            String nameLat = tiepointElement.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LAT);
            String nameLon = tiepointElement.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LON);
            int scanlineHeight = Integer.parseInt(tiepointElement.getChildTextTrim(BOWTIE_SCANLINE_HEIGHT_TAG));

            if (nameLat != null && nameLon != null) {
                TiePointGrid latGrid = product.getTiePointGrid(nameLat);
                TiePointGrid lonGrid = product.getTiePointGrid(nameLon);
                if (latGrid != null && lonGrid != null) {
                    if (latGrid.hasRasterData() && lonGrid.hasRasterData()) {
                        return new BowtieTiePointGeoCoding(latGrid, lonGrid, scanlineHeight);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported element " + element);
        }
        return null;
    }

    @Override
    public Element createXmlFromObject(Object object) {
        if (object instanceof BowtiePixelGeoCoding) {
            BowtiePixelGeoCoding geoCoding = (BowtiePixelGeoCoding) object;

            final Element element = new Element(BOWTIE_PIXEL_GEO_CODING_TAG);

            final Element latElement = new Element(DimapProductConstants.TAG_LATITUDE_BAND);
            latElement.setText(geoCoding.getLatBand().getName());
            element.addContent(latElement);

            final Element lonElement = new Element(DimapProductConstants.TAG_LONGITUDE_BAND);
            lonElement.setText(geoCoding.getLonBand().getName());
            element.addContent(lonElement);

            final Element scanlineHeightElement = new Element(BOWTIE_SCANLINE_HEIGHT_TAG);
            scanlineHeightElement.setText(String.valueOf(geoCoding.getScanlineHeight()));
            element.addContent(scanlineHeightElement);

            return element;
        } else if (object instanceof BowtieTiePointGeoCoding) {
            BowtieTiePointGeoCoding geoCoding = (BowtieTiePointGeoCoding) object;

            final Element element = new Element(BOWTIE_TIEPOINT_GEO_CODING_TAG);

            final Element latElement = new Element(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LAT);
            latElement.setText(geoCoding.getLatGrid().getName());
            element.addContent(latElement);

            final Element lonElement = new Element(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LON);
            lonElement.setText(geoCoding.getLonGrid().getName());
            element.addContent(lonElement);

            final Element scanlineHeightElement = new Element(BOWTIE_SCANLINE_HEIGHT_TAG);
            scanlineHeightElement.setText(String.valueOf(geoCoding.getScanlineHeight()));
            element.addContent(scanlineHeightElement);

            return element;
        } else {
            throw new IllegalArgumentException("Unsupported object " + object);
        }
    }
}
