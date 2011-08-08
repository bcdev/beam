package org.esa.beam.dataio.netcdf.util;

import org.junit.Test;
import ucar.ma2.DataType;

import static org.junit.Assert.*;

public class DataTypeUtilsTest {

    @Test
    public void testConvertToBYTE() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.BYTE);
        assertEquals((byte) 12, convertedNumber);
        assertEquals(DataType.BYTE, DataType.getType(convertedNumber.getClass()));
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.BYTE);
        assertEquals((byte) -123, convertedNumber);
        assertEquals(DataType.BYTE, DataType.getType(convertedNumber.getClass()));
    }

    @Test
    public void testConvertToSHORT() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.SHORT);
        assertEquals(DataType.SHORT, DataType.getType(convertedNumber.getClass()));
        assertEquals((short) 12, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.SHORT);
        assertEquals(DataType.SHORT, DataType.getType(convertedNumber.getClass()));
        assertEquals((short) -123, convertedNumber);
    }

    @Test
    public void testConvertToINT() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.INT);
        assertEquals(DataType.INT, DataType.getType(convertedNumber.getClass()));
        assertEquals(12, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.INT);
        assertEquals(DataType.INT, DataType.getType(convertedNumber.getClass()));
        assertEquals(-123, convertedNumber);
    }

    @Test
    public void testConvertToLONG() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.LONG);
        assertEquals(DataType.LONG, DataType.getType(convertedNumber.getClass()));
        assertEquals(12L, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.LONG);
        assertEquals(DataType.LONG, DataType.getType(convertedNumber.getClass()));
        assertEquals(-123L, convertedNumber);
    }

    @Test
    public void testConvertToFLOAT() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.FLOAT);
        assertEquals(DataType.FLOAT, DataType.getType(convertedNumber.getClass()));
        assertEquals(12.3f, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.FLOAT);
        assertEquals(DataType.FLOAT, DataType.getType(convertedNumber.getClass()));
        assertEquals(-123f, convertedNumber);
    }

    @Test
    public void testConvertToDOUBLE() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.DOUBLE);
        assertEquals(DataType.DOUBLE, DataType.getType(convertedNumber.getClass()));
        assertEquals(12.3, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.DOUBLE);
        assertEquals(DataType.DOUBLE, DataType.getType(convertedNumber.getClass()));
        assertEquals(-123.0, convertedNumber);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertToWithIllegalArgument() {
        DataTypeUtils.convertTo(12.3, DataType.STRING);
    }
}
