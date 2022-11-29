package com.tianwj.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.tianwj.geo.BasePoint;
import com.tianwj.geo.BasePolygon;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Polygon;

/**
 * H3相关工具类
 *
 * @author tianwj
 */
@Slf4j
public class H3Util {

    private H3Util() {
    }

    private static class SingletonHelper {
        private static final H3Core INSTANCE;

        static {
            try {
                INSTANCE = H3Core.newInstance();
            } catch (IOException e) {
                log.error("H3 newInstance error", e);
                throw new RuntimeException(e);
            }
        }
    }

    private static H3Core getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * H3 Code 转 H3 Id
     */
    public static Long code2id(String h3Code) {
        Preconditions.checkArgument(StringUtils.isNotBlank(h3Code));

        return getInstance().stringToH3(h3Code);
    }

    /**
     * H3 Code 转 H3 Id
     */
    public static String id2Code(Long h3Id) {
        Preconditions.checkNotNull(h3Id);

        return getInstance().h3ToString(h3Id);
    }

    /**
     * 查询H3的中心点
     */
    public static BasePoint getCenterPoint(Long h3Id) {
        Preconditions.checkNotNull(h3Id);

        LatLng latLng = getInstance().cellToLatLng(h3Id);
        return BasePoint.builder().lon(latLng.lng).lat(latLng.lat).build();
    }

    /**
     * 根据坐标查询所属H3Id
     */
    public static Long getH3Id(BasePoint point, int h3Res) {
        Preconditions.checkNotNull(point);
        Preconditions.checkNotNull(h3Res);

        return getInstance().latLngToCell(point.getLat(), point.getLon(), h3Res);
    }

    /**
     * H3 转 polygon
     */
    public static Polygon convertPolygon(Long h3Id) {
        Preconditions.checkNotNull(h3Id);

        List<List<List<LatLng>>> multiPolygonPoint = getInstance().cellsToMultiPolygon(Lists.newArrayList(h3Id), true);

        List<List<List<Double>>> coordinates = multiPolygonPoint.get(0).stream().map(it -> {
            List<List<Double>> points = Lists.newArrayList();
            it.forEach(point -> points.add(Lists.newArrayList(point.lng, point.lat)));
            return points;
        }).collect(Collectors.toList());

        return GeoUtil.createPolygon(coordinates);
    }

    /**
     * H3 转 BasePolygon
     */
    public static BasePolygon convertBasePolygon(Long h3Id) {
        Preconditions.checkNotNull(h3Id);

        Polygon polygon = convertPolygon(h3Id);

        return BasePolygon.fromPolygon(polygon);
    }
}
