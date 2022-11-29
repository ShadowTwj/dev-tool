package com.tianwj.geo;

import com.google.common.collect.Lists;
import com.tianwj.exception.PolygonConvertException;
import com.tianwj.util.GeoUtil;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

/**
 * 用于表示一个支持空洞的单多边形
 *
 * @author tianwj
 */
@Builder
@AllArgsConstructor
@Data
public class BasePolygon {

    /**
     * 由坐标点组成的闭合环列表，有一个或多个环组成。第一个环表示单个多边形，后面的环表示多边形的空洞
     */
    private List<List<BasePoint>> basePoints;

    public BasePolygon() {
        basePoints = Lists.newArrayList();
    }

    public static BasePolygon fromTripleList(List<List<List<Double>>> tripleList) {
        if (tripleList == null) {
            return null;
        }
        List<List<BasePoint>> basePoints = tripleList.stream()
                .map(middleList -> middleList.stream()
                        .map(innerList -> new BasePoint(innerList.get(0), innerList.get(1)))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
        return new BasePolygon(basePoints);
    }

    public static BasePolygon fromEsCoordinate(List<Coordinate> coordinateList) {
        if (coordinateList == null) {
            return null;
        }
        List<List<BasePoint>> basePoints = Collections.singletonList(coordinateList.stream()
                .map(coordinate -> new BasePoint(coordinate.x, coordinate.y))
                .collect(Collectors.toList()));
        return new BasePolygon(basePoints);
    }

    public static BasePolygon fromPolygon(Polygon polygon) {
        return GeoUtil.convertBasePolygon(polygon);
    }

    public List<Coordinate> convertEsCoordinate() throws PolygonConvertException {
        if (basePoints.size() > 1) {
            throw new RuntimeException("不支持空洞");
        }
        return basePoints.get(0).stream().map(basePoint -> new Coordinate(basePoint.getLon(), basePoint.getLat())).collect(Collectors.toList());
    }

    public Polygon toPolygon() throws PolygonConvertException {
        if (basePoints.size() > 1) {
            throw new PolygonConvertException("不支持空洞");
        }
        return GeoUtil.fromPointList(basePoints.get(0));
    }
}
