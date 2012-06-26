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


import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.ui.ImageInfoEditor;
import org.esa.beam.util.math.MathUtils;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * This class implements the Basic Color Manipulation GUI. It's design concept is similar to the ImageInfoEditor2 class that implements the "Sliders" option in BEAM.
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik
 * Date: 1/13/12
 * Time: 11:58 AM
 * To change this template use File | Settings | File Templates.
 */

class BasicColorEditor extends JPanel {

    private final ColorManipulationForm parentForm;
    private boolean showExtraInfo;

    private double maxVal;
    private double minVal;
    private double minValData, maxValData;
    private JFormattedTextField maxValField;
    private JFormattedTextField minValField;
    private JCheckBox fileDefaultCheckBox;
    private JButton dataDefaultButton;
    private NumberFormat valFormat;
    private ImageInfoEditor imageInfoEditor;
    private double roundFactor;

    private ColorPaletteChooser colorChooser;

    private ChangeListener applyEnablerCL;

    private ColorPaletteDef defaultColorPaletteDef;
    private String cpdFileName;

    BasicColorEditor(final ColorManipulationForm parentForm, ImageInfoEditor imageInfoEditor) {
        this.parentForm = parentForm;
        maxVal = parentForm.getMaxValueData();
        minVal = parentForm.getMinValueData();
        minValData = minVal;
        maxValData = maxVal;
        defaultColorPaletteDef = parentForm.getImageInfo().getColorPaletteDef();
        setLayout(new BorderLayout());
        setShowExtraInfo(true);
        valFormat = NumberFormat.getNumberInstance();
        valFormat.setMaximumFractionDigits(4);
        this.imageInfoEditor = imageInfoEditor;
        applyEnablerCL = parentForm.createApplyEnablerChangeListener();
    }

    public boolean getShowExtraInfo() {
        return showExtraInfo;
    }

    public void setShowExtraInfo(boolean showExtraInfo) {
        boolean oldValue = this.showExtraInfo;
        if (oldValue != showExtraInfo) {
            this.showExtraInfo = showExtraInfo;
            updateStxOverlayComponent();
            firePropertyChange("showExtraInfo", oldValue, this.showExtraInfo);
        }
    }

    private void updateStxOverlayComponent() {
        removeAll();
        add(createSimpleColorRampEditorComponent(), BorderLayout.NORTH);
        revalidate();
        repaint();
    }


    /**
     * Returns a JPanel object that contains the Basic Color Editor elements.
     *
     * @return
     */
    private JPanel createSimpleColorRampEditorComponent() {

        JPanel simpleColorEditorPanel = new JPanel();
        simpleColorEditorPanel.setLayout(new GridLayout(0, 1));

        //colorChooser = new ColorPaletteChooser(new File("/Users/aabduraz/.beam/beam-ui/auxdata/color-palettes"));
        colorChooser = new ColorPaletteChooser(parentForm.getIODir(), defaultColorPaletteDef);
        colorChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ImageIcon currentColorBar = (ImageIcon) colorChooser.getSelectedItem();
                cpdFileName = currentColorBar.getDescription();
                File cpdFile = colorChooser.getColorPaletteDir();
                parentForm.loadColorPaletteFile(new File(cpdFile, cpdFileName));
                fileDefaultCheckBox.doClick();
                parentForm.setApplyEnabled(true);
            }
        });

        colorChooser.addPropertyChangeListener("color_bar_changed", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });


        parentForm.getProductSceneView().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        final JPanel minPanel = new JPanel();
        minPanel.setLayout(new BoxLayout(minPanel, BoxLayout.Y_AXIS));
        minPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        minPanel.add(new JLabel("Min:"));

        minValField = editSliderSampleMinMax(0, "minSample");
        minPanel.add(minValField);


        final JPanel maxPanel = new JPanel();
        maxPanel.setLayout(new BoxLayout(maxPanel, BoxLayout.Y_AXIS));
        maxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        maxPanel.add(new JLabel("Max:"));
        maxValField = editSliderSampleMinMax(getSliderCount() - 1, "maxSample");
        maxPanel.add(maxValField);


        final JPanel minMaxPanel = new JPanel();
        TableLayout basicPanelLayout = new TableLayout(2);
        basicPanelLayout.setCellColspan(0, 0, 2);
        basicPanelLayout.setTablePadding(10, 10);

        minMaxPanel.setLayout(basicPanelLayout);

        minMaxPanel.add(colorChooser);
        minMaxPanel.add(minPanel);
        minMaxPanel.add(maxPanel);

        fileDefaultCheckBox = new JCheckBox("File Default");
        fileDefaultCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (!parentForm.isColorPaletteFileLoaded()) {
                    JOptionPane.showMessageDialog(minMaxPanel, "Color Palette File has not been loaded.");
                    fileDefaultCheckBox.setSelected(false);
                    return;
                }

                AbstractButton abstractButton = (AbstractButton) actionEvent
                        .getSource();
                boolean selected = abstractButton.getModel().isSelected();

                if (selected) {
                    maxVal = parentForm.getMaxValueFile();
                    minVal = parentForm.getMinValueFile();
                    validateMinMax(minVal, maxVal);
                    maxValField.setValue(maxVal);
                    minValField.setValue(minVal);


                    minValField.setEditable(false);
                    maxValField.setEditable(false);
                    minValField.setEnabled(false);
                    maxValField.setEnabled(false);
                    minValField.setDisabledTextColor(Color.GRAY);
                    maxValField.setDisabledTextColor(Color.GRAY);
                } else {

                    minValField.setEnabled(true);
                    maxValField.setEnabled(true);
                    minValField.setEditable(true);
                    maxValField.setEditable(true);
                }

            }
        });

        dataDefaultButton = new JButton("Data Default");
        dataDefaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileDefaultCheckBox.setSelected(false);
                minVal = parentForm.getMinValueData();
                maxVal = parentForm.getMaxValueData();

                if (minVal > ((Number) maxValField.getValue()).doubleValue()) {
                    maxValField.setValue(maxVal);
                    minValField.setValue(minVal);
                }
                minValField.setValue(minVal);
                maxValField.setValue(maxVal);
                minValField.setEnabled(true);
                maxValField.setEnabled(true);
                minValField.setEditable(true);
                maxValField.setEditable(true);

            }
        });

        minMaxPanel.add(fileDefaultCheckBox);
        minMaxPanel.add(dataDefaultButton);
        JPanel simpleColorManipulationPanel = new JPanel();
        simpleColorManipulationPanel.setLayout(new BoxLayout(simpleColorManipulationPanel, BoxLayout.Y_AXIS));
        simpleColorManipulationPanel.add(minMaxPanel);

        simpleColorEditorPanel.add(simpleColorManipulationPanel);
        return simpleColorEditorPanel;
    }

    private double getValueForDisplay(double minSample) {
        if (!Double.isNaN(minSample)) { // prevents NaN to be converted to zero
            minSample = MathUtils.round(minSample, 1000.0);
        }
        return minSample;
    }

    private void activateApply() {
        parentForm.setCurrentMaxValue(maxVal);
        parentForm.setCurrentMinValue(minVal);
        parentForm.setApplyEnabled(true);
    }

    private void deactivateApply() {
        parentForm.setApplyEnabled(false);
    }

    protected void resetToDataDefault() {
        dataDefaultButton.doClick();
    }

    protected void resetToFileDefault() {
        fileDefaultCheckBox.doClick();
    }

    protected void resetMinMax() {
        minVal = parentForm.getCurrentMinValue();
        maxVal = parentForm.getCurrentMaxValue();
        minValField.setValue(MathUtils.round((new Double(minVal)).doubleValue(), 100000000));
        maxValField.setValue(MathUtils.round((new Double(maxVal)).doubleValue(), 100000000));
        if (minValData != parentForm.getMinValueData() || maxValData != parentForm.getMaxValueData()) {
            resetDefaultColorFile(parentForm.getImageInfo().getColorPaletteDef());
            minValData = parentForm.getMinValueData();
            maxValData = parentForm.getMaxValueData();
        }
    }


    private void showErrorMessage(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage);

    }

    protected void resetDefaultColorFile(ColorPaletteDef colorPaletteDef) {
        defaultColorPaletteDef = colorPaletteDef;
        colorChooser.resetToDefaultColorPalette(defaultColorPaletteDef);
    }

    private boolean validateMinMax(double min, double max) {

        String errorMessage = new String();
        boolean valid = true;

        if (min > max) {
            errorMessage = "Min cannot be greater than Max";
            valid = false;
            deactivateApply();
        }
        if (errorMessage.length() != 0) {
            showErrorMessage(errorMessage);
        }

        return valid;
    }

    private double getMinSample() {
        return parentForm.getImageInfo().getColorPaletteDef().getFirstPoint().getSample();
    }

    private double getMaxSample() {
        return parentForm.getImageInfo().getColorPaletteDef().getLastPoint().getSample();
    }

    private double round(double value) {
        roundFactor = MathUtils.computeRoundFactor(getMinSample(), getMaxSample(), 8);
        return MathUtils.round(value, roundFactor);
    }

    private int getSliderCount() {
        return parentForm.getImageInfo().getColorPaletteDef().getNumPoints();
    }

    private JFormattedTextField editSliderSampleMinMax(final int sliderIndex, final String sampleName) {
        final PropertyContainer vc = new PropertyContainer();
        vc.addProperty(Property.create(sampleName, parentForm.getImageInfo().getColorPaletteDef().getPointAt(sliderIndex).getSample()));
        vc.getDescriptor(sampleName).setDisplayName(sampleName);
        //vc.getDescriptor(sampleName).setUnit(getModel().getParameterUnit());
        final ValueRange valueRange;

        if (sliderIndex == 0) {
            valueRange = new ValueRange(Double.NEGATIVE_INFINITY, round(getMaxSample()));
        } else if (sliderIndex == getSliderCount() - 1) {
            valueRange = new ValueRange(round(getMinSample()), Double.POSITIVE_INFINITY);
        } else {
            valueRange = null;
        }


        vc.getDescriptor(sampleName).setValueRange(valueRange);

        final BindingContext ctx = new BindingContext(vc);
        final NumberFormatter formatter = new NumberFormatter(new DecimalFormat("#0.000000#"));
        formatter.setValueClass(Double.class); // to ensure that double values are returned
        final JFormattedTextField field = new JFormattedTextField(formatter);
        field.setColumns(9);
        field.setHorizontalAlignment(JFormattedTextField.LEFT);
        field.setFocusLostBehavior(0);
        ctx.bind(sampleName, field);

        field.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                double currentVal;
                try {
                    currentVal = Double.parseDouble(field.getText());

                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(field, "Please enter valid number.");
                    field.requestFocusInWindow();
                    return;
                }
                boolean valid = sampleName.equals("minSample") ? validateMinMax(currentVal, maxVal) : validateMinMax(minVal, currentVal);
                if (valid) {
                    field.setValue(currentVal);
                }
            }
        });
        ctx.addPropertyChangeListener(sampleName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                if (sampleName.equals("minSample")) {
                    minVal = (Double) ctx.getBinding(sampleName).getPropertyValue();
                } else {
                    maxVal = (Double) ctx.getBinding(sampleName).getPropertyValue();
                }

                imageInfoEditor.updateMinMax(minVal, maxVal);
                activateApply();
            }
        });

        return field;
    }


}
