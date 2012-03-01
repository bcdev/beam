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

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.ImageInfoEditor;
import org.esa.beam.util.math.MathUtils;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.NumberFormat;

/**
 * This class implements the Basic Color Manipulation GUI. It's design concept is similar to the ImageInfoEditor2 class that implements the "Sliders" option in BEAM.
 */
class BasicColorEditor extends ImageInfoEditor  {

    private final ColorManipulationForm parentForm;
    private boolean showExtraInfo;

    private double maxVal;
    private double minVal;
    private JFormattedTextField maxValField;
    private JFormattedTextField minValField;
    private JButton fileDefaultButton;
    private JButton dataDefaultButton;
    private JRadioButton logButton;
    private JRadioButton linearButton;
    private ButtonGroup buttonGroup;
    private NumberFormat valFormat;
    private boolean log10ScaledDisplay;


    BasicColorEditor(final ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        maxVal = parentForm.getMaxValueData();
        minVal = parentForm.getMinValueData();
        setLayout(new BorderLayout());
        setShowExtraInfo(true);
        log10ScaledDisplay = false;
        valFormat = NumberFormat.getNumberInstance();
        valFormat.setMaximumFractionDigits(4);
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
     * Has two buttons for loading default min and max values from color palette file (File Default) and product data (Data Default).
     * Has two text fields for receiving user input for min and max.
     * Has two radio buttons for choosing scaling options, log or linear.
     * All the above fields has listeners for the following logic:
     *
     * When "File Default" button is pressed, min and max should be loaded with the min/max sample values of the current color palette file.
     *                     If color palette file is not loaded, a dialog will be displayed with such message and min/max will not be changed.
     * When "Data Default" button is pressed, min and max should be loaded with the min/max sample values of the product data file.
     * Max should be greater or equal to Min.
     * When "log" scale is chosen, min and max cannot be zero or negative.
     * When a new file is loaded, the current min, max sample values should be in effect.
     *
     * Used the existing "Apply", "Reset", "Import", "Export" buttons.
     * @return
     */
    private JPanel createSimpleColorRampEditorComponent(){

        JPanel simpleColorEditorPanel = new JPanel();
        simpleColorEditorPanel.setLayout(new GridLayout(0,1)  );

        final JPanel minPanel = new JPanel();
        minPanel.setLayout(new FlowLayout());
        minPanel.add(new JLabel("Min:"));
        minValField = new JFormattedTextField(valFormat);
        minValField.setValue(new Double(minVal));
        minValField.setColumns(10);

        minValField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                //minVal = ((Number)minValField.getValue()).doubleValue();
                activateApply();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {

            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                //minVal = ((Number)minValField.getValue()).doubleValue();
                //activateApply();
            }
        });

        minValField.addMouseListener(new MouseListener() {
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
                //To change body of implemented methods use File | Settings | File Templates.
                double value;
                try {
                    String textValue = minValField.getText();
                    value = Double.parseDouble(textValue);
                }
                catch (NumberFormatException nfe){
                    JOptionPane.showMessageDialog(minPanel, "Please enter valid number." );
                    minValField.requestFocusInWindow();
                    return;
                }

                boolean valid = validateMinMax(value, maxVal);
                if (valid ) {
                    minVal = value ;
                    recomputeSamplePoints();
                    activateApply();
                } else {
                    minValField.requestFocusInWindow();
                    return;
                }
            }
        });
        minPanel.add(minValField);

        final JPanel maxPanel = new JPanel();
        maxPanel.setLayout(new FlowLayout());
        maxPanel.add(new JLabel("Max:"));
        maxValField = new JFormattedTextField(valFormat);
        maxValField.setValue(new Double(maxVal));
        maxValField.setColumns(10);

        maxValField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {

                activateApply();

            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {


            }
        });

        maxValField.addMouseListener(new MouseListener() {
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

               double value;
                try {
                    String textValue = maxValField.getText();
                    value = Double.parseDouble(textValue);
                }
                catch (NumberFormatException nfe){
                    JOptionPane.showMessageDialog(minPanel, "Please enter valid number." );
                    maxValField.requestFocusInWindow();
                    return;
                }

                boolean valid = validateMinMax(minVal, value);
                if (valid ) {
                    maxVal = value ;
                    recomputeSamplePoints();
                    activateApply();
                } else {
                    maxValField.requestFocusInWindow();
                    return;
                }
            }
        });

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

        minMaxPanel.add(minPanel,c);

        c.gridy = 1;

        minMaxPanel.add(maxPanel,c);



        JPanel scalePanel = new JPanel();
        scalePanel.setLayout(new BoxLayout(scalePanel, BoxLayout.Y_AXIS));
        scalePanel.setBorder(new TitledBorder(new EtchedBorder(), ""));

        buttonGroup = new ButtonGroup();
        logButton  = new JRadioButton("Log");
        logButton.setSelected(log10ScaledDisplay);
        buttonGroup.add(logButton);
        scalePanel.add(logButton);

        linearButton = new JRadioButton("Linear");
        linearButton.setSelected(!log10ScaledDisplay);
        buttonGroup.add(linearButton);
        scalePanel.add(linearButton);

        logButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                log10ScaledDisplay = true;
                validateMinMax(minVal, maxVal);     ///?? is this needed?
                recomputeSamplePoints();
            }
        });

        linearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                log10ScaledDisplay = false;
                recomputeSamplePoints();
            }
        });

        //final JPanel buttonPanel = new JPanel();
        //buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        fileDefaultButton = new JButton("File Default");
        fileDefaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if ( !parentForm.isColorPaletteFileLoaded() ) {
                    JOptionPane.showMessageDialog(minMaxPanel, "Color Palette File has not been loaded." );
                    return;
                }
                maxVal = parentForm.getMaxValueFile();
                minVal = parentForm.getMinValueFile();
                validateMinMax(minVal, maxVal );
                recomputeSamplePoints();
                if (minVal > ((Number)maxValField.getValue()).doubleValue()){
                    maxValField.setValue(maxVal);
                    minValField.setValue(minVal);
                }
                minValField.setValue(minVal);
                maxValField.setValue(maxVal);
                }
        });
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth =1;
        minMaxPanel.add(fileDefaultButton,c);

        dataDefaultButton = new JButton("Data Default");
        dataDefaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                minVal = parentForm.getMinValueData();
                maxVal = parentForm.getMaxValueData();
                validateMinMax(minVal, maxVal );
                recomputeSamplePoints();
                if (minVal > ((Number)maxValField.getValue()).doubleValue()){
                    maxValField.setValue(maxVal);
                    minValField.setValue(minVal);
                }
                minValField.setValue(minVal);
                maxValField.setValue(maxVal);
            }
        });
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        minMaxPanel.add(dataDefaultButton,c);

        JPanel simpleColorManipulationPanel = new JPanel();
        //simpleColorManipulationPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 1 ));
        simpleColorManipulationPanel.setLayout(new BoxLayout(simpleColorManipulationPanel, BoxLayout.Y_AXIS ));
                //simpleColorManipulationPanel.add(buttonPanel);
                simpleColorManipulationPanel.add(minMaxPanel);
        simpleColorManipulationPanel.add(scalePanel);


        simpleColorEditorPanel.add(simpleColorManipulationPanel);
        return simpleColorEditorPanel;
    }

    private double getValueForDisplay(double minSample) {
        if (!Double.isNaN(minSample)) { // prevents NaN to be converted to zero
            minSample = MathUtils.round(minSample, 1000.0);
        }
        return minSample;
    }

    private void activateApply(){
        parentForm.setCurrentMaxValue(maxVal);
        parentForm.setCurrentMinValue(minVal);
        parentForm.getProductSceneView().getRaster().setLog10ScaledDisplay(log10ScaledDisplay);
        parentForm.setApplyEnabled(true);
    }

    private void deactivateApply(){
        parentForm.setApplyEnabled(false);
    }

    protected void resetToDataDefault(){
        dataDefaultButton.doClick();
    }

    protected void resetToFileDefault(){
        fileDefaultButton.doClick();
    }
    protected void resetMinMax(){
        minVal = parentForm.getCurrentMinValue();
        maxVal = parentForm.getCurrentMaxValue();
        minValField.setValue(MathUtils.round((new Double(minVal)).doubleValue(), 1000));
        maxValField.setValue(MathUtils.round((new Double(maxVal)).doubleValue(), 1000));
    }
    protected void recomputeSamplePoints(){
        //validateMinMax(minVal, maxVal);
        RasterDataNode  rasterDataNode = parentForm.getProductSceneView().getRaster();
        rasterDataNode.setLog10ScaledDisplay(log10ScaledDisplay);
        if (!log10ScaledDisplay ) {
            //rasterDataNode.resetValidMask();
        }
        parentForm.getImageInfo().getColorPaletteDef().computeSamplePoints(minVal, maxVal, log10ScaledDisplay);
    //    super.setColorPalette(parentForm.getImageInfo().getColorPaletteDef().getColors());
        activateApply();
    }

    private void showErrorMessage(String errorMessage){
        JOptionPane.showMessageDialog(this, errorMessage );

    }
    protected boolean isLog10ScaledDisplay() {
        return log10ScaledDisplay;
    }
    private boolean validateMinMax(double min, double max){

        String errorMessage = new String();
        boolean valid = true;

        if ( min > max) {
            errorMessage = "Min cannot be greater than Max";
            valid = false;
            deactivateApply();
        } else if (log10ScaledDisplay) {
            if (min < 0 || max < 0 ){

                errorMessage = "Log cannot be applied to negative values. Will apply linear scale.";
                valid = false;

            } else if (min == 0 || max == 0 ) {
                errorMessage = "Log cannot be applied to zero. Will apply linear scale.";
                valid = false;
            }
            if (!valid) {
                logButton.setSelected(false);
                linearButton.setSelected(true);
                log10ScaledDisplay = false;

            }

        }
        if (errorMessage.length() != 0 )
        {
            showErrorMessage(errorMessage);
        }

        return valid;
    }


}
