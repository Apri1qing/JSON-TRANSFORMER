package cn.april.service;

/**
 * 特殊表达式处理器接口
 * 用于处理各种特殊语法的表达式，如 @time:, @math:, @string: 等
 *
 * @author April
 */
public interface SpecialExpressionProcessor {

    /**
     * 处理表达式
     *
     * @param expression 表达式字符串
     * @param value 输入值
     * @return 处理结果
     */
    Object process(String expression, Object value);
    
    /**
     * 获取处理器类型
     *
     * @return 处理器类型标识
     */
    String getType();
    
    /**
     * 获取处理器描述
     *
     * @return 处理器描述信息
     */
    String getDescription();
}
