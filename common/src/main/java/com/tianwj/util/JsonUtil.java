package com.tianwj.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @author tianwj
 * @date 2022/11/28 16:07
 */
public class JsonUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtil.class);

    private static ObjectMapper mapper;

    static {
        mapper = Jackson2ObjectMapperBuilder.json()
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .timeZone("GMT+8")
                // 允许转义
                .featuresToEnable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
                .build();
        // 序列化结果不包含为null的字段
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * 对象 转为 json
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public static String object2Json(Object obj) {
        if (obj == null) {
            return null;
        }
        Writer strWriter = new StringWriter();
        try {
            mapper.writeValue(strWriter, obj);
            return strWriter.toString();
        } catch (Exception e) {
            LOGGER.error("Java对象转为json失败: message={} obj={}", e.getMessage(), obj, e);
            return null;
        }
    }

    /**
     * json 转为 对象
     *
     * @param json
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T json2Object(String json, Class<T> tClass) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return mapper.readValue(json, tClass);
        } catch (Exception e) {
            LOGGER.error("json转为Java对象失败: message={} json={} tClass={}", e.getMessage(), json, tClass, e);
            return null;
        }
    }

    public static <T> T json2Object(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return mapper.readValue(json, typeReference);
        } catch (Exception e) {
            LOGGER.error("json转为Java对象失败: message={} json={} typeReference={}", e.getMessage(), json, typeReference, e);
            return null;
        }
    }

    public static <T> T json2Object(String json, Type type) {
        try {
            JavaType javaType = mapper.getTypeFactory().constructType(type);
            return mapper.readValue(json, javaType);
        } catch (Exception e) {
            LOGGER.error("json转化对象失败: json={}, type={}", json, type, e);
            throw new RuntimeException("json转化对象失败", e);
        }
    }

    /**
     * 对象转换 type1 -> type2，等价于 type1 -> json -> type2
     *
     * @param fromValue   数据源对象
     * @param toValueType 目标对象类型
     * @return 目标对象或null
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        if (fromValue == null) {
            return null;
        }
        try {
            return mapper.convertValue(fromValue, toValueType);
        } catch (Exception e) {
            LOGGER.error("convertValue error: fromValue={} toValueType={}", fromValue, toValueType, e);
            return null;
        }
    }
}
