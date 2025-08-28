package cn.april.service.converters;

import cn.april.service.TypeConverter;

/**
 * 布尔类型转换器
 * 
 * @author April
 */
public class BooleanTypeConverter implements TypeConverter {
    
    @Override
    public boolean supports(String targetType) {
        return "boolean".equalsIgnoreCase(targetType) || "bool".equalsIgnoreCase(targetType);
    }
    
    @Override
    public Object convert(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Boolean) {
            return value;
        } else if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return "true".equals(str) || "1".equals(str) || "yes".equals(str);
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to boolean");
    }
    
    @Override
    public String getName() {
        return "BooleanTypeConverter";
    }
}
