package org.esa.beam.visat.actions;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.dataio.geometry.VectorDataNodeReader2;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.GeometryDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptorRegistry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.crs.CrsSelectionPanel;
import org.esa.beam.framework.ui.crs.CustomCrsForm;
import org.esa.beam.framework.ui.crs.PredefinedCrsForm;
import org.esa.beam.framework.ui.crs.ProductCrsForm;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author olafd
 * @author Thomas Storm
 */
abstract class AbstractImportVectorDataNodeAction extends ExecCommand {
    protected FeatureUtils.FeatureCrsProvider crsProvider;
    protected VectorDataNodeReader2.PlacemarkDescriptorProvider placemarkDescriptorProvider;

    protected AbstractImportVectorDataNodeAction() {
        crsProvider = new MyFeatureCrsProvider(getHelpId());
        placemarkDescriptorProvider = new MyPlacemarkDescriptorProvider();
    }

    private static class MyPlacemarkDescriptorProvider implements VectorDataNodeReader2.PlacemarkDescriptorProvider {
        @Override
        public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType) {
            final PlacemarkDescriptor placemarkDescriptor = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(simpleFeatureType);
            if (placemarkDescriptor != null && placemarkDescriptor.getQualification(simpleFeatureType) == DecodeQualification.INTENDED) {
                return placemarkDescriptor;
            }

            TypeDialog typeDialog = new TypeDialog(VisatApp.getApp().getApplicationWindow(), simpleFeatureType);
            if (typeDialog.show() != ModalDialog.ID_OK) {
                return PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(GeometryDescriptor.class);
            }

            return typeDialog.getPlacemarkDescriptor();
        }
    }

    private class MyFeatureCrsProvider implements FeatureUtils.FeatureCrsProvider {

        private final String helpId;

        public MyFeatureCrsProvider(String helpId) {
            this.helpId = helpId;
        }

        @Override
        public CoordinateReferenceSystem getFeatureCrs(final Product product) {
            final CoordinateReferenceSystem[] featureCrsBuffer = new CoordinateReferenceSystem[1];
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    featureCrsBuffer[0] = promptForFeatureCrs(VisatApp.getApp(), product);
                }
            };
            if (!SwingUtilities.isEventDispatchThread()) {
                try {
                    SwingUtilities.invokeAndWait(runnable);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            } else {
                runnable.run();
            }
            CoordinateReferenceSystem featureCrs = featureCrsBuffer[0];
            return featureCrs != null ? featureCrs : DefaultGeographicCRS.WGS84;
        }

        private CoordinateReferenceSystem promptForFeatureCrs(VisatApp visatApp, Product product) {
            final ProductCrsForm productCrsForm = new ProductCrsForm(visatApp, product);
            final CustomCrsForm customCrsForm = new CustomCrsForm(visatApp);
            final PredefinedCrsForm predefinedCrsForm = new PredefinedCrsForm(visatApp);

            final CrsSelectionPanel crsSelectionPanel = new CrsSelectionPanel(productCrsForm,
                                                                              customCrsForm,
                                                                              predefinedCrsForm);
            final ModalDialog dialog = new ModalDialog(visatApp.getApplicationWindow(), getDialogTitle(),
                                                       ModalDialog.ID_OK_CANCEL_HELP, helpId);

            final TableLayout tableLayout = new TableLayout(1);
            tableLayout.setTableWeightX(1.0);
            tableLayout.setTableFill(TableLayout.Fill.BOTH);
            tableLayout.setTablePadding(4, 4);
            tableLayout.setCellPadding(0, 0, new Insets(4, 10, 4, 4));
            final JPanel contentPanel = new JPanel(tableLayout);
            final JLabel label = new JLabel();
            label.setText("<html><b>" +
                                  "These vector data does not define a coordinate reference system (CRS).<br/>" +
                                  "Please specify a CRS so that coordinates can interpreted correctly.</b>");

            contentPanel.add(label);
            contentPanel.add(crsSelectionPanel);
            dialog.setContent(contentPanel);
            if (dialog.show() == ModalDialog.ID_OK) {
                try {
                    return crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(product));
                } catch (FactoryException e) {
                    visatApp.showErrorDialog(getDialogTitle(),
                                             "Can not create Coordinate Reference System.\n" + e.getMessage());
                }
            }
            return DefaultGeographicCRS.WGS84;
        }
    }

        protected abstract String getDialogTitle();

    }
