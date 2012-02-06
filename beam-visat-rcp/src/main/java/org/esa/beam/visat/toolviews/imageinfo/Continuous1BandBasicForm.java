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

package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.ImageInfoEditorModel;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * This class is added to implement Basic Color Manipulation. Its design pattern is similar to Continuous1BandGraphicalForm, which implements the Sliders.
 */
class Continuous1BandBasicForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final BasicColorEditor basicColorEditor;
    private final JPanel contentPanel;
    private final MoreOptionsForm moreOptionsForm;
    private ChangeListener applyEnablerCL;

    Continuous1BandBasicForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;

        basicColorEditor = new BasicColorEditor(parentForm);
        contentPanel = new JPanel(new BorderLayout(2, 2));
        contentPanel.add(basicColorEditor, BorderLayout.CENTER);
        moreOptionsForm = new MoreOptionsForm(parentForm, true);
        applyEnablerCL = parentForm.createApplyEnablerChangeListener();
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public BasicColorEditor getBasicColorEditor() {
        return basicColorEditor;
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        updateFormModel(productSceneView);
    }

    @Override
    public void handleFormHidden(ProductSceneView productSceneView) {
        //if (basicColorEditor.getModel() != null) {
        //    basicColorEditor.getModel().removeChangeListener(applyEnablerCL);
        //    basicColorEditor.setModel(null);
        //}
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {

        productSceneView.getRaster().setLog10ScaledDisplay(basicColorEditor.isLog10Scaled())  ;
        //productSceneView.getRaster().getSceneRasterData();
        basicColorEditor.recomputeSamplePoints();
        //parentForm.applyColorPaletteDef(productSceneView.getImageInfo().getColorPaletteDef(), productSceneView.getRaster(), productSceneView.getImageInfo());
//        if (basicColorEditor.isLog10Scaled()) {
//            final double newMin = parentForm.getImageInfo().getColorPaletteDef().getMinDisplaySample();
//            final double newMax = parentForm.getImageInfo().getColorPaletteDef().getMaxDisplaySample();
//            try {
//                productSceneView.getRaster().quantizeRasterData(newMin, newMax,1, ProgressMonitor.NULL);
//            } catch (IOException ioe) {
//
//            }
//        }


        //basicColorEditor.recomputeSamplePoints();
        ImageInfoEditorModel1B model = new ImageInfoEditorModel1B(parentForm.getImageInfo());
        model.addChangeListener(applyEnablerCL);

        //ImageInfoEditorModel oldModel = basicColorEditor.getModel();
       // setDisplayProperties(model, productSceneView.getRaster());
        //basicColorEditor.setModel(model);
        //if (oldModel != null) {
         //   model.setHistogramViewGain(oldModel.getHistogramViewGain());
         //   model.setMinHistogramViewSample(oldModel.getMinHistogramViewSample());
          //  model.setMaxHistogramViewSample(oldModel.getMaxHistogramViewSample());
       // }
        //if (model.getSliderSample(0) < model.getMinHistogramViewSample() ||
         //       model.getSliderSample(model.getSliderCount() - 1) > model.getMaxHistogramViewSample()) {
            //basicColorEditor.computeZoomInToSliderLimits();
       // }

        //parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void resetFormModel(ProductSceneView productSceneView) {
         basicColorEditor.resetToDataDefault();
         updateFormModel(productSceneView);
        //basicColorEditor.computeZoomOutToFullHistogramm();
       // parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
       // if (basicColorEditor.getModel() != null) {
        //    setDisplayProperties(basicColorEditor.getModel(), raster);
        //    if (event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_STX)) {
        //        updateFormModel(parentForm.getProductSceneView());
        //    }
       // }
    }

    @Override
    public AbstractButton[] getToolButtons() {
        return new AbstractButton[0];
    }

    @Override
    public RasterDataNode[] getRasters() {
        return parentForm.getProductSceneView().getRasters();
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return moreOptionsForm;
    }


    static void setDisplayProperties(ImageInfoEditorModel model, RasterDataNode raster) {
        model.setDisplayProperties(raster.getName(), raster.getUnit(), raster.getStx(), raster);
    }

}
