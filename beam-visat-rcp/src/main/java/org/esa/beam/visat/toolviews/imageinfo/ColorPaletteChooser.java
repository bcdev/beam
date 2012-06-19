package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.Scaling;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: dshea
 * User: aabduraz
 * Date: 4/10/12
 * Time: 9:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class ColorPaletteChooser extends JComboBox {
    private final int COLORBAR_HEIGHT = 15;
    private final int COLORBAR_WIDTH = 204;
    private final String DEFAULT_GRAY_COLOR_PALETTE_FILE_NAME = "defaultGrayColor.cpd";

    private File colorPaletteDir;
    private Dimension colorBarDimension;
    private ComboBoxModel colorModel;
    private ArrayList<ImageIcon> icons;

    public ColorPaletteChooser(File colorPaletteDir) {
        super();
        colorBarDimension = new Dimension(COLORBAR_WIDTH, COLORBAR_HEIGHT);
        this.colorPaletteDir = colorPaletteDir;
        updateDisplay();
        setColorPaletteDir(colorPaletteDir);
        setEditable(false);
        setRenderer(new ComboBoxRenderer());
    }

    public ColorPaletteChooser(File colorPaletteDir, ColorPaletteDef defaultColorPaletteDef) {
        super();
        colorBarDimension = new Dimension(COLORBAR_WIDTH, COLORBAR_HEIGHT);
        this.colorPaletteDir = colorPaletteDir;
        createDefaultGrayColorPaletteFile(defaultColorPaletteDef);
        updateDisplay();
        setColorPaletteDir(colorPaletteDir);
        setEditable(false);
        setRenderer(new ComboBoxRenderer());

    }

    public void distributeSlidersEvenly(ColorPaletteDef colorPaletteDef) {
        final double pos1 = colorPaletteDef.getMinDisplaySample();
        final double pos2 = colorPaletteDef.getMaxDisplaySample();
        final double delta = pos2 - pos1;
        final double evenSpace = delta / (colorPaletteDef.getNumPoints() - 1);
        for (int i = 1; i < colorPaletteDef.getNumPoints() - 1; i++) {
            final double value = pos1 + evenSpace * i;
            colorPaletteDef.getPointAt(i).setSample(value);
        }
    }

    private void drawPalette(Graphics2D g2, ColorPaletteDef colorPaletteDef, Rectangle paletteRect) {
        int paletteX1 = paletteRect.x;
        int paletteX2 = paletteRect.x + paletteRect.width;

        g2.setStroke(new BasicStroke(1.0f));
        Color[] colorPalette = colorPaletteDef.createColorPalette(Scaling.IDENTITY);
        int divisor = paletteX2 - paletteX1;
        for (int x = paletteX1; x < paletteX2; x++) {

            int palIndex = ((colorPalette.length * (x - paletteX1)) / divisor);

            if (palIndex < 0) {
                palIndex = 0;
            }
            if (palIndex >= colorPalette.length) {
                palIndex = colorPalette.length - 1;
            }
            g2.setColor(colorPalette[palIndex]);
            g2.drawLine(x, paletteRect.y, x, paletteRect.y + paletteRect.height);
        }
    }

       private ImageIcon createGrayColorBarIcon(ColorPaletteDef colorPaletteDef, Dimension dimension) {
       BufferedImage bufferedImage = new BufferedImage(dimension.width, dimension.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        drawPalette(g2, colorPaletteDef, new Rectangle(dimension));
        ImageIcon icon = new ImageIcon(bufferedImage);
        icon.setDescription(DEFAULT_GRAY_COLOR_PALETTE_FILE_NAME);
        return icon;
    }

    private void createDefaultGrayColorPaletteFile(ColorPaletteDef defaultGrayColorPaletteDef) {

        File grayColorFile = new File(colorPaletteDir, DEFAULT_GRAY_COLOR_PALETTE_FILE_NAME);
        try {
            ColorPaletteDef.storeColorPaletteDef(defaultGrayColorPaletteDef, grayColorFile);
        } catch (IOException ioe) {
            System.err.println("Default Gray Color File is not created!");
        }
    }

    private void drawPalette(Graphics2D g2, File paletteFile, Rectangle paletteRect) throws IOException {
        ColorPaletteDef colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(paletteFile);
        distributeSlidersEvenly(colorPaletteDef);
        drawPalette(g2, colorPaletteDef, paletteRect);
    }

    private ImageIcon createColorBarIcon(File cpdFile, Dimension dimension) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(dimension.width, dimension.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        drawPalette(g2, cpdFile, new Rectangle(dimension));
        ImageIcon icon = new ImageIcon(bufferedImage);
        icon.setDescription(cpdFile.getName());
        return icon;
    }

    ComboBoxModel createColorBarModel() {
        ArrayList<ImageIcon> icons = new ArrayList<ImageIcon>();
        File[] files = colorPaletteDir.listFiles();
        ImageIcon defaultIcon = null;
        for (File file : files) {
            try {
                ImageIcon icon = createColorBarIcon(file, colorBarDimension);
                icons.add(icon);
                if (file.getName().indexOf(DEFAULT_GRAY_COLOR_PALETTE_FILE_NAME) != -1) {
                    defaultIcon = icon;
                }
            } catch (IOException e) {
            }
        }
        Collections.sort(icons, new Comparator<ImageIcon>() {
            @Override
            public int compare(ImageIcon o1, ImageIcon o2) {
                return o1.getDescription().compareTo(o2.getDescription());
            }
        });
        this.icons = icons;
        System.out.println("number of icons before sorting :"  + this.icons.size() );
        this.icons.remove(defaultIcon);
        this.icons.add(0, defaultIcon);
              System.out.println("number of icons after sorting :"  + this.icons.size() );
        DefaultComboBoxModel colorBarModel = new DefaultComboBoxModel(icons.toArray(new ImageIcon[icons.size()]));
        colorBarModel.setSelectedItem(defaultIcon);
        return  colorBarModel;
    }

    private void updateDisplay() {
        colorModel = createColorBarModel();
        setModel(colorModel);
    }

    public void resetToDefaultGrayColorPalette(ColorPaletteDef colorPaletteDef){
       createDefaultGrayColorPaletteFile(colorPaletteDef);
       ImageIcon defaultGrayColorBarIcon = createGrayColorBarIcon(colorPaletteDef, colorBarDimension);
//        icons.remove();
        icons.add(defaultGrayColorBarIcon);
      colorModel.setSelectedItem(defaultGrayColorBarIcon);
        setModel(colorModel);

    }
    public File getColorPaletteDir() {
        return colorPaletteDir;
    }

    public void setColorPaletteDir(File colorPaletteDir) {
        this.colorPaletteDir = colorPaletteDir;
        updateDisplay();
    }

    public Dimension getColorBarDimension() {
        return colorBarDimension;
    }

    public void setColorBarDimension(Dimension colorBarDimension) {
        this.colorBarDimension = colorBarDimension;
        updateDisplay();
    }

    public ColorPaletteDef getSelectedColorPaletteDef() {
        Object obj = getSelectedItem();
        File paletteFile = new File(colorPaletteDir, obj.toString());
        ColorPaletteDef colorPaletteDef = null;
        try {
            colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(paletteFile);
        } catch (IOException e) {
        }
        return colorPaletteDef;
    }

    private class ComboBoxRenderer extends JLabel implements ListCellRenderer {

        public ComboBoxRenderer() {
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }

        /*
        * This method finds the image and text corresponding
        * to the selected value and returns the label, set up
        * to display the text and image.
        */
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            ImageIcon icon = (ImageIcon) value;
            BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth() + 2, icon.getIconHeight() + 2,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bufferedImage.createGraphics();
            if (isSelected) {
                g2.setColor(Color.darkGray);
            } else {
                g2.setColor(list.getParent().getBackground());
            }
            g2.fillRect(0, 0, icon.getIconWidth() + 2, icon.getIconHeight() + 2);
            g2.drawImage(icon.getImage(), 1, 1, null);

            setIcon(new ImageIcon(bufferedImage));
            setToolTipText(icon.getDescription());

            return this;
        }

    }


}
