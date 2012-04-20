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

package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * A panel which performs the 'compute' action.
 *
 * @author Marco Zuehlke
 */
class SingleRoiComputePanel extends JPanel {

    interface ComputeMask {
        void compute(Mask selectedMask);
    }

    private final ProductNodeListener productNodeListener;

    private final JButton computeButton;
    private final JCheckBox useRoiCheckBox;
    private final JComboBox maskNameComboBox;

    private RasterDataNode raster;
    private Product product;

    SingleRoiComputePanel(final ComputeMask method, final RasterDataNode raster) {
        productNodeListener = new PNL();
        final Icon icon = UIUtils.loadImageIcon("icons/ViewRefresh16.png");

        computeButton = new JButton("Compute");     /*I18N*/
        computeButton.setMnemonic('A');
        computeButton.setEnabled(raster != null);
        computeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean useRoi = useRoiCheckBox.isSelected();
                if (useRoi) {
                    String selectedMaskName = (String) maskNameComboBox.getSelectedItem();
                    Mask mask = product.getMaskGroup().get(selectedMaskName);
                    method.compute(mask);
                } else {
                    method.compute(null);
                }
            }
        });
        computeButton.setIcon(icon);

        useRoiCheckBox = new JCheckBox("Use ROI-Mask");
        useRoiCheckBox.setMnemonic('R');
        maskNameComboBox = new JComboBox();

        useRoiCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateMaskListEnablement();
            }
        });

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.SOUTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        setLayout(tableLayout);

        add(computeButton);
        add(useRoiCheckBox);
        add(maskNameComboBox);

        setRaster(raster);
    }

    void setRaster(final RasterDataNode newRaster) {
        if (this.raster != newRaster) {
            this.raster = newRaster;
            if (newRaster == null) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = null;
                updateMaskListState();
            } else if (product != newRaster.getProduct()) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = newRaster.getProduct();
                if (product != null) {
                    product.addProductNodeListener(productNodeListener);
                }
                updateMaskListState();
            }
        }
    }

    private void updateMaskListState() {
        final boolean hasRaster = (raster != null);
        computeButton.setEnabled(hasRaster);
        if (hasRaster) {
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            final boolean hasRois = (maskGroup.getNodeCount() > 0);
            useRoiCheckBox.setEnabled(hasRois);
            if (hasRois) {
                maskNameComboBox.setModel(new DefaultComboBoxModel(maskGroup.getNodeNames()));
                maskNameComboBox.setSelectedIndex(0);
            } else {
                maskNameComboBox.setModel(new DefaultComboBoxModel());
                useRoiCheckBox.setSelected(false);
            }
        } else {
            maskNameComboBox.setModel(new DefaultComboBoxModel());
            useRoiCheckBox.setSelected(false);
        }
        updateMaskListEnablement();
    }

    private void updateMaskListEnablement() {
        boolean hasRoiMasks = maskNameComboBox.getModel().getSize() > 0;
        boolean useRoi = useRoiCheckBox.isSelected();
        maskNameComboBox.setEnabled(hasRoiMasks && useRoi);
    }

    private class PNL implements ProductNodeListener {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handleEvent(event);
        }

        private void handleEvent(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Mask) {
                updateMaskListState();
            }
        }
    }
}
