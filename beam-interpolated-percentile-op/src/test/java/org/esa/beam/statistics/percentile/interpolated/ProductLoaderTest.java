package org.esa.beam.statistics.percentile.interpolated;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.WildcardMatcher;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProductIO.class, WildcardMatcher.class})
public class ProductLoaderTest {

    private final static String PATH_1 = "product path 1";
    private final static String PATH_2 = "product path 2";
    private final static File FILE_1 = new File(".", "product path 1");
    private final static File FILE_2 = new File(".", "product path 2");

    private Logger M_logger;
    private ProductLoader productLoader;
    private Product product1;
    private Product product2;
    private ProductValidator M_productValidator;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(WildcardMatcher.class);
        PowerMockito.when(WildcardMatcher.glob(PATH_1)).thenReturn(new File[]{FILE_1});
        PowerMockito.when(WildcardMatcher.glob(PATH_2)).thenReturn(new File[]{FILE_2});

        product1 = mock(Product.class);
        product2 = mock(Product.class);

        PowerMockito.mockStatic(ProductIO.class);
        PowerMockito.when(ProductIO.readProduct(FILE_1)).thenReturn(product1);
        PowerMockito.when(ProductIO.readProduct(FILE_2)).thenReturn(product2);


        String[] paths = {PATH_1, PATH_2};
        M_logger = mock(Logger.class);

        M_productValidator = Mockito.mock(ProductValidator.class);
        when(M_productValidator.isValid(product1)).thenReturn(true);
        when(M_productValidator.isValid(product2)).thenReturn(true);

        productLoader = new ProductLoader(paths, M_productValidator, M_logger);
    }

    @Test
    public void testAllWorksFine() {
        Product[] products = productLoader.loadProducts();

        assertNotNull(products);
        assertEquals(2, products.length);
        assertSame(product1, products[0]);
        assertSame(product2, products[1]);
        verifyNoMoreInteractions(product1);
        verifyNoMoreInteractions(product2);
        verifyNoMoreInteractions(M_logger);
    }

    @Test
    public void testAllProductsAreInvalid() {
        when(M_productValidator.isValid(product1)).thenReturn(false);
        when(M_productValidator.isValid(product2)).thenReturn(false);

        Product[] products = productLoader.loadProducts();

        assertNotNull(products);
        assertEquals(0, products.length);
        verify(product1, times(1)).dispose();
        verify(product2, times(1)).dispose();
        verifyNoMoreInteractions(product1);
        verifyNoMoreInteractions(product2);
        verifyNoMoreInteractions(M_logger);
    }

    @Test
    public void testProduct1isInvalid() {
        when(M_productValidator.isValid(product1)).thenReturn(false);

        Product[] products = productLoader.loadProducts();

        assertNotNull(products);
        assertEquals(1, products.length);
        assertSame(product2, products[0]);
        verify(product1, times(1)).dispose();
        verifyNoMoreInteractions(product1);
        verifyNoMoreInteractions(product2);
        verifyNoMoreInteractions(M_logger);
    }

    @Test
    public void testInvalidProductWildcard() throws IOException {
        IOException exception = new IOException("message");
        PowerMockito.when(WildcardMatcher.glob(PATH_1)).thenThrow(exception);

        Product[] products = productLoader.loadProducts();

        assertNotNull(products);
        assertEquals(1, products.length);
        assertSame(product2, products[0]);
        verifyNoMoreInteractions(product1);
        verifyNoMoreInteractions(product2);
        verify(M_logger).severe("'" + PATH_1 + "' is not a valid products wildcard path.");
        verify(M_logger, times(1)).log(Level.SEVERE, "message", exception);
        verifyNoMoreInteractions(M_logger);
    }

    @Test
    public void testProductIOException() throws IOException {
        IOException exception = new IOException("message");
        PowerMockito.when(ProductIO.readProduct(FILE_1)).thenThrow(exception);

        Product[] products = productLoader.loadProducts();

        assertNotNull(products);
        assertEquals(1, products.length);
        assertSame(product2, products[0]);
        verifyNoMoreInteractions(product1);
        verifyNoMoreInteractions(product2);
        verify(M_logger).severe("Unable to read product '" + FILE_1.getAbsolutePath() + "'.");
        verify(M_logger, times(1)).log(Level.SEVERE, "message", exception);
        verifyNoMoreInteractions(M_logger);
    }
}
