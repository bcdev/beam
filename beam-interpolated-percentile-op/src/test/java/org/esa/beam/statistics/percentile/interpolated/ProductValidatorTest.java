package org.esa.beam.statistics.percentile.interpolated;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.*;
import org.junit.runner.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Utils.class)
public class ProductValidatorTest {

    private BandConfiguration _bandConfiguration;
    private ProductData.UTC _timeRangeStart;
    private ProductData.UTC _timeRangeEnd;
    private Logger S_logger;
    private ProductValidator _productValidator;
    private Product M_product;
    private GeoCoding M_geoCoding;
    private Area _nonIntersectingArea;

    @Before
    public void setUp() throws Exception {
        _bandConfiguration = new BandConfiguration();
        _bandConfiguration.sourceBandName = "sbn";
        ArrayList<BandConfiguration> bandConfigurations = new ArrayList<BandConfiguration>();
        bandConfigurations.add(_bandConfiguration);

        _timeRangeStart = ProductData.UTC.parse("2012-05-21 00:00:00", InterpolatedPercentileOp.DATETIME_PATTERN);
        _timeRangeEnd = ProductData.UTC.parse("2012-07-08 00:00:00", InterpolatedPercentileOp.DATETIME_PATTERN);

        Area targetArea = new Area(new Rectangle(9, 51, 20, 15));
        Area intersectingArea = new Area(new Rectangle(3, 45, 20, 15));
        _nonIntersectingArea = new Area(new Rectangle(3, 45, 2, 1));
        PowerMockito.mockStatic(Utils.class);
        PowerMockito.when(Utils.createProductArea(any(Product.class))).thenReturn(intersectingArea);

        final Logger logger = Logger.getAnonymousLogger();
        S_logger = spy(logger);

        _productValidator = new ProductValidator(bandConfigurations, _timeRangeStart, _timeRangeEnd, targetArea, S_logger);

        M_geoCoding = mock(GeoCoding.class);
        when(M_geoCoding.canGetPixelPos()).thenReturn(true);

        final ProductData.UTC productStartTime = ProductData.UTC.parse("2012-05-22 00:00:00", InterpolatedPercentileOp.DATETIME_PATTERN);
        final ProductData.UTC productEndTime = ProductData.UTC.parse("2012-07-07 00:00:00", InterpolatedPercentileOp.DATETIME_PATTERN);

        M_product = mock(Product.class);
        when(M_product.getName()).thenReturn("ProductMock");
        when(M_product.getGeoCoding()).thenReturn(M_geoCoding);
        when(M_product.getStartTime()).thenReturn(productStartTime);
        when(M_product.getEndTime()).thenReturn(productEndTime);
        when(M_product.containsBand(_bandConfiguration.sourceBandName)).thenReturn(true);
    }

    @Test
    public void testValidProduct() {
        boolean result = _productValidator.isValid(M_product);

        assertEquals(true, result);
        verifyNoMoreInteractions(S_logger);
    }

    @Test
    public void testThatVerificationFailsIfProductHasNoGeoCoding() {
        when(M_product.getGeoCoding()).thenReturn(null);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' does not contain a geo coding.");
    }

    @Test
    public void testThatVerificationFailsIfTheGeoCodingCanNotGetPixelPositionFromGeoPos() {
        when(M_geoCoding.canGetPixelPos()).thenReturn(false);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The geo-coding of the product 'ProductMock' can not determine the pixel position from a geodetic position.");
    }

    @Test
    public void testThatVerificationFailsIfTheProductDoesNotContainAStartTime() {
        when(M_product.getStartTime()).thenReturn(null);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' must contain start and end time.");
    }

    @Test
    public void testThatVerificationFailsIfTheProductDoesNotContainAnEndTime() {
        when(M_product.getEndTime()).thenReturn(null);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' must contain start and end time.");
    }

    @Test
    public void testThatVerificationFailsIfTheProductCanNotHandleTheBandConfiguration() {
        when(M_product.containsBand(_bandConfiguration.sourceBandName)).thenReturn(false);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' does not contain the band 'sbn'.");
    }

    @Test
    public void testThatVerificationFailsIfTheProductStartsBeforeTimeRange() {
        final long timeRangeStartTime = _timeRangeStart.getAsDate().getTime();
        final Date beforeTime = new Date(timeRangeStartTime - 1);
        when(M_product.getStartTime()).thenReturn(ProductData.UTC.create(beforeTime, 0));

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' is not inside the date range from 21-MAY-2012 00:00:00.000000 to 08-JUL-2012 00:00:00.000000");
    }

    @Test
    public void testThatVerificationFailsIfTheProductEndsAfterTimeRange() {
        final long timeRangeEndTime = _timeRangeEnd.getAsDate().getTime();
        final Date afterTime = new Date(timeRangeEndTime + 1000);
        when(M_product.getEndTime()).thenReturn(ProductData.UTC.create(afterTime, 0));

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' is not inside the date range from 21-MAY-2012 00:00:00.000000 to 08-JUL-2012 00:00:00.000000");
    }

    @Test
    public void testThatVerificationFailsIfTheProductDoesNotIntersectTheTargetArea() {
        PowerMockito.when(Utils.createProductArea(any(Product.class))).thenReturn(_nonIntersectingArea);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' does not intersect the target product.");
    }
}
