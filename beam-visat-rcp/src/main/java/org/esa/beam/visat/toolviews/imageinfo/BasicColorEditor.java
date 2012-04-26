package org.esa.beam.visat.toolviews.imageinfo;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik
 * Date: 1/13/12
 * Time: 11:58 AM
 * To change this template use File | Settings | File Templates.
 */
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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.ui.ImageInfoEditor;
import org.esa.beam.util.math.MathUtils;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * This class implements the Basic Color Manipulation GUI. It's design concept is similar to the ImageInfoEditor2 class that implements the "Sliders" option in BEAM.
 */
class BasicColorEditor extends JPanel {

    private final ColorManipulationForm parentForm;
    private boolean showExtraInfo;

    private double maxVal;
    private double minVal;
    private JFormattedTextField maxValField;
    private JFormattedTextField minValField;
    private JCheckBox fileDefaultCheckBox;
    private JButton dataDefaultButton;
    private NumberFormat valFormat;
    private ImageInfoEditor imageInfoEditor;
        private double roundFactor;

    BasicColorEditor(final ColorManipulationForm parentForm, ImageInfoEditor imageInfoEditor) {
        this.parentForm = parentForm;
        maxVal = parentForm.getMaxValueData();
        minVal = parentForm.getMinValueData();
        setLayout(new BorderLayout());
        setShowExtraInfo(true);
        valFormat = NumberFormat.getNumberInstance();
        valFormat.setMaximumFractionDigits(4);
        this.imageInfoEditor = imageInfoEditor;
        //addChangeListener(new RepaintCL());
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

    private double getMinSample(){
        return parentForm.getImageInfo().getColorPaletteDef().getFirstPoint().getSample();
    }

    private double getMaxSample(){
        return parentForm.getImageInfo().getColorPaletteDef().getLastPoint().getSample();
    }

        private double round(double value) {
            roundFactor = MathUtils.computeRoundFactor(getMinSample(), getMaxSample(), 2);
        return MathUtils.round(value, roundFactor);
    }

    private int getSliderCount(){
        return   parentForm.getImageInfo().getColorPaletteDef().getNumPoints();
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
        final NumberFormatter formatter = new NumberFormatter(new DecimalFormat("#0.0#"));
        formatter.setValueClass(Double.class); // to ensure that double values are returned
        final JFormattedTextField field = new JFormattedTextField(formatter);
        field.setColumns(11);
        field.setHorizontalAlignment(JFormattedTextField.RIGHT);
        ctx.bind(sampleName, field);

        ctx.addPropertyChangeListener(sampleName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                //imageInfoEditor.getModel().setSliderSample(sliderIndex, (Double) ctx.getBinding(sampleName).getPropertyValue());
                //imageInfoEditor.updateMinMax();
                if (sampleName.equals("minSample")) {
                    minVal = (Double) ctx.getBinding(sampleName).getPropertyValue();
                }  else {
                    maxVal = (Double) ctx.getBinding(sampleName).getPropertyValue();
                }

                imageInfoEditor.updateMinMax(minVal, maxVal);
            }
        });

        return field;
    }



//    private void editSliderSample(MouseEvent evt, final int sliderIndex) {
//         final PropertyContainer vc = new PropertyContainer();
//         vc.addProperty(Property.create("sample", getSliderSample(sliderIndex)));
//         vc.getDescriptor("sample").setDisplayName("sample");
//         vc.getDescriptor("sample").setUnit(getModel().getParameterUnit());
//         final ValueRange valueRange;
//         if (sliderIndex == 0) {
//             valueRange = new ValueRange(Double.NEGATIVE_INFINITY, round(getMaxSliderSample(sliderIndex)));
//         } else if (sliderIndex == getSliderCount() - 1) {
//             valueRange = new ValueRange(round(getMinSliderSample(sliderIndex)), Double.POSITIVE_INFINITY);
//         } else {
//             valueRange = new ValueRange(round(getMinSliderSample(sliderIndex)), round(getMaxSliderSample(sliderIndex)));
//         }
//         vc.getDescriptor("sample").setValueRange(valueRange);
//
//         final BindingContext ctx = new BindingContext(vc);
//         final NumberFormatter formatter = new NumberFormatter(new DecimalFormat("#0.0#"));
//         formatter.setValueClass(Double.class); // to ensure that double values are returned
//         final JFormattedTextField field = new JFormattedTextField(formatter);
//         field.setColumns(11);
//         field.setHorizontalAlignment(JFormattedTextField.RIGHT);
//         ctx.bind("sample", field);
//
//         showPopup(evt, field);
//
//         ctx.addPropertyChangeListener("sample", new PropertyChangeListener() {
//
//             @Override
//             public void propertyChange(PropertyChangeEvent pce) {
//                 hidePopup();
//                 setSliderSample(sliderIndex, (Double) ctx.getBinding("sample").getPropertyValue());
//                 computeZoomInToSliderLimits();
//             }
//         });
//     }


    /**
     * Returns a JPanel object that contains the Basic Color Editor elements.
     * Has two buttons for loading default min and max values from color palette file (File Default) and product data (Data Default).
     * Has two text fields for receiving user input for min and max.
     * Has two radio buttons for choosing scaling options, log or linear.
     * All the above fields has listeners for the following logic:
     * <p/>
     * When "File Default" button is pressed, min and max should be loaded with the min/max sample values of the current color palette file.
     * If color palette file is not loaded, a dialog will be displayed with such message and min/max will not be changed.
     * When "Data Default" button is pressed, min and max should be loaded with the min/max sample values of the product data file.
     * Max should be greater or equal to Min.
     * <p/>
     * Used the existing "Apply", "Reset", "Import", "Export" buttons.
     *
     * @return
     */
    private JPanel createSimpleColorRampEditorComponent() {

        JPanel simpleColorEditorPanel = new JPanel();
        simpleColorEditorPanel.setLayout(new GridLayout(0, 1));

        final JPanel minPanel = new JPanel();
        minPanel.setLayout(new FlowLayout());
        minPanel.add(new JLabel("Min:"));
//        minValField = new JFormattedTextField(valFormat);
//        minValField.setValue(new Double(minVal));
//        minValField.setColumns(10);
//
//        minValField.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent documentEvent) {
//                //minVal = ((Number)minValField.getValue()).doubleValue();
//                activateApply();
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent documentEvent) {
//
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent documentEvent) {
//                //minVal = ((Number)minValField.getValue()).doubleValue();
//                //activateApply();
//            }
//        });
//
//        minValField.addMouseListener(new MouseListener() {
//            @Override
//            public void mouseClicked(MouseEvent mouseEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void mousePressed(MouseEvent mouseEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void mouseReleased(MouseEvent mouseEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void mouseEntered(MouseEvent mouseEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void mouseExited(MouseEvent mouseEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//                double value;
//                try {
//                    String textValue = minValField.getText();
//                    value = Double.parseDouble(textValue);
//                } catch (NumberFormatException nfe) {
//                    JOptionPane.showMessageDialog(minPanel, "Please enter valid number.");
//                    minValField.requestFocusInWindow();
//                    return;
//                }
//
//                boolean valid = validateMinMax(value, maxVal);
//                if (valid) {
//                    minVal = value;
//                    recomputeSamplePoints();
//                    activateApply();
//                } else {
//                    minValField.requestFocusInWindow();
//                    return;
//                }
//            }
//        });

        minValField = editSliderSampleMinMax(0, "minSample");
        minPanel.add(minValField);

        final JPanel maxPanel = new JPanel();
        maxPanel.setLayout(new FlowLayout());
        maxPanel.add(new JLabel("Max:"));
//        maxValField = new JFormattedTextField(valFormat);
//        maxValField.setValue(new Double(maxVal));
//        maxValField.setColumns(10);
//
//        maxValField.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent documentEvent) {
//
//                activateApply();
//
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent documentEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent documentEvent) {
//
//
//            }
//        });
//
//        maxValField.addMouseListener(new MouseListener() {
//            @Override
//            public void mouseClicked(MouseEvent mouseEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void mousePressed(MouseEvent mouseEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void mouseReleased(MouseEvent mouseEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void mouseEntered(MouseEvent mouseEvent) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void mouseExited(MouseEvent mouseEvent) {
//
//                double value;
//                try {
//                    String textValue = maxValField.getText();
//                    value = Double.parseDouble(textValue);
//                } catch (NumberFormatException nfe) {
//                    JOptionPane.showMessageDialog(minPanel, "Please enter valid number.");
//                    maxValField.requestFocusInWindow();
//                    return;
//                }
//
//                boolean valid = validateMinMax(minVal, value);
//                if (valid) {
//                    maxVal = value;
//                    recomputeSamplePoints();
//                    activateApply();
//                } else {
//                    maxValField.requestFocusInWindow();
//                    return;
//                }
//            }
//        });

        maxValField = editSliderSampleMinMax(getSliderCount()-1, "maxSample");
        maxPanel.add(maxValField);


        final JPanel minMaxPanel = new JPanel();

        minMaxPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        minMaxPanel.setBorder(new TitledBorder(new EtchedBorder(), ""));

        //c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;

        minMaxPanel.add(minPanel, c);

        c.gridy = 1;

        minMaxPanel.add(maxPanel, c);


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
//                recomputeSamplePoints();
                    //         if (minVal > ((Number) maxValField.getValue()).doubleValue()) {
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
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        minMaxPanel.add(fileDefaultCheckBox, c);

        dataDefaultButton = new JButton("Data Default");
        dataDefaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileDefaultCheckBox.setSelected(false);
                minVal = parentForm.getMinValueData();
                maxVal = parentForm.getMaxValueData();
                validateMinMax(minVal, maxVal);
                recomputeSamplePoints();
                if (minVal > ((Number) maxValField.getValue()).doubleValue()) {
                    maxValField.setValue(maxVal);
                    minValField.setValue(minVal);
                }
                minValField.setValue(minVal);
                maxValField.setValue(maxVal);
                minValField.setEditable(true);
                maxValField.setEditable(true);

            }
        });
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        minMaxPanel.add(dataDefaultButton, c);

        JPanel simpleColorManipulationPanel = new JPanel();
        simpleColorManipulationPanel.setLayout(new BoxLayout(simpleColorManipulationPanel, BoxLayout.Y_AXIS));
        simpleColorManipulationPanel.add(minMaxPanel);
        //simpleColorManipulationPanel.add(scalePanel);


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
        minValField.setValue(MathUtils.round((new Double(minVal)).doubleValue(), 1000));
        maxValField.setValue(MathUtils.round((new Double(maxVal)).doubleValue(), 1000));
    }


    protected void recomputeSamplePoints() {
//        //validateMinMax(minVal, maxVal);
        //  RasterDataNode rasterDataNode = parentForm.getProductSceneView().getRaster();
////        rasterDataNode.setLog10ScaledDisplay(logScaled);
//        if (!logScaled) {
//            //rasterDataNode.resetValidMask();
//        }
        //parentForm.getImageInfo().getColorPaletteDef().computeSamplePoints(minVal, maxVal);
        //    super.setColorPalette(parentForm.getImageInfo().getColorPaletteDef().getColors());
        activateApply();
    }

    private void showErrorMessage(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage);

    }

//    protected boolean isLogScaled() {
//        return logScaled;
//    }

    private boolean validateMinMax(double min, double max) {

        String errorMessage = new String();
        boolean valid = true;

        if (min > max) {
            errorMessage = "Min cannot be greater than Max";
            valid = false;
            deactivateApply();
//        } else if (logScaled) {
//            if (min < 0 || max < 0) {
//
//                errorMessage = "Log cannot be applied to negative values. Will apply linear scale.";
//                valid = false;
//
//            } else if (min == 0 || max == 0) {
//                errorMessage = "Log cannot be applied to zero. Will apply linear scale.";
//                valid = false;
//            }
//            if (!valid) {
//                logButton.setSelected(false);
//                linearButton.setSelected(true);
//                logScaled = false;
//
//            }
//
        }
        if (errorMessage.length() != 0) {
            showErrorMessage(errorMessage);
        }

        return valid;
    }


}
