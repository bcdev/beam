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

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.ImageInfoEditorModel;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class is added to implement Basic Color Manipulation. Its design pattern is similar to Continuous1BandGraphicalForm, which implements the Sliders.
 */
class Continuous1BandBasicForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final BasicColorEditor basicColorEditor;
    private final ImageInfoEditor2 imageInfoEditor;
    private final JPanel contentPanel;
    private final MoreOptionsForm moreOptionsForm;
    private ChangeListener applyEnablerCL;

    private final AbstractButton logDisplayButton;
    private final AbstractButton evenDistButton;

    Continuous1BandBasicForm(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        imageInfoEditor = new ImageInfoEditor2(parentForm);
        basicColorEditor = new BasicColorEditor(parentForm, imageInfoEditor);
        contentPanel = new JPanel(new BorderLayout(2, 2));
        contentPanel.add(basicColorEditor, BorderLayout.CENTER);
        moreOptionsForm = new MoreOptionsForm(parentForm, true);

        logDisplayButton = ImageInfoEditorSupport.createToggleButton("icons/LogDisplay24.png");
        logDisplayButton.setName("logDisplayButton");
        logDisplayButton.setToolTipText("Switch to logarithmic display"); /*I18N*/
        logDisplayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setLogarithmicDisplay(parentForm.getProductSceneView().getRaster(), logDisplayButton.isSelected());
            }
        });

        evenDistButton = ImageInfoEditorSupport.createButton("icons/EvenDistribution24.gif");
        evenDistButton.setName("evenDistButton");
        evenDistButton.setToolTipText("Distribute sliders evenly between first and last slider"); /*I18N*/
        evenDistButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                distributeSlidersEvenly();
            }
        });

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
        if (imageInfoEditor.getModel() != null) {
            imageInfoEditor.getModel().removeChangeListener(applyEnablerCL);
            imageInfoEditor.setModel(null);
        }
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {


        final ImageInfoEditorModel newModel = new ImageInfoEditorModel1B(parentForm.getImageInfo());
        newModel.addChangeListener(applyEnablerCL);
        imageInfoEditor.setModel(newModel);

        final RasterDataNode raster = productSceneView.getRaster();
        setLogarithmicDisplay(raster, newModel.getImageInfo().isLogScaled());


        logDisplayButton.setSelected(newModel.getImageInfo().isLogScaled());

        basicColorEditor.resetMinMax();

        parentForm.revalidateToolViewPaneControl();
    }

    public AbstractButton[] getToolButtons() {
        return new AbstractButton[]{
                logDisplayButton,
                evenDistButton,

        };
    }

    @Override
    public void resetFormModel(ProductSceneView productSceneView) {
        basicColorEditor.resetToDataDefault();
        updateFormModel(productSceneView);
        imageInfoEditor.computeZoomOutToFullHistogramm();
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
        if (imageInfoEditor.getModel() != null) {
             setDisplayProperties(imageInfoEditor.getModel(), raster);
            if (event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_STX)) {
                updateFormModel(parentForm.getProductSceneView());
            }
        }

    }

//    @Override
//    public AbstractButton[] getToolButtons() {
//        return new AbstractButton[0];
//    }

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

    private void setLogarithmicDisplay(final RasterDataNode raster, final boolean logarithmicDisplay) {
        final ImageInfoEditorModel model = imageInfoEditor.getModel();
        if (logarithmicDisplay) {
            final StxFactory stxFactory = new StxFactory();
            final Stx stx = stxFactory
                    .withHistogramBinCount(raster.getStx().getHistogramBinCount())
                    .withLogHistogram(logarithmicDisplay)
                    .withResolutionLevel(raster.getSourceImage().getModel().getLevelCount() - 1)
                    .create(raster, com.bc.ceres.core.ProgressMonitor.NULL);
            model.setDisplayProperties(raster.getName(), raster.getUnit(), stx, Continuous1BandGraphicalForm.POW10_SCALING);
        } else {
            model.setDisplayProperties(raster.getName(), raster.getUnit(), raster.getStx(), Scaling.IDENTITY);
        }
        model.getImageInfo().setLogScaled(logarithmicDisplay);
    }

    private void distributeSlidersEvenly() {
        imageInfoEditor.distributeSlidersEvenly();
    }


}
