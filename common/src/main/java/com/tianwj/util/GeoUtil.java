package com.tianwj.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.tianwj.geo.BaseMultiPolygon;
import com.tianwj.geo.BasePoint;
import com.tianwj.geo.BasePolygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.algorithm.Area;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.referencing.operation.TransformException;

/**
 * 通用Geometry格式转换和处理工具
 * 参考：
 * https://github.com/locationtech/jts
 * https://locationtech.github.io/jts/jts-faq.html#robustness
 */
@Slf4j
public class GeoUtil {
    // 默认精度，在赤道上最大精度为11.1mm
    public static final int DEFAULT_DECIMALS = 15;

    // 精度相关
    private static final PrecisionModel PRECISION_MODEL = new PrecisionModel(Math.pow(10, DEFAULT_DECIMALS));
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(PRECISION_MODEL);
    private static final GeometryPrecisionReducer GEOMETRY_PRECISION_REDUCER = new GeometryPrecisionReducer(PRECISION_MODEL);

    // json
    private static final GeoJsonReader GEO_JSON_READER = new GeoJsonReader(GEOMETRY_FACTORY);
    private static final GeoJsonWriter GEO_JSON_WRITER = new GeoJsonWriter(DEFAULT_DECIMALS);

    // wkt
    private static final WKTReader WKT_READER = new WKTReader(GEOMETRY_FACTORY);
    private static final WKTWriter WKT_WRITER = new WKTWriter();

    // wkb
    private static final ThreadLocal<WKBReader> WKB_READER_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<WKBWriter> WKB_WRITER_THREAD_LOCAL = new ThreadLocal<>();

    // 空的geo json集合
    public static final String EMPTY_GEO_COLLECTION_JSON = "{\"type\": \"GeometryCollection\", \"geometries\": []}";

    // 边界重整阈值
    public static final double BUFFER_TOLERANCE = 0.0000_1;

    // MultiPolygon 舍弃面积比例
    public static final double OMIT_TOLERANCE = 0.0001;

    static {
        // 输出结果中不展示坐标系id
        GEO_JSON_WRITER.setEncodeCRS(false);
    }

    public static Point toJts(Double lat, Double lon) {
        Coordinate coordinate = new Coordinate(lon, lat);
        return GEOMETRY_FACTORY.createPoint(coordinate);
    }

    /**
     * 把GeoJson转换为相应的Geometry对象，不会降低Geometry精度
     *
     * @param geoJson GeoJson
     * @return Geometry对象或null
     * @throws NullPointerException 如果参数为null
     */
    public static Geometry fromGeoJson(String geoJson) {
        Preconditions.checkNotNull(geoJson);
        try {
            return GEO_JSON_READER.read(geoJson);
        } catch (ParseException e) {
            log.error("geoJson2Geometry error, input={}", geoJson);
            return null;
        }
    }

    /**
     * 把Geometry对象转换为相应的GeoJson，结果精度会降低到最多7位小数
     *
     * @param geometry Geometry对象
     * @return GeoJson
     * @throws NullPointerException 如果参数为null
     */
    public static String toGeoJson(Geometry geometry) {
        Preconditions.checkNotNull(geometry);
        return GEO_JSON_WRITER.write(geometry);
    }

    /**
     * 把wkt转换为相应的Geometry对象，结果精度会降低到最多8位小数
     * 原因参考：https://github.com/locationtech/jts/issues/708
     *
     * @param wkt WKT
     * @return Geometry对象或null
     * @throws NullPointerException 如果参数为null
     */
    public static Geometry wkt2Geometry(String wkt) {
        Preconditions.checkNotNull(wkt);
        try {
            return WKT_READER.read(wkt);
        } catch (ParseException e) {
            log.error("wkt2Geometry error, input={}", wkt, e);
            return null;
        }
    }

    /**
     * 把Geometry对象转换为相应的wkt，不会降低Geometry精度
     *
     * @param geometry Geometry对象
     * @return WKT
     * @throws NullPointerException 如果参数为null
     */
    public static String geometry2Wkt(Geometry geometry) {
        Preconditions.checkNotNull(geometry);
        return WKT_WRITER.write(geometry);
    }

    /**
     * wkb转换为geometry
     */
    public static Geometry wkb2Geometry(byte[] bytes) {
        Preconditions.checkNotNull(bytes);
        try {
            return getWkbReader().read(bytes);
        } catch (ParseException e) {
            log.error("wkb2Geometry error", e);
            return null;
        }
    }

    /**
     * WKBReader非线程安全，从ThreadLocal中取
     */
    private static WKBReader getWkbReader() {
        WKBReader wkbReader = WKB_READER_THREAD_LOCAL.get();
        if (wkbReader != null) {
            return wkbReader;
        }
        wkbReader = new WKBReader(GEOMETRY_FACTORY);
        WKB_READER_THREAD_LOCAL.set(wkbReader);
        return wkbReader;
    }

    /**
     * geometry转换为wkb
     */
    public static byte[] geometry2wkb(Geometry geometry) {
        Preconditions.checkNotNull(geometry);
        return getWkbWriter().write(geometry);
    }

    /**
     * WKBWriter非线程安全，从ThreadLocal中取
     */
    private static WKBWriter getWkbWriter() {
        WKBWriter wkbWriter = WKB_WRITER_THREAD_LOCAL.get();
        if (wkbWriter != null) {
            return wkbWriter;
        }
        wkbWriter = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN);
        WKB_WRITER_THREAD_LOCAL.set(wkbWriter);
        return wkbWriter;
    }

    /**
     * 返回一个新的Geometry对象，数据精度降低到最多7位小数
     *
     * @param geometry Geometry对象
     * @return 精度降低后的Geometry对象
     * @throws NullPointerException 如果参数为null
     */
    public static Geometry reducePrecision(Geometry geometry) {
        Preconditions.checkNotNull(geometry);
        //return GEOMETRY_PRECISION_REDUCER.reduce(geometry);
        return geometry;
    }

    /**
     * 把一组坐标点转换为相应的Polygon对象
     *
     * @param pointList 坐标点列表
     * @return Polygon对象或null
     * @throws NullPointerException 如果参数为null
     */
    public static Polygon fromPointList(List<BasePoint> pointList) {
        Preconditions.checkNotNull(pointList);
        try {
            return noCheckFromPointList(pointList);
        } catch (Exception e) {
            log.error("fromPointList error, input={}", pointList, e);
            return null;
        }
    }

    /**
     * 把多组坐标点转换为相应的MultiPolygon对象
     *
     * @param multiPointList 多组坐标点列表
     * @return MultiPolygon对象或null
     * @throws NullPointerException 如果参数为null
     */
    public static MultiPolygon fromMultiPointList(List<List<BasePoint>> multiPointList) {
        Preconditions.checkNotNull(multiPointList);
        try {
            Polygon[] polygons = multiPointList.stream().map(pointList -> noCheckFromPointList(pointList)).toArray(Polygon[]::new);
            return GEOMETRY_FACTORY.createMultiPolygon(polygons);
        } catch (Exception e) {
            log.error("fromMultiPointList error, input={}", multiPointList, e);
            return null;
        }
    }

    private static Polygon noCheckFromPointList(List<BasePoint> pointList) {
        Coordinate[] coordinates = pointList.stream().map(point -> new Coordinate(point.getLon(), point.getLat())).toArray(Coordinate[]::new);
        return GEOMETRY_FACTORY.createPolygon(coordinates);
    }

    public static Polygon geoJson2Polygon(String geoJson) {
        Preconditions.checkNotNull(geoJson);

        Geometry geometry = fromGeoJson(geoJson);
        if (geometry == null || !(geometry instanceof Polygon)) {
            return null;
        } else {
            return (Polygon) geometry;
        }
    }

    /**
     * GeoJson转BasePolygon
     */
    public static BasePolygon geoJson2BasePolygon(String geoJson) {

        Polygon polygon = geoJson2Polygon(geoJson);
        return convertBasePolygon(polygon);
    }

    public static String basePolygon2GeoJson(BasePolygon basePolygon) {

        Polygon polygon = basePolygon.toPolygon();
        return toGeoJson(polygon);
    }

    /**
     * JTS Polygon 转 BasePolygon
     *
     * @return {@link BasePolygon}
     */
    public static BasePolygon convertBasePolygon(Polygon polygon) {
        Preconditions.checkNotNull(polygon);

        List<List<BasePoint>> result = Lists.newArrayList();

        //外环处理
        List<BasePoint> pointList =
                Arrays.stream(polygon.getExteriorRing().getCoordinates()).map(it -> new BasePoint(it.getX(), it.getY())).collect(Collectors.toList());
        result.add(pointList);
        //内环处理
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            result.add(Arrays.stream(polygon.getInteriorRingN(i).getCoordinates())
                    .map(it -> new BasePoint(it.getX(), it.getY()))
                    .collect(Collectors.toList()));
        }

        return new BasePolygon(result);
    }

    /**
     * JTS MultiPolygon 转 BaseMultiPolygon
     *
     * @return {@link BaseMultiPolygon}
     */
    public static BaseMultiPolygon convertBaseMultiPolygon(MultiPolygon multiPolygon) {
        Preconditions.checkNotNull(multiPolygon);

        BaseMultiPolygon baseMultiPolygon = new BaseMultiPolygon();
        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
            Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
            BasePolygon basePolygon = convertBasePolygon(polygon);
            baseMultiPolygon.getBasePoints().add(basePolygon.getBasePoints());
        }

        return baseMultiPolygon;
    }

    /**
     * 判断是否是有空洞的Polygon
     *
     * @param geometry 待处理Geometry对象
     * @return 是否有洞
     * @throws NullPointerException 如果参数为null
     */
    public static boolean hasHoles(Geometry geometry) {
        Preconditions.checkNotNull(geometry);
        if (geometry instanceof Polygon) {
            return ((Polygon) geometry).getNumInteriorRing() > 0;
        }
        return false;
    }

    /**
     * 返回polygon图形内部空洞的面积和
     *
     * @param geometry -待处理Geometry对象
     * @return 内部空洞面积和
     */
    public static double getHolesArea(Geometry geometry) {
        Preconditions.checkNotNull(geometry);
        double area = 0.0;
        if (geometry instanceof Polygon) {
            Polygon polygon = ((Polygon) geometry);
            if (polygon.getNumInteriorRing() <= 0) {
                return area;
            }
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                area += Area.ofRing(polygon.getInteriorRingN(i).getCoordinateSequence());
            }
        }
        return area;
    }

    /**
     * 如果指定对象是有空洞的Polygon，则返回一个新的无空洞Polygon，否则返回原对象
     *
     * @param geometry 待处理Geometry对象
     * @return 无空洞Polygon或原对象
     * @throws NullPointerException 如果参数为null
     */
    public static Geometry removeHoles(Geometry geometry) {
        Preconditions.checkNotNull(geometry);
        if (geometry instanceof Polygon) {
            return GEOMETRY_FACTORY.createPolygon(((Polygon) geometry).getExteriorRing());
        }
        return geometry;
    }

    /**
     * 修剪MultiPolygon成Polygon，舍弃 小图形面积/总面积 < OMIT_TOLERANCE
     *
     * @param geo 待修剪图形
     * @return 非MultiPolygon直接返回，MultiPolygon修剪失败则返回null
     */
    public static Geometry trimPolygon(Geometry geo) {
        if (geo instanceof MultiPolygon) {
            List<Geometry> list = new ArrayList<>();
            MultiPolygon multiPolygon = (MultiPolygon) geo;
            double totalSize = geo.getArea();
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                Geometry tmp = multiPolygon.getGeometryN(i);
                if (tmp.getArea() / totalSize >= OMIT_TOLERANCE) {
                    list.add(tmp);
                }
            }
            if (list.size() != 1) {
                return null;
            }
            return list.get(0);
        }
        return geo;
    }

    /**
     * 地理坐标(经纬度GCJ-02)  "lat,lon" 转 BasePoint
     *
     * @return {@link BasePoint}
     */
    public static BasePoint convertBasePoint(String location) {
        Preconditions.checkNotNull(location);

        String[] parts = location.split(",");
        return new BasePoint(Double.parseDouble(parts[1]), Double.parseDouble(parts[0]));
    }

    /**
     * BasePoint 转 String
     */
    public static String convertString(BasePoint point) {
        Preconditions.checkNotNull(point);

        return point.getLat() + "," + point.getLon();
    }

    /**
     * 尝试标准化Geometry对象，边界会有细微调整，用于修复常见边界问题导致的计算异常。
     * 1.Geometry.norm()
     * 2.Geometry.buffer(BUFFER_TOLERANCE).buffer(-BUFFER_TOLERANCE)
     *
     * @param geo 待处理对象
     * @return 处理结果
     */
    public static Geometry normalize(Geometry geo) {
        return geo.norm().buffer(BUFFER_TOLERANCE).buffer(-BUFFER_TOLERANCE);
    }

    /**
     * 判断两个图形是否相交，容许一定的误差
     *
     * @param g1        第一个图形
     * @param g2        第二个图形
     * @param tolerance 容忍误差，相交面积/min(g1面积,g2面积)
     * @return 是否重叠
     */
    public static boolean intersects(Geometry g1, Geometry g2, double tolerance) {
        Geometry geo;
        try {
            geo = g1.intersection(g2);
        } catch (Exception e) {
            geo = normalize(g1).intersection(normalize(g2));
        }
        return geo.getArea() / Math.min(g1.getArea(), g2.getArea()) > tolerance;
    }

    /**
     * 是否重叠，相交面积在误差范围内不算相交
     *
     * @param g1            几何图形
     * @param g2            几何图形
     * @param deviationArea 容忍误差，相交面积
     * @return 是否相交 && 相交面积超出容忍误差
     */
    public static boolean isIntersectionOverDeviation(Geometry g1, Geometry g2, double deviationArea) {
        Geometry geo;
        try {
            geo = g1.intersection(g2);
        } catch (Exception e) {
            geo = normalize(g1).intersection(normalize(g2));
        }
        return geo.getArea() > deviationArea;
    }

    public static double distance(Point p1, Point p2) throws TransformException {
        return JTS.orthodromicDistance(convertCoordinate(p1), convertCoordinate(p2), DefaultGeographicCRS.WGS84);
    }

    private static com.vividsolutions.jts.geom.Coordinate convertCoordinate(Point point) {
        return new com.vividsolutions.jts.geom.Coordinate(point.getX(), point.getY());
    }

    /**
     * 按质心距离分组
     */
    public static List<MultiPoint> groupByCentroidDistance(List<Point> points, int distance) throws TransformException {
        List<List<Point>> groups = new ArrayList<>();

        for (Point point : points) {
            boolean isCreateGroup = true;
            for (List<Point> group : groups) {

                MultiPoint multiPoint = GEOMETRY_FACTORY.createMultiPoint(group.toArray(new Point[0]));
                //获取点集质心
                Point centroid = multiPoint.getCentroid();
                if (distance(centroid, point) <= distance) {
                    isCreateGroup = false;
                    group.add(point);
                }
            }

            if (isCreateGroup) {
                List<Point> group = new ArrayList<>();
                group.add(point);
                groups.add(group);
            }
        }

        return groups.stream().map(group -> GEOMETRY_FACTORY.createMultiPoint(group.toArray(new Point[0]))).collect(Collectors.toList());
    }

    public static List<MultiPoint> groupByDistance(List<Point> points, int distance) throws TransformException {

        List<List<Point>> groups = new ArrayList<>();

        for (Point point : points) {
            boolean isCreateGroup = true;
            for (List<Point> group : groups) {
                double dis = distance(group.get(0), point);
                if (dis <= distance) {
                    isCreateGroup = false;
                    group.add(point);
                }
            }
            if (isCreateGroup) {
                List<Point> group = new ArrayList<>();
                group.add(point);
                groups.add(group);
            }
        }

        return groups.stream().map(group -> GEOMETRY_FACTORY.createMultiPoint(group.toArray(new Point[0]))).collect(Collectors.toList());
    }

    /**
     * 判断第一个图形是否包含第二个图形，容许一定的误差
     *
     * @param big       第一个图形
     * @param small     第二个图形
     * @param tolerance 容忍误差，相交面积/small面积
     * @return 是否包含
     */
    public static boolean covers(Geometry big, Geometry small, double tolerance) {
        Geometry geo;
        try {
            geo = big.intersection(small);
        } catch (Exception e) {
            geo = normalize(big).intersection(normalize(small));
        }
        double p = geo.getArea() / small.getArea();
        return Math.abs(p - 1) < tolerance;
    }

    /**
     * 是否完全包含，在误差范围内算包含
     */
    public static boolean isCoverInDeviation(Geometry big, Geometry small, double deviationArea) {
        Geometry geo;
        try {
            geo = big.intersection(small);
        } catch (Exception e) {
            geo = normalize(big).intersection(normalize(small));
        }
        return Math.abs(geo.getArea() - small.getArea()) <= deviationArea;
    }

    /**
     * 判断第一个图形是否包含第二个图形
     *
     * @param g1 第一个图形
     * @param g2 第二个图形
     * @return 是否包含
     */
    public static boolean contains(Geometry g1, Geometry g2) {
        if (g1 == null || g2 == null) {
            return false;
        }
        GeometryCollection collection;
        if (g1 instanceof GeometryCollection) {
            collection = (GeometryCollection) g1;
        } else {
            collection = createCollection(g1);
        }
        for (int i = 0; i < collection.getNumGeometries(); i++) {
            Geometry tmp = collection.getGeometryN(i);
            if (tmp.contains(g2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 图形放大后再进行union操作，之后再缩小结果
     *
     * @param g1 图形1
     * @param g2 图形2
     * @return 并集
     */
    public static Geometry union(Geometry g1, Geometry g2) {
        Geometry geo = null;
        try {
            geo = g1.union(g2);
        } catch (Exception e) {
            // ignore
        }
        if (geo == null || geo instanceof MultiPolygon) {
            geo = g1.buffer(BUFFER_TOLERANCE).union(g2.buffer(BUFFER_TOLERANCE)).buffer(-BUFFER_TOLERANCE);
        }
        return geo;
    }

    /**
     * 标准化图形后再进行difference操作
     *
     * @param g1 被减数
     * @param g2 减数
     * @return 差
     */
    public static Geometry difference(Geometry g1, Geometry g2) {
        try {
            return g1.difference(g2);
        } catch (Exception e) {
            return normalize(g1).difference(normalize(g2));
        }
    }

    /**
     * 标准化图形后再进行intersection操作
     *
     * @param g1 图形1
     * @param g2 图形2
     * @return 交集
     */
    public static Geometry intersection(Geometry g1, Geometry g2) {
        try {
            return g1.intersection(g2);
        } catch (Exception e) {
            return normalize(g1).intersection(normalize(g2));
        }
    }

    /**
     * 根据坐标数组创建多边形
     * List<Double> 点
     * List<List<Double>> 边
     * List<List<List<Double>>> 如果有洞，需多个边
     *
     * @param coordinates 坐标数组
     * @return 多边形或null
     */
    public static Polygon createPolygon(List<List<List<Double>>> coordinates) {
        if (CollectionUtils.isEmpty(coordinates)) {
            log.warn("createPolygon coordinates is empty");
            return null;
        }
        Coordinate[] list = coordinates.get(0).stream().map(x -> new Coordinate(x.get(0), x.get(1))).toArray(Coordinate[]::new);
        return GEOMETRY_FACTORY.createPolygon(list);
    }

    /**
     * 根据坐标数组创建多边形
     * List<Double> 点
     * List<List<Double>> 边
     * List<List<List<Double>>> 如果有洞，需多个边
     * List<List<List<List<Double>>>> 多个多个边
     *
     * @param coordinates 坐标数组
     * @return 多边形或null
     */
    public static MultiPolygon createMultiPolygon(List<List<List<List<Double>>>> coordinates) {
        if (CollectionUtils.isEmpty(coordinates)) {
            log.warn("createMultiPolygon coordinates is empty");
            return null;
        }

        Polygon[] polygonArr = new Polygon[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            polygonArr[i] = createPolygon(coordinates.get(i));
        }

        MultiPolygon multiPolygon = GEOMETRY_FACTORY.createMultiPolygon(polygonArr);
        return multiPolygon;
    }

    /**
     * 创建图形集合
     *
     * @param geometries 图形列表
     * @return 图形集合
     */
    public static GeometryCollection createCollection(Geometry... geometries) {
        return GEOMETRY_FACTORY.createGeometryCollection(geometries);
    }

    /**
     * 创建Point
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return point
     */
    public static Point createPoint(double longitude, double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    /**
     * 创建MultiPoint
     *
     * @param pointList point集合
     * @return multiPoint
     */
    public static MultiPoint createMultiPoint(List<BasePoint> pointList) {
        if (CollectionUtils.isEmpty(pointList)) {
            log.warn("createMultiPoint pointList is empty");
            return null;
        }
        Point[] points = pointList.stream().map(it -> createPoint(it.getLon(), it.getLat())).toArray(Point[]::new);

        return GEOMETRY_FACTORY.createMultiPoint(points);
    }

    /**
     * 创建Point
     *
     * @param esPoint es geo_point
     * @return point
     */
    public static Point fromEsPoint(String esPoint) {
        try {
            String[] parts = esPoint.split(",");
            return createPoint(Double.parseDouble(parts[1]), Double.parseDouble(parts[0]));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 合并多个Geometry
     */
    public static Geometry merge(List<Geometry> geometryList) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(geometryList));
        return GEOMETRY_FACTORY.buildGeometry(geometryList);
    }

    /**
     * 是否是合法的简单多边形，不包含空洞和异形
     *
     * @param geo 待校验图形
     * @return 是否合法
     */
    public static boolean isValidSimplePolygon(Geometry geo) {
        return geo instanceof Polygon && !hasHoles(geo) && geo.isValid();
    }
}