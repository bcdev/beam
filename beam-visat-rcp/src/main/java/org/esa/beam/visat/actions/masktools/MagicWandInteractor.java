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

package org.esa.beam.visat.actions.masktools;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.swing.figure.ViewportInteractor;
import com.bc.ceres.swing.undo.UndoContext;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.MaskLayerType;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.IOException;

import static org.esa.beam.visat.actions.masktools.MagicWandModel.MAGIC_WAND_MASK_NAME;
import static org.esa.beam.visat.actions.masktools.MagicWandModel.getSpectralBands;

/**
 * An interactor that lets users create masks using a "magic wand".
 * The mask comprises all pixels in the image that are "spectrally" close to the pixel that
 * has been selected using the magic wand.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class MagicWandInteractor extends ViewportInteractor {

    private static final String DIALOG_TITLE = "Magic Wand Settings";

    private JDialog optionsWindow;
    private final MyLayerListener layerListener;

    private MagicWandModel model;
    private UndoContext undoContext;
    private MagicWandForm form;

    public MagicWandInteractor() {
        layerListener = new MyLayerListener();
        model = new MagicWandModel();
    }

    static double[] getSpectrum(Band[] bands, int pixelX, int pixelY) throws IOException {
        final double[] pixel = new double[1];
        final double[] spectrum = new double[bands.length];

        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            band.readPixels(pixelX, pixelY, 1, 1, pixel, com.bc.ceres.core.ProgressMonitor.NULL);
            final double value;
            if (band.isPixelValid(pixelX, pixelY)) {
                value = pixel[0];
            } else {
                value = Double.NaN;
            }
            spectrum[i] = value;
        }
        return spectrum;
    }

    @Override
    public boolean activate() {
        if (optionsWindow == null) {
            optionsWindow = createOptionsWindow();
        }
        optionsWindow.setVisible(true);

        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            view.getRootLayer().addListener(layerListener);
        }

        return super.activate();
    }

    @Override
    public void deactivate() {
        super.deactivate();

        if (optionsWindow != null) {
            optionsWindow.setVisible(false);
        }

        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            view.getRootLayer().removeListener(layerListener);
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {

        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view == null) {
            return;
        }
        final Product product = view.getProduct();
        final Band[] spectralBands = getSpectralBands(product);
        if (spectralBands.length == 0) {
            VisatApp.getApp().showErrorDialog("No spectral bands found.");
            return;
        }

        final Point2D mp = toModelPoint(event);
        // todo - convert to image point and check against image boundaries! (nf)
        final int pixelX = (int) mp.getX();
        final int pixelY = (int) mp.getY();
        final double[] spectrum;
        try {
            spectrum = getSpectrum(spectralBands, pixelX, pixelY);
        } catch (IOException e1) {
            return;
        }
        MagicWandModel oldModel = getModel().clone();
        getModel().addSpectrum(spectrum);
        updateMagicWandMask(product, spectralBands);
        MagicWandModel newModel = getModel().clone();
        undoContext.postEdit(new MyUndoableEdit(oldModel, newModel));
    }

    void updateMask() {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            final Product product = view.getProduct();
            updateMagicWandMask(product, MagicWandModel.getSpectralBands(product));
        }
    }

    private void updateMagicWandMask(Product product, Band[] spectralBands) {
        MagicWandModel.setMagicWandMask(product, getModel().createExpression(spectralBands));
    }

    private JDialog createOptionsWindow() {
        form = new MagicWandForm(this);
        JDialog optionsWindow = new JDialog(VisatApp.getApp().getMainFrame(), DIALOG_TITLE, false);
        UIUtils.centerComponent(optionsWindow, VisatApp.getApp().getMainFrame());
        optionsWindow.getContentPane().add(form.createPanel());
        optionsWindow.pack();
        return optionsWindow;
    }

    public MagicWandModel getModel() {
        return model;
    }

    public void setUndoContext(UndoContext undoContext) {
        this.undoContext = undoContext;
    }

    void updateModel(MagicWandModel other) {
        getModel().set(other);
        updateMask();
        form.getBindingContext().adjustComponents();
        form.updateUndoRedoState();
    }


    /**
     * A layer listener that sets the layer for "magic_wand" mask
     * visible, once it is added to the view's layer tree.
     */
    private static class MyLayerListener extends AbstractLayerListener {
        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            for (Layer childLayer : childLayers) {
                LayerType layerType = childLayer.getLayerType();
                if (layerType instanceof MaskLayerType) {
                    if (childLayer.getName().equals(MAGIC_WAND_MASK_NAME)) {
                        childLayer.setVisible(true);
                    }
                }
            }
        }
    }

    private class MyUndoableEdit extends AbstractUndoableEdit {
        private final MagicWandModel oldModel;
        private final MagicWandModel newModel;

        public MyUndoableEdit(MagicWandModel oldModel, MagicWandModel newModel) {
            this.oldModel = oldModel;
            this.newModel = newModel;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            updateModel(oldModel);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            updateModel(newModel);
        }

        @Override
        public String getPresentationName() {
            return "Modify magic wand mask";
        }
    }

}