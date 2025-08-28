package cn.april.service.converters;

import cn.april.service.TypeConverter;

/**
 * 单精度浮点数类型转换器
 * 
 * @author April
 */
public class FloatTypeConverter implements TypeConverter {
    
    @Override
    public boolean supports(String targetType) {
        return "float".equalsIgnoreCase(targetType);
    }
    
    @Override
    public Object convert(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            return Float.parseFloat((String) value);
        }
        
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to float");
    }
    
    @Override
    public String getName() {
        return "FloatTypeConverter";
    }
}
