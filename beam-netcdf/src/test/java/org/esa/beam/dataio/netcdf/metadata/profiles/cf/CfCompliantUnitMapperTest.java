package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.junit.Test;

import static org.junit.Assert.*;

public class CfCompliantUnitMapperTest {

    @Test
    public void testTryFindCFCompliantUnitString() {
        assertEquals("degree", CfCompliantUnitMapper.tryFindUnitString("deg"));
    }

    @Test
    public void testUnknownStaysUnchanged() {
        assertEquals("barz", CfCompliantUnitMapper.tryFindUnitString("barz"));
    }
}
