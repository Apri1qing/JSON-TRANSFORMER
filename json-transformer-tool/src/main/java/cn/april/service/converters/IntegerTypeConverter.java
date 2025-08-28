package cn.april.service.converters;

import cn.april.service.TypeConverter;

/**
 * 整数类型转换器
 * 
 * @author April
 */
public class IntegerTypeConverter implements TypeConverter {
    
    @Override
    public boolean supports(String targetType) {
        return "int".equalsIgnoreCase(targetType) || "integer".equalsIgnoreCase(targetType);
    }
    
    @Override
    public Object convert(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to int");
    }
    
    @Override
    public String getName() {
        return "IntegerTypeConverter";
    }
}
