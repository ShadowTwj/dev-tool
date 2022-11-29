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
import org.locationtech.jts.geom.MultiPolygon;

/**
 * 用于表示一个支持空洞的单多边形
 *
 * @author tianwj
 */
@Builder
@AllArgsConstructor
@Data
public class BaseMultiPolygon {

    /**
     * 有一个或多个图形组成
     * 每一个图形由坐标点组成的闭合环列表，有一个或多个环组成。第一个环表示单个多边形，后面的环表示多边形的空洞
     */
    private List<List<List<BasePoint>>> basePoints;

    public BaseMultiPolygon() {
        basePoints = Lists.newArrayList();
    }

    public static BaseMultiPolygon fromQuadraList(List<List<List<List<Double>>>> quadraList) {
        if (quadraList == null) {
            return null;
        }
        List<List<List<BasePoint>>> basePoints = quadraList.stream()
                .map(outerList -> outerList.stream()
                        .map(middleList -> middleList.stream()
                                .map(innerList -> new BasePoint(innerList.get(0), innerList.get(1)))
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
        return new BaseMultiPolygon(basePoints);
    }

    public static BaseMultiPolygon fromEsCoordinate(List<List<Coordinate>> coordinateList) {
        if (coordinateList == null) {
            return null;
        }
        List<List<List<BasePoint>>> basePoints = coordinateList.stream()
                .map(innerList -> Collections.singletonList(innerList.stream()
                        .map(coordinate -> new BasePoint(coordinate.x, coordinate.y))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
        return new BaseMultiPolygon(basePoints);
    }

    public static BaseMultiPolygon fromMultiPolygon(MultiPolygon multiPolygon) {
        return GeoUtil.convertBaseMultiPolygon(multiPolygon);
    }

    public static BaseMultiPolygon fromBasePolygon(BasePolygon polygon) {
        if (polygon == null) {
            return null;
        }
        return new BaseMultiPolygon(Collections.singletonList(polygon.getBasePoints()));
    }

    public List<List<Coordinate>> convertEsCoordinate() throws PolygonConvertException {
        return basePoints.stream().map(outerList -> {
            if (outerList.size() > 1) {
                throw new PolygonConvertException("不支持空洞");
            }
            return outerList.get(0).stream().map(basePoint -> new Coordinate(basePoint.getLon(), basePoint.getLat())).collect(Collectors.toList());
        }).collect(Collectors.toList());
    }

    public MultiPolygon toMultiPolygon() throws PolygonConvertException {
        List<List<BasePoint>> multiPointList = basePoints.stream().map(outerList -> {
            if (outerList.size() > 1) {
                throw new PolygonConvertException("不支持空洞");
            }
            return outerList.get(0);
        }).collect(Collectors.toList());
        return GeoUtil.fromMultiPointList(multiPointList);
    }
}
