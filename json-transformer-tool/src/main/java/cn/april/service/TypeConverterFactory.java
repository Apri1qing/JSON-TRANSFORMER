package cn.april.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.april.service.converters.BooleanTypeConverter;
import cn.april.service.converters.DoubleTypeConverter;
import cn.april.service.converters.FloatTypeConverter;
import cn.april.service.converters.IntegerTypeConverter;
import cn.april.service.converters.LongTypeConverter;
import cn.april.service.converters.StringTypeConverter;

/**
 * 类型转换器工厂，使用策略模式管理所有类型转换器
 * 
 * @author April
 */
public class TypeConverterFactory {
    
    private static final Logger log = LoggerFactory.getLogger(TypeConverterFactory.class);

    // 转换器注册表 - 使用Map提升查找性能
    private static final Map<String, TypeConverter> CONVERTER_MAP = new ConcurrentHashMap<>();
    
    static {
        // 注册所有内置转换器
        registerConverter(new StringTypeConverter());
        registerConverter(new IntegerTypeConverter());
        registerConverter(new LongTypeConverter());
        registerConverter(new DoubleTypeConverter());
        registerConverter(new FloatTypeConverter());
        registerConverter(new BooleanTypeConverter());
    }
    
    /**
     * 注册转换器
     */
    private static void registerConverter(TypeConverter converter) {
        if (converter != null) {
            // 注册所有支持的类型
            String[] supportedTypes = getSupportedTypes(converter);
            for (String type : supportedTypes) {
                CONVERTER_MAP.put(type, converter);
            }
        }
    }
    
    /**
     * 获取转换器支持的类型列表
     */
    private static String[] getSupportedTypes(TypeConverter converter) {
        if (converter instanceof StringTypeConverter) {
            return new String[]{"string", "str"};
        }
        if (converter instanceof IntegerTypeConverter) {
            return new String[]{"int", "integer"};
        }
        if (converter instanceof LongTypeConverter) {
            return new String[]{"long"};
        }
        if (converter instanceof DoubleTypeConverter) {
            return new String[]{"double"};
        }
        if (converter instanceof FloatTypeConverter) {
            return new String[]{"float"};
        }
        if (converter instanceof BooleanTypeConverter) {
            return new String[]{"boolean", "bool"};
        }
        return new String[]{};
    }

    /**
     * 获取类型转换器
     */
    public static TypeConverter getConverter(String targetType) {
        if (targetType == null || targetType.trim().isEmpty()) {
            return null;
        }
        
        String normalizedType = targetType.toLowerCase().trim();
        return CONVERTER_MAP.get(normalizedType);
    }
    
    /**
     * 执行类型转换
     */
    public static Object convert(Object value, String targetType) {
        try {
            TypeConverter converter = getConverter(targetType);
            if (converter == null) {
                log.warn("未找到类型转换器: {}, 保持原值", targetType);
                return value;
            }
            
            return converter.convert(value);
            
        } catch (Exception e) {
            log.warn("类型转换失败: {} -> {}, 错误: {}, 保持原值", 
                value != null ? value.getClass().getSimpleName() : "null", targetType, e.getMessage());
            return value;
        }
    }
}
