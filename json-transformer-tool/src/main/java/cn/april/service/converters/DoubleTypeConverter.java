package cn.april.service.converters;

import cn.april.service.TypeConverter;

/**
 * 双精度浮点数类型转换器
 * 
 * @author April
 */
public class DoubleTypeConverter implements TypeConverter {
    
    @Override
    public boolean supports(String targetType) {
        return "double".equalsIgnoreCase(targetType);
    }
    
    @Override
    public Object convert(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to double");
    }
    
    @Override
    public String getName() {
        return "DoubleTypeConverter";
    }
}
