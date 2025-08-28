package cn.april.service;

/**
 * 类型转换策略接口
 * 
 * @author April
 */
public interface TypeConverter {
    
    /**
     * 检查是否支持该类型转换
     */
    boolean supports(String targetType);
    
    /**
     * 执行类型转换
     */
    Object convert(Object value) throws Exception;
    
    /**
     * 获取转换器名称
     */
    String getName();
}
