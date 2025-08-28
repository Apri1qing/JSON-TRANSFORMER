package cn.april.service.converters;

import cn.april.service.TypeConverter;

/**
 * 字符串类型转换器
 * 
 * @author April
 */
public class StringTypeConverter implements TypeConverter {
    
    @Override
    public boolean supports(String targetType) {
        return "string".equalsIgnoreCase(targetType) || "str".equalsIgnoreCase(targetType);
    }
    
    @Override
    public Object convert(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
    
    @Override
    public String getName() {
        return "StringTypeConverter";
    }
}
