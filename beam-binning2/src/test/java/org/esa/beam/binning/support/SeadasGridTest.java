package org.esa.beam.binning.support;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Norman Fomferra
 */
public class SeadasGridTest {

    @Test
    public void testConstructorRejectsTooLargeBaseGrids() {

        // ok
        new SeadasGrid(new SEAGrid(41068));

        // not ok
        try {
            new SeadasGrid(new SEAGrid(41070));
            fail("IAE expected");
        } catch (IllegalArgumentException e) {
            assertEquals("Base grid has more than 2147483646 bins", e.getMessage());
        }
    }

    @Test
    public void testConvertCalvalusToSeadasBinIndex() {
        // 3 + 8 + 12 + 12 + 8 + 3 bins
        final SEAGrid baseGrid = new SEAGrid(6);

        // These asserts show the first bin indices of our SEA baseGrid.
        assertEquals(0L, baseGrid.getFirstBinIndex(0));
        assertEquals(3L, baseGrid.getFirstBinIndex(1));
        assertEquals(11L, baseGrid.getFirstBinIndex(2));
        assertEquals(23L, baseGrid.getFirstBinIndex(3));
        assertEquals(35L, baseGrid.getFirstBinIndex(4));
        assertEquals(43L, baseGrid.getFirstBinIndex(5));
        assertEquals(46L, baseGrid.getNumBins());

        // Our SEA baseGrid bin indices shall be converted to SeaDAS as follows:
        //
        //  Calvalus           SeaDAS (minus one)
        //  row     bin        row      bin
        //   0    0 ..  2       5    43 .. 45
        //   1    3 .. 10       4    35 .. 42
        //   2   11 .. 22       3    23 .. 34
        //   3   23 .. 34       2    11 .. 22
        //   4   35 .. 42       1     3 .. 10
        //   5   43 .. 45       0     0 ..  2

        SeadasGrid seadasGrid = new SeadasGrid(baseGrid);

        assertEquals(44, seadasGrid.convertBinIndex(0L));
        assertEquals(46, seadasGrid.convertBinIndex(2L));
        assertEquals(36, seadasGrid.convertBinIndex(3L));
        assertEquals(43, seadasGrid.convertBinIndex(10L));
        assertEquals(24, seadasGrid.convertBinIndex(11L));
        assertEquals(35, seadasGrid.convertBinIndex(22L));
        assertEquals(12, seadasGrid.convertBinIndex(23L));
        assertEquals(23, seadasGrid.convertBinIndex(34L));
        assertEquals(4, seadasGrid.convertBinIndex(35L));
        assertEquals(11, seadasGrid.convertBinIndex(42L));
        assertEquals(1, seadasGrid.convertBinIndex(43L));
        assertEquals(3, seadasGrid.convertBinIndex(45L));

        assertEquals(5, seadasGrid.convertRowIndex(0));
        assertEquals(4, seadasGrid.convertRowIndex(1));
        assertEquals(3, seadasGrid.convertRowIndex(2));
        assertEquals(2, seadasGrid.convertRowIndex(3));
        assertEquals(1, seadasGrid.convertRowIndex(4));
        assertEquals(0, seadasGrid.convertRowIndex(5));
    }

}
