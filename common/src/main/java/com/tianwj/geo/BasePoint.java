package com.tianwj.geo;

import com.tianwj.util.GeoUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

/**
 * 用于表示一个经纬度坐标点
 *
 * @author tianwj
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BasePoint {

    private static final Double MAX_LON = 180d;

    private static final Double MIN_LON = -180d;

    private static final Double MAX_LAT = 90d;

    private static final Double MIN_LAT = -90d;

    /**
     * 经度
     */
    private Double lon;

    /**
     * 纬度
     */
    private Double lat;

    /**
     * 根据es string格式的geo_point获取BasePoint对象
     *
     * @param esString es string格式的geo_point
     * @return BasePoint对象或null
     */
    public static BasePoint fromEsString(String esString) {
        String[] parts = esString.split(",");
        Double lon = Double.parseDouble(parts[1]);
        Double lat = Double.parseDouble(parts[0]);

        if (!isValidLat(lat)) {
            throw new RuntimeException(String.format("lat error val=%f，expect[-90,90]", lat));
        }

        if (!isValidLon(lon)) {
            throw new RuntimeException(String.format("lon error val=%f，expect[-180,180]", lon));
        }

        return new BasePoint(lon, lat);
    }

    public static boolean isValidLon(Double lon) {

        return lon <= MAX_LON && lon >= MIN_LON;
    }

    public static boolean isValidLat(Double lat) {

        return lat <= MAX_LAT && lat >= MIN_LAT;
    }

    /**
     * 根据BasePoint对象获取es string格式的geo_point
     *
     * @param point BasePoint对象
     * @return es string格式的geo_point或null
     */
    public static String toEsString(BasePoint point) {
        try {
            return point.getLat() + "," + point.getLon();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据经纬度坐标获取es string格式的geo_point
     *
     * @param lon 经度
     * @param lat 纬度
     * @return es string格式的geo_point或null
     */
    public static String toEsString(double lon, double lat) {
        try {
            return lat + "," + lon;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 转化为geoJson格式
     */
    public String toGeoJson() {
        return GeoUtil.toGeoJson(GeoUtil.toJts(lon, lat));
    }

    public Point toJts() {
        return GeoUtil.createPoint(lon, lat);
    }
}

