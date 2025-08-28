package cn.april.service.converters;

import cn.april.service.TypeConverter;

/**
 * 长整数类型转换器
 * 
 * @author April
 */
public class LongTypeConverter implements TypeConverter {
    
    @Override
    public boolean supports(String targetType) {
        return "long".equalsIgnoreCase(targetType);
    }
    
    @Override
    public Object convert(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to long");
    }
    
    @Override
    public String getName() {
        return "LongTypeConverter";
    }
}
