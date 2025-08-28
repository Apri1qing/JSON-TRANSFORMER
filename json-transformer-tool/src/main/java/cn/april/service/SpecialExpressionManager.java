package cn.april.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 特殊表达式管理器
 * 负责管理和执行各种特殊表达式处理器
 *
 * @author April
 */
public class SpecialExpressionManager {

    private static final Logger log = LoggerFactory.getLogger(SpecialExpressionManager.class);

    private final List<SpecialExpressionProcessor> processors = new ArrayList<>();
    private final Map<String, SpecialExpressionProcessor> processorMap = new ConcurrentHashMap<>();

    /**
     * 注册特殊表达式处理器
     *
     * @param processor 处理器实例
     */
    public void registerProcessor(SpecialExpressionProcessor processor) {
        if (processor != null) {
            processors.add(processor);
            processorMap.put(processor.getType(), processor);
            log.info("注册特殊表达式处理器: {} - {}", processor.getType(), processor.getDescription());
        }
    }

    /**
     * 处理特殊表达式
     *
     * @param expression 表达式字符串
     * @param value      输入值
     * @return 处理结果，如果不是特殊表达式则返回null
     */
    public Object process(String expression, Object value) {
        if (expression == null) {
            return null;
        }

        // 快速提取表达式类型
        String type = extractExpressionType(expression);
        if (type == null) {
            return null;
        }

        // 直接从 Map 中查找处理器
        SpecialExpressionProcessor processor = processorMap.get(type);
        if (processor != null) {
            try {
                // 对于特殊表达式，传递原始值（去掉可能的引号）
                Object processedValue = processValueForSpecialExpression(value);
                Object result = processor.process(expression, processedValue);
                return result;
            } catch (Exception e) {
                log.warn("特殊表达式处理失败: {}, 处理器: {}, 错误: {}",
                        expression, processor.getType(), e.getMessage());
                return value;
            }
        }

        return null;
    }

    /**
     * 提取表达式的类型
     * 从 @type:command 中提取 type 部分
     */
    private String extractExpressionType(String expression) {
        if (expression == null || !expression.startsWith("@")) {
            return null;
        }

        int colonIndex = expression.indexOf(':');
        return colonIndex == -1 ? null : expression.substring(1, colonIndex);
    }

    /**
     * 为特殊表达式处理值，去掉可能的引号
     */
    private Object processValueForSpecialExpression(Object value) {
        if (value == null) {
            return null;
        }

        // 检查类型
        if (!(value instanceof JsonNode)) {
            return value;
        }

        JsonNode jsonNode = (JsonNode) value;

        if (jsonNode.isTextual()) {
            String strValue = jsonNode.asText();

            // 去掉可能的多层引号
            while (strValue.startsWith("\"") && strValue.endsWith("\"")) {
                strValue = strValue.substring(1, strValue.length() - 1);
            }

            return strValue;
        }

        // 非文本 JsonNode，直接返回
        return jsonNode;
    }

    /**
     * 判断是否是特殊表达式
     *
     * @param expression 表达式字符串
     * @return 是否是特殊表达式
     */
    public boolean isSpecialExpression(String expression) {
        if (expression == null) {
            return false;
        }

        // 快速提取表达式类型并检查是否存在对应的处理器
        String type = extractExpressionType(expression);
        return type != null && processorMap.containsKey(type);
    }

    /**
     * 获取已注册的处理器信息
     *
     * @return 处理器信息列表
     */
    public List<String> getRegisteredProcessors() {
        List<String> info = new ArrayList<>();
        for (SpecialExpressionProcessor processor : processors) {
            info.add(String.format("%s: %s", processor.getType(), processor.getDescription()));
        }
        return info;
    }
}
