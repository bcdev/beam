package org.esa.beam.meris.radiometry.smilecorr;

import java.io.IOException;

/**
 * @author Marco Peters
 */
public class Main {

    private static final int BAND_10_INDEX = 9;
    private static final int BAND_11_INDEX = 10;
    private static final int NUM_DETECTOR_INDEX = 3700;


    public static void main(String[] args) throws IOException {

        final SmileCorrectionAuxdata auxdata = SmileCorrectionAuxdata.loadAuxdata("MER_FR");
        final CorrFac115 corrFac115 = new CorrFac115();
        final double[][] detectorWavelengths = auxdata.getDetectorWavelengths();
        String prefix = "old_";
        System.out.println(prefix + "detectorWL" + "\t" + prefix + "corrFac115" + "\t" + prefix + "E0_Band11" + "\t" + "E0_Band10");
        final double[][] detectorSunSpectralFluxes = auxdata.getDetectorSunSpectralFluxes();
        for (int detectorIndex = 0; detectorIndex < NUM_DETECTOR_INDEX; detectorIndex++) {
            final double detectorWL = detectorWavelengths[detectorIndex][BAND_11_INDEX];
            final double band11_E0 = detectorSunSpectralFluxes[detectorIndex][BAND_11_INDEX];
            final double band10_E0 = detectorSunSpectralFluxes[detectorIndex][BAND_10_INDEX];
            System.out.println(detectorWL + "\t" + corrFac115.y(detectorWL) + "\t" + band11_E0 + "\t" + band10_E0);
        }
    }
}
