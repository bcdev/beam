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

package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.TableLayout;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.crs.CrsSelectionPanel;
import org.esa.beam.framework.ui.crs.CustomCrsForm;
import org.esa.beam.framework.ui.crs.PredefinedCrsForm;
import org.esa.beam.framework.ui.crs.ProductCrsForm;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayerFilterFactory;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Debug;
import org.esa.beam.util.FeatureCollectionClipper;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.ShapefileUtils;
import org.geotools.data.FeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StreamTokenizer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class ImportGeometryAction extends ExecCommand {

    private static final String DLG_TITLE = "Import Geometry";
    private static final String PROPERTY_SHAPE_IO_DIR = "shape.io.dir";

    @Override
    public void actionPerformed(final CommandEvent event) {
        importGeometry(VisatApp.getApp());
        VisatApp.getApp().updateState();
    }

    @Override
    public void updateState(final CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null);
    }

    private void importGeometry(final VisatApp visatApp) {
        final PropertyMap propertyMap = visatApp.getPreferences();
        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setDialogTitle(DLG_TITLE);
        final BeamFileFilter plainTextFilter = new BeamFileFilter("CSV",
                                                                  new String[]{".txt", ".dat", ".csv"},
                                                                  "Plain text");
        final BeamFileFilter shapefileFilter = new BeamFileFilter("SHAPEFILE",
                                                                  new String[]{".shp"},
                                                                  "ESRI shapefiles");
        fileChooser.addChoosableFileFilter(plainTextFilter);
        fileChooser.addChoosableFileFilter(shapefileFilter);
        fileChooser.setFileFilter(shapefileFilter);/*I18N*/
        fileChooser.setCurrentDirectory(getIODir(propertyMap));
        final int result = fileChooser.showOpenDialog(visatApp.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(propertyMap, file.getAbsoluteFile().getParentFile());
                loadGeometry(visatApp, file);
            }
        }
    }

    private void loadGeometry(final VisatApp visatApp, final File file) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        if (product == null) {
            return;
        }

        final GeoCoding geoCoding = product.getGeoCoding();
        if (isShapefile(file) && (geoCoding == null || !geoCoding.canGetPixelPos())) {
            visatApp.showErrorDialog(DLG_TITLE, "Failed to import geometry.\n"
                                                + "Current geo-coding cannot convert from geographic to pixel coordinates."); /* I18N */
            return;
        }

        VectorDataNode vectorDataNode;
        try {
            vectorDataNode = readGeometry(visatApp, file, product);
        } catch (Exception e) {
            visatApp.showErrorDialog(DLG_TITLE, "Failed to import geometry.\n" + "An I/O Error occurred:\n"
                                                + e.getMessage()); /* I18N */
            Debug.trace(e);
            return;
        }

        if (vectorDataNode.getFeatureCollection().isEmpty()) {
            visatApp.showErrorDialog(DLG_TITLE, "The geometry was loaded successfully,\n"
                                                + "but no part is located within the scene boundaries."); /* I18N */
            return;
        }
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
        vectorDataGroup.add(vectorDataNode);
        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null) {
            final LayerFilter nodeFilter = VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode);
            Layer vectorDataLayer = LayerUtils.getChildLayer(sceneView.getRootLayer(),
                                                             LayerUtils.SEARCH_DEEP,
                                                             nodeFilter);
            if (vectorDataLayer != null) {
                vectorDataLayer.setVisible(true);
            }
        }
    }

    private static String findUniqueVectorDataNodeName(String suggestedName,
                                                       ProductNodeGroup<VectorDataNode> vectorDataGroup) {
        String name = suggestedName;
        int index = 1;
        while (vectorDataGroup.contains(name)) {
            name = suggestedName + "_" + index;
            index++;
        }
        return name;
    }

    private boolean isShapefile(File file) {
        return file.getName().toLowerCase().endsWith(".shp");
    }

    private VectorDataNode readGeometry(VisatApp visatApp, File file, Product product) throws IOException {
        if (isShapefile(file)) {
            return readGeometryFromShapefile(visatApp, file, product);
        } else {
            return readGeometryFromTextFile(file, product);
        }
    }

    private VectorDataNode readGeometryFromShapefile(VisatApp visatApp, File file, Product product) throws IOException {
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = loadShapefile(visatApp, file, product);
        Style[] styles = ShapefileUtils.loadSLD(file);
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
        String name = findUniqueVectorDataNodeName(featureCollection.getSchema().getName().getLocalPart(),
                                                   vectorDataGroup);
        if (styles.length > 0) {
            SimpleFeatureType featureType = ShapefileUtils.createStyledFeatureType(featureCollection.getSchema());
            VectorDataNode vectorDataNode = new VectorDataNode(name, featureType);
            FeatureCollection<SimpleFeatureType, SimpleFeature> styledCollection = vectorDataNode.getFeatureCollection();
            String defaultCSS = vectorDataNode.getDefaultStyleCss();
            ShapefileUtils.applyStyle(styles[0], defaultCSS, featureCollection, styledCollection);
            return vectorDataNode;
        } else {
            return new VectorDataNode(name, featureCollection);
        }
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> loadShapefile(VisatApp visatApp, File file,
                                                                              Product product) throws IOException {
        final URL url = file.toURI().toURL();
        final CoordinateReferenceSystem targetCrs = ImageManager.getModelCrs(product.getGeoCoding());
        final Geometry clipGeometry = FeatureCollectionClipper.createGeoBoundaryPolygon(product);
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = ShapefileUtils.getFeatureSource(url);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureSource.getFeatures();

        CoordinateReferenceSystem featureCrs = featureCollection.getSchema().getCoordinateReferenceSystem();
        if (featureCrs == null) {
            featureCrs = promptForFeatureCrs(visatApp, product);
        }

        if (featureCrs == null) {
            featureCrs = DefaultGeographicCRS.WGS84;
        }
        return FeatureCollectionClipper.doOperation(featureCollection, featureCrs,
                                                    clipGeometry, DefaultGeographicCRS.WGS84,
                                                    null, targetCrs);
    }

    private CoordinateReferenceSystem promptForFeatureCrs(VisatApp visatApp, Product product) {
        final ProductCrsForm productCrsForm = new ProductCrsForm(visatApp, product);
        final CustomCrsForm customCrsForm = new CustomCrsForm(visatApp);
        final PredefinedCrsForm predefinedCrsForm = new PredefinedCrsForm(visatApp);

        final CrsSelectionPanel crsSelectionPanel = new CrsSelectionPanel(productCrsForm,
                                                                          customCrsForm,
                                                                          predefinedCrsForm);
        final ModalDialog dialog = new ModalDialog(visatApp.getApplicationWindow(), DLG_TITLE,
                                                   ModalDialog.ID_OK_CANCEL_HELP, getHelpId());

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4,4);
        tableLayout.setCellPadding(0, 0, new Insets(4, 10, 4, 4));
        final JPanel contentPanel = new JPanel(tableLayout);
        final JLabel label = new JLabel();
        label.setText("<html><b>" +
                      "The ESRI Shapefile you want to import does not define a CRS.<br/>" +
                      "Please specify the CRS in which the coordinates are defined.</b>");
        
        contentPanel.add(label);
        contentPanel.add(crsSelectionPanel);
        dialog.setContent(contentPanel);
        if (dialog.show() == ModalDialog.ID_OK) {
            try {
                return crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(product));
            } catch (FactoryException e) {
                visatApp.showErrorDialog(DLG_TITLE, "Can not create Coordinate Reference System.\n" + e.getMessage());
            }
        }
        return null;
    }

    private static VectorDataNode readGeometryFromTextFile(final File file, Product product) throws IOException {
        final ArrayList<PixelPos> pixelPositions = new ArrayList<PixelPos>(256);
        final GeoCoding geoCoding = product.getGeoCoding();

        final FileReader fileReader = new FileReader(file);
        final LineNumberReader reader = new LineNumberReader(fileReader);
        try {

            final StreamTokenizer st = createConfiguredTokenizer(reader);

            final float[] values = new float[]{0.0F, 0.0F, 0.0F, 0.0F}; // values for {x, y, lat, lon}
            final boolean[] valid = new boolean[]{false, false, false, false}; // {x, y, lat, lon} columns valid?
            final int[] indices = new int[]{0, 1, 2, 3}; // indexes of {x, y, lat, lon} columns

            boolean headerAvailable = false;
            int column = 0;

            while (true) {
                final int tt = st.nextToken();

                if (tt == StreamTokenizer.TT_EOF
                    || tt == StreamTokenizer.TT_EOL) {
                    final boolean xyAvailable = valid[0] && valid[1];
                    final boolean latLonAvailable = valid[2] && valid[3] && geoCoding != null && geoCoding.canGetPixelPos();
                    if (xyAvailable || latLonAvailable) {
                        PixelPos pixelPos;
                        if (latLonAvailable) {
                            pixelPos = geoCoding.getPixelPos(new GeoPos(values[2], values[3]), null);
                        } else {
                            pixelPos = new PixelPos(values[0], values[1]);
                        }

                        // Do not add positions which are out of bounds, it leads to scrambled shapes
                        if (pixelPos.x != -1 && pixelPos.y != -1) {
                            pixelPositions.add(pixelPos);
                        }
                    }

                    Arrays.fill(values, 0.0f);
                    Arrays.fill(valid, false);

                    if (tt == StreamTokenizer.TT_EOF) {
                        break;
                    } else if (tt == StreamTokenizer.TT_EOL) {
                        column = 0;
                    }
                } else if (tt == StreamTokenizer.TT_WORD) {
                    final String token = st.sval;
                    int headerText = -1;
                    if ("x".equalsIgnoreCase(token)
                        || "pixel-x".equalsIgnoreCase(token)
                        || "pixel_x".equalsIgnoreCase(token)) {
                        indices[0] = column;
                        headerText = 0;
                    } else if ("y".equalsIgnoreCase(token)
                               || "pixel-y".equalsIgnoreCase(token)
                               || "pixel_y".equalsIgnoreCase(token)) {
                        indices[1] = column;
                        headerText = 1;
                    } else if ("lat".equalsIgnoreCase(token)
                               || "latitude".equalsIgnoreCase(token)) {
                        indices[2] = column;
                        headerText = 2;
                    } else if ("lon".equalsIgnoreCase(token)
                               || "long".equalsIgnoreCase(token)
                               || "longitude".equalsIgnoreCase(token)) {
                        indices[3] = column;
                        headerText = 3;
                    } else {
                        for (int i = 0; i < 4; i++) {
                            if (column == indices[i]) {
                                try {
                                    values[i] = Float.parseFloat(token);
                                    valid[i] = true;
                                    break;
                                } catch (NumberFormatException ignore) {
                                }
                            }
                        }
                    }
                    if (!headerAvailable && headerText >= 0) {
                        for (int i = 0; i < indices.length; i++) {
                            if (headerText != i) {
                                indices[i] = -1;
                            }
                        }
                        headerAvailable = true;
                    }
                    column++;
                } else {
                    Debug.assertTrue(false);
                }
            }
        } finally {
            reader.close();
            fileReader.close();
        }

        Geometry geometry = null;
        if (!pixelPositions.isEmpty()) {
            geometry = createGeometry(pixelPositions);
        }
        if (geometry == null) {
            return null;
        }
        CoordinateReferenceSystem modelCrs = ImageManager.getModelCrs(geoCoding);
        AffineTransform imageToModelTransform = ImageManager.getImageToModelTransform(geoCoding);
        GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
        transformer.setMathTransform(new AffineTransform2D(imageToModelTransform));
        transformer.setCoordinateReferenceSystem(modelCrs);
        try {
            geometry = transformer.transform(geometry);
        } catch (TransformException ignored) {
            return null;
        }

        String name = FileUtils.getFilenameWithoutExtension(file);
        findUniqueVectorDataNodeName(name, product.getVectorDataGroup());
        SimpleFeatureType simpleFeatureType = PlainFeatureFactory.createDefaultFeatureType(modelCrs);
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(name, simpleFeatureType);

        VectorDataNode vectorDataNode = new VectorDataNode(name, featureCollection);
        String style = vectorDataNode.getDefaultStyleCss();
        SimpleFeature simpleFeature = PlainFeatureFactory.createPlainFeature(simpleFeatureType, name, geometry, style);
        featureCollection.add(simpleFeature);

        return vectorDataNode;
    }

    private static Geometry createGeometry(ArrayList<PixelPos> pixelPositions) {
        GeometryFactory geometryFactory = new GeometryFactory();
        ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
        PixelPos pixelPos0 = pixelPositions.get(0);
        coordinates.add(new Coordinate(pixelPos0.x, pixelPos0.y));
        PixelPos pixelPos1 = null;
        for (int i = 1; i < pixelPositions.size(); i++) {
            pixelPos1 = pixelPositions.get(i);
            coordinates.add(new Coordinate(pixelPos1.x, pixelPos1.y));
        }
        if (pixelPos1 != null && pixelPos1.distanceSq(pixelPos0) < 1.0e-5) {
            coordinates.add(coordinates.get(0));
        }
        if (coordinates.get(0).equals(coordinates.get(coordinates.size() - 1))) {
            return geometryFactory.createLinearRing(coordinates.toArray(new Coordinate[coordinates.size()]));
        } else {
            return geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        }
    }

    private static StreamTokenizer createConfiguredTokenizer(LineNumberReader reader) {
        final StreamTokenizer st = new StreamTokenizer(reader);
        st.resetSyntax();
        st.eolIsSignificant(true);
        st.lowerCaseMode(true);
        st.commentChar('#');
        st.whitespaceChars(' ', ' ');
        st.whitespaceChars('\t', '\t');
        st.wordChars(33, 255);
        return st;
    }

    private static void setIODir(final PropertyMap propertyMap, final File dir) {
        if (dir != null) {
            propertyMap.setPropertyString(PROPERTY_SHAPE_IO_DIR, dir.getPath());
        }
    }

    private static File getIODir(final PropertyMap propertyMap) {
        final File dir = SystemUtils.getUserHomeDir();
        return new File(propertyMap.getPropertyString(PROPERTY_SHAPE_IO_DIR, dir.getPath()));
    }

}
