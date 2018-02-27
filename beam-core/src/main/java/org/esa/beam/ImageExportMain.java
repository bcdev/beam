package org.esa.beam;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.ProductUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Marco Peters
 */
public class ImageExportMain {

    public static void main(String[] args) {

        String[] rgbBandNames = new String[3];
        rgbBandNames[0] = "radiance_3";
        rgbBandNames[1] = "radiance_2";
        rgbBandNames[2] = "radiance_1";

        try {
            Product inputProduct = ProductIO.readProduct(args[0]);
//                        Product inputProduct = ProductIO.readProduct("G:\\EOData\\ALOS\\AVNIR2\\AV2_MMC_FR__0000091001\\VOL-ALAV2A038712630-O1B2G_U");
            System.out.println(inputProduct);
            Band[] produtBands = inputProduct.getBands();
            Band[] rgbBands = new Band[3];

            int n = 0;
            for (Band band : produtBands) {

                if (band.getName().equals(rgbBandNames[0])) {
                    rgbBands[0] = band;
                } else if (band.getName().equals(rgbBandNames[1])) {
                    rgbBands[1] = band;
                } else if (band.getName().equals(rgbBandNames[2])) {
                    rgbBands[2] = band;
                }

                n = n + 1;
            }

            ImageInfo outImageInfo = ProductUtils.createImageInfo(rgbBands, true, ProgressMonitor.NULL);
            BufferedImage outImage = ProductUtils.createRgbImage(rgbBands, outImageInfo, ProgressMonitor.NULL);

            final File outputFile = new File(inputProduct.getName() + ".png");
            ImageIO.write(outImage, "PNG", outputFile);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

}
