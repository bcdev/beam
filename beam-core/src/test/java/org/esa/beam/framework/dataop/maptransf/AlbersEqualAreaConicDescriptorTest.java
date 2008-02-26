package org.esa.beam.framework.dataop.maptransf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.param.Parameter;

import java.awt.geom.Point2D;


/**
 * Created by IntelliJ IDEA.
 * User: Kashif
 * Date: Feb 18, 2008
 * Time: 1:42:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class AlbersEqualAreaConicDescriptorTest extends TestCase {
    private AlbersEqualAreaConicDescriptor albersProj;

    public void testGetTypeId() {
        assertEquals("Albers_Equal_Area_Conic", albersProj.getTypeID());
    }

    public void testGetName() {
        assertEquals("Albers Equal Area Conic", albersProj.getName());
    }

    public void testGetMapUnit() {
        assertEquals("meter", albersProj.getMapUnit());
    }

    public void testGetParameters() {
        final Parameter[] parameters = albersProj.getParameters();
        assertEquals(9, parameters.length);

        Parameter current = parameters[0];
        assertEquals("semi_major", current.getName());
        assertEquals(Ellipsoid.WGS_84.getSemiMajor(), (Double) current.getValue(), 1e-8);
        assertEquals("Semi major", current.getProperties().getLabel());
        assertEquals("meter", current.getProperties().getPhysicalUnit());

        current = parameters[1];
        assertEquals("semi_minor", current.getName());
        assertEquals(Ellipsoid.WGS_84.getSemiMinor(), (Double) current.getValue(), 1e-8);
        assertEquals("Semi minor", current.getProperties().getLabel());
        assertEquals("meter", current.getProperties().getPhysicalUnit());

        current = parameters[2];
        assertEquals("latitude_of_origin", current.getName());
        assertEquals(50.0, (Double) current.getValue(), 1e-8);
        assertEquals("Latitude of origin", current.getProperties().getLabel());
        assertEquals("degree", current.getProperties().getPhysicalUnit());

        current = parameters[3];
        assertEquals("central_meridian", current.getName());
        assertEquals(99.0, (Double) current.getValue(), 1e-8);
        assertEquals("Central meridian", current.getProperties().getLabel());
        assertEquals("degree", current.getProperties().getPhysicalUnit());

        current = parameters[4];
        assertEquals("latitude_of_intersection_1", current.getName());
        assertEquals(56.0, (Double) current.getValue(), 1e-8);
        assertEquals("Latitude of intersection 1", current.getProperties().getLabel());
        assertEquals("degree", current.getProperties().getPhysicalUnit());

        current = parameters[5];
        assertEquals("latitude_of_intersection_2", current.getName());
        assertEquals(73.0, (Double) current.getValue(), 1e-8);
        assertEquals("Latitude of intersection 2", current.getProperties().getLabel());
        assertEquals("degree", current.getProperties().getPhysicalUnit());

        current = parameters[6];
        assertEquals("scale_factor", current.getName());
        assertEquals(1.0, (Double) current.getValue(), 1e-8);
        assertEquals("Scale factor", current.getProperties().getLabel());
        assertEquals("", current.getProperties().getPhysicalUnit());

        current = parameters[7];
        assertEquals("false_easting", current.getName());
        assertEquals(1000000.0, (Double) current.getValue(), 1e-8);
        assertEquals("False easting", current.getProperties().getLabel());
        assertEquals("meter", current.getProperties().getPhysicalUnit());

        current = parameters[8];
        assertEquals("false_northing", current.getName());
        assertEquals(0.0, (Double) current.getValue(), 1e-8);
        assertEquals("False northing", current.getProperties().getLabel());
        assertEquals("meter", current.getProperties().getPhysicalUnit());
    }

    public void testRegisterProjection() {
        albersProj.registerProjections();

        MapProjection projection = MapProjectionRegistry.getProjection(albersProj.getName());
        assertNotNull(projection);
    }

    public void testHasTransformUI() {
        assertTrue(albersProj.hasTransformUI());
    }

    public void testGetMapTransformUI() {
        double[] parameterValues = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        final MapTransformUI ui = albersProj.getTransformUI(albersProj.createTransform(parameterValues));
        assertNotNull(ui);
    }

    // @todo 2 tb/** reanimate test once the projection is performing as expected
//    public void testGetTransform() {
//        double[] parameterValues = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
//        final CartographicMapTransform transform = (CartographicMapTransform) albersProj.createTransform(parameterValues);
//        assertEquals(4, transform.getCentralMeridian(), 1e-8);
//    }

    public void testForwardProjection() {
        double[] parameterValues = new double[]{Ellipsoid.WGS_84.getSemiMajor(),
                Ellipsoid.WGS_84.getSemiMinor(),
                -17.5,
                63.5,
                -32.5,
                -2.5,
                  7,
                0.0,
                0.0};

        final CartographicMapTransform transform = (CartographicMapTransform) albersProj.createTransform(parameterValues);
        // lat long
        GeoPos geoPos = new GeoPos(-17.5f, 63.5f);
        Point2D point2D = transform.forward(geoPos, new Point2D.Double());
        assertEquals(0.0, point2D.getX(), 1e-8);
        assertEquals(0.0, point2D.getY(), 1e-8);

        geoPos = new GeoPos(-16.5f, 63.5f);
        point2D = transform.forward(geoPos, new Point2D.Double());
        assertEquals(0.0, point2D.getX(), 1e-8);
        assertEquals(114525.988258, point2D.getY(), 1e-6);

        geoPos = new GeoPos(-17.5f, 62.5f);
        point2D = transform.forward(geoPos, new Point2D.Double());
        assertEquals(-102607.681947, point2D.getX(), 1e-6);
        assertEquals(-260.225668, point2D.getY(), 1e-6);

        geoPos = new GeoPos(9.2f, -17.55f);
        point2D = transform.forward(geoPos, new Point2D.Double());
        // note: these values are far from the projection center - may be OK to lose abit of precision here.
        // finally we are measuring in meters - so it's 10 cm ;-)  2008-02-20 TB
        assertEquals(-9263556.721358, point2D.getX(), 1e-1);
        assertEquals(1019992.418963, point2D.getY(), 1e-1);
    }

    public void testInverseProjection() {
        double[] parameterValues = new double[]{Ellipsoid.WGS_84.getSemiMajor(),
                Ellipsoid.WGS_84.getSemiMinor(),
                22.6,
                12.4,
                60,
                20, 7,
                0.0,
                0.0};

        final CartographicMapTransform transform = (CartographicMapTransform) albersProj.createTransform(parameterValues);

        Point2D mapPos = new Point2D.Double();
        GeoPos geoPos = new GeoPos();
        mapPos.setLocation(0, 0);
        geoPos = transform.inverse(mapPos, geoPos);
        assertEquals(12.4, geoPos.getLon(), 1e-6);
        assertEquals(22.6, geoPos.getLat(), 1e-6);

        mapPos.setLocation(2000, 0);
        geoPos = transform.inverse(mapPos, geoPos);
        assertEquals(12.419686666666666666666666666667, geoPos.getLon(), 1e-6);
        assertEquals(22.599998055555555555555555555556, geoPos.getLat(), 1e-6);

        mapPos.setLocation(0, 55000);
        geoPos = transform.inverse(mapPos, geoPos);
        assertEquals(12.4, geoPos.getLon(), 1e-6);
        assertEquals(23.090180277777777777777777777778, geoPos.getLat(), 1e-6);

        mapPos.setLocation(100000, 200000);
        geoPos = transform.inverse(mapPos, geoPos);
        assertEquals(13.405169166666666666666666666667, geoPos.getLon(), 1e-6);
        assertEquals(24.372718611111111111111111111111, geoPos.getLat(), 1e-6);
    }

    // @todo test create deep clone
    public void testCreateDeepClone() {

        double[] parameterValues = new double[]{Ellipsoid.WGS_84.getSemiMajor(),
                Ellipsoid.WGS_84.getSemiMinor(),
                22.6,
                12.4,
                60,
                20,
                7,
                0.0,
                0.0};

        final CartographicMapTransform transform = (CartographicMapTransform) albersProj.createTransform(parameterValues);
        MapTransform map = transform.createDeepClone();
        double[] mapValue = map.getParameterValues();
        assertEquals(6378137.0, mapValue[0]);
        assertEquals(6356752.3, mapValue[1]);
        assertEquals(22.6, mapValue[2]);
        assertEquals(12.4, mapValue[3]);
        assertEquals(60.0, mapValue[4]);
        assertEquals(20.0, mapValue[5]);
        assertEquals(7.0, mapValue[6]);
        assertEquals(0.0, mapValue[7]);
        assertEquals(0.0, mapValue[8]);


    }
  public void testForward_impl() throws IllegalArgumentException{
           double[] parameterValues = new double[]{Ellipsoid.WGS_84.getSemiMajor(),
                Ellipsoid.WGS_84.getSemiMinor(),
                22.6,
                12.4,
                0.0,
                0.0,
                7,
                0.0,
                0.0};

         try
         {
         final CartographicMapTransform transform = (CartographicMapTransform) albersProj.createTransform(parameterValues);
         fail("Invalid parameter set.");
         }
         catch(IllegalArgumentException expected){

         }
   }


    protected void setUp() throws Exception {
        albersProj = new AlbersEqualAreaConicDescriptor();
    }
}
