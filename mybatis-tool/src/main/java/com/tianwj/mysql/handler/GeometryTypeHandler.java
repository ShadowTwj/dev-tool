package com.tianwj.mysql.handler;

import com.tianwj.util.GeoUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Geometry;

/**
 * MySQL GeometryTypeHandler
 * <p>
 * mybatis-config.xml add:
 * <code>
 * <typeHandlers>
 * <typeHandler handler="com.tianwj.mysql.handler.GeometryTypeHandler"/>
 * </typeHandlers>
 * </code>
 *
 * @author tianwj
 */
@MappedTypes(Geometry.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class GeometryTypeHandler extends BaseTypeHandler<Geometry> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, Geometry geometry, JdbcType jdbcType) throws SQLException {
        byte[] bytes = GeoUtil.geometry2wkb(geometry);
        byte[] wkb = new byte[bytes.length + 4];
        // srid设置为0
        wkb[0] = wkb[1] = wkb[2] = wkb[3] = 0;
        System.arraycopy(bytes, 0, wkb, 4, bytes.length);
        preparedStatement.setBytes(i, wkb);
    }

    @Override
    public Geometry getNullableResult(ResultSet resultSet, String s) throws SQLException {
        byte[] bytes = resultSet.getBytes(s);
        // 前4个字节代表srid，去掉srid
        byte[] geomBytes = ByteBuffer.allocate(bytes.length - 4).order(ByteOrder.LITTLE_ENDIAN).put(bytes, 4, bytes.length - 4).array();
        return GeoUtil.wkb2Geometry(geomBytes);
    }

    @Override
    public Geometry getNullableResult(ResultSet resultSet, int i) throws SQLException {
        byte[] bytes = resultSet.getBytes(i);
        // 前4个字节代表srid，去掉srid
        byte[] geomBytes = ByteBuffer.allocate(bytes.length - 4).order(ByteOrder.LITTLE_ENDIAN).put(bytes, 4, bytes.length - 4).array();
        return GeoUtil.wkb2Geometry(geomBytes);
    }

    @Override
    public Geometry getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        byte[] bytes = callableStatement.getBytes(i);
        // 前4个字节代表srid，去掉srid
        byte[] geomBytes = ByteBuffer.allocate(bytes.length - 4).order(ByteOrder.LITTLE_ENDIAN).put(bytes, 4, bytes.length - 4).array();
        return GeoUtil.wkb2Geometry(geomBytes);
    }
}

