package cn.april.service;

import cn.april.model.FieldMapping;
import cn.april.model.TransformConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简洁的JSON转换服务
 * 支持对象到对象、对象组到对象组的转换
 *
 * @author April
 */
public class JsonTransformService {

    private static final Logger log = LoggerFactory.getLogger(JsonTransformService.class);
    
    // 常量定义
    private static final String VALUE_PLACEHOLDER = "value";
    private static final String NULL_STRING = "null";
    
    // 核心组件
    private final ObjectMapper objectMapper;
    private final JsonPathNavigator pathNavigator;
    private final TransformConfig transformConfig;
    private final GroovyShell groovyShell;
    private final Map<String, Script> compiledExpressions;
    private final SpecialExpressionManager specialExpressionManager;

    /**
     * 构造函数 - 用于项目启动时配置转换规则
     *
     * @param transformConfig 预配置的转换规则
     */
    public JsonTransformService(TransformConfig transformConfig) {
        this.objectMapper = new ObjectMapper();
        this.pathNavigator = new JsonPathNavigator(objectMapper);
        this.transformConfig = transformConfig;
        this.groovyShell = new GroovyShell();
        this.compiledExpressions = new ConcurrentHashMap<>();
        this.specialExpressionManager = new SpecialExpressionManager();
        
        // 注册特殊表达式处理器
        registerSpecialExpressionProcessors();
        // 在初始化时就编译所有配置中的路径和表达式
        precompileAllPaths();
        precompileAllExpressions();
    }
    
    /**
     * 注册特殊表达式处理器
     */
    private void registerSpecialExpressionProcessors() {
        try {
            // 注册时间表达式处理器
            specialExpressionManager.registerProcessor(new TimeExpressionProcessor());
            log.info("已注册的处理器: {}", String.join(", ", specialExpressionManager.getRegisteredProcessors()));
        } catch (Exception e) {
            throw new RuntimeException("特殊表达式处理器注册失败", e);
        }
    }
    
    /**
     * 预编译所有配置中的路径
     */
    private void precompileAllPaths() {
        try {
            compilePathsFromMappings(transformConfig.getTemplateMappings(), true);
            compilePathsFromMappings(transformConfig.getMappings(), false);
            
            // 编译目标数组路径
            if (transformConfig.getTargetNodePath() != null) {
                pathNavigator.precompilePath(transformConfig.getTargetNodePath());
            }
            
            log.info("所有配置路径预编译完成");
        } catch (Exception e) {
            throw new RuntimeException("路径预编译失败", e);
        }
    }
    
    /**
     * 编译映射中的路径
     */
    private void compilePathsFromMappings(List<FieldMapping> mappings, boolean isTemplate) {
        if (mappings == null) return;
        
        for (FieldMapping mapping : mappings) {
            if (mapping.getTargetPath() != null) {
                pathNavigator.precompilePath(mapping.getTargetPath());
            }
            if (!isTemplate && mapping.getSourcePath() != null) {
                pathNavigator.precompilePath(mapping.getSourcePath());
            }
        }
    }
    
    /**
     * 预编译所有配置中的Groovy表达式
     */
    private void precompileAllExpressions() {
        try {
            compileExpressionsFromMappings(transformConfig.getTemplateMappings());
            compileExpressionsFromMappings(transformConfig.getMappings());
            
            int specialExpressionCount = countSpecialExpressions();
            log.info("所有表达式预编译完成，Groovy表达式: {} 个，特殊表达式: {} 个", 
                    compiledExpressions.size(), specialExpressionCount);
        } catch (Exception e) {
            throw new RuntimeException("表达式预编译失败", e);
        }
    }
    
    /**
     * 编译映射中的表达式
     */
    private void compileExpressionsFromMappings(List<FieldMapping> mappings) {
        if (mappings == null) return;
        
        for (FieldMapping mapping : mappings) {
            String expression = mapping.getTransformExpression();
            if (expression != null && !specialExpressionManager.isSpecialExpression(expression)) {
                precompileExpression(expression);
            }
        }
    }
    
    /**
     * 统计特殊表达式数量
     */
    private int countSpecialExpressions() {
        return countSpecialExpressionsFromMappings(transformConfig.getTemplateMappings()) +
               countSpecialExpressionsFromMappings(transformConfig.getMappings());
    }
    
    /**
     * 统计映射中的特殊表达式数量
     */
    private int countSpecialExpressionsFromMappings(List<FieldMapping> mappings) {
        if (mappings == null) return 0;
        
        int count = 0;
        for (FieldMapping mapping : mappings) {
            String expression = mapping.getTransformExpression();
            if (expression != null && specialExpressionManager.isSpecialExpression(expression)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 预编译单个Groovy表达式
     */
    private void precompileExpression(String expression) {
        if (compiledExpressions.containsKey(expression)) {
            return;
        }
        
        try {
            Script script = groovyShell.parse(expression);
            compiledExpressions.put(expression, script);
        } catch (Exception e) {
            throw new RuntimeException("表达式预编译失败: " + expression, e);
        }
    }
    
    /**
     * 转换JSON
     */
    public JsonNode transform(String sourceJson) throws JsonProcessingException {
        // 1. 解析源JSON
        JsonNode sourceData = objectMapper.readTree(sourceJson);
        
        // 2. 检查是否有finalJsonTemplate
        if (transformConfig.getFinalJsonTemplate() != null && !transformConfig.getFinalJsonTemplate().trim().isEmpty()) {
            // 有模板的情况：基于模板进行转换
            return transformWithTemplate(sourceData);
        } else {
            // 无模板的情况：直接按照mapping规则转换
            return transformWithoutTemplate(sourceData);
        }
    }
    
    /**
     * 基于模板的转换
     */
    private JsonNode transformWithTemplate(JsonNode sourceData) throws JsonProcessingException {
        // 1. 解析目标模板
        JsonNode template = objectMapper.readTree(transformConfig.getFinalJsonTemplate());
        
        // 2. 创建结果JSON
        JsonNode result = template.deepCopy();

        // 3. 处理模板字段转换（只对对象模板进行）
        if (transformConfig.getTemplateMappings() != null && !transformConfig.getTemplateMappings().isEmpty()) {
            if (result.isObject()) {
                processTemplateMappings((ObjectNode) result, transformConfig);
            } else if (result.isArray()) {
                // 如果是数组模板，对数组中的每个对象元素进行模板字段转换
                processArrayTemplateMappings((ArrayNode) result, transformConfig);
            }
        }

        // 4. 处理源数据到模板
        processSourceDataToTemplate(sourceData, result, transformConfig);

        return result;
    }
    
    /**
     * 无模板的直接转换
     */
    private JsonNode transformWithoutTemplate(JsonNode sourceData) {
        if (sourceData.isArray()) {
            // 源数据是数组：转换每个元素
            List<ObjectNode> transformedObjects = transformArray(sourceData, transformConfig);
            return objectMapper.valueToTree(transformedObjects);
        } else {
            // 源数据是对象：直接转换
            ObjectNode transformed = transformSingleObject(sourceData, transformConfig);
            return transformed != null ? transformed : objectMapper.createObjectNode();
        }
    }

    /**
     * 统一处理源数据到模板的逻辑
     */
    private void processSourceDataToTemplate(JsonNode sourceData, JsonNode result, TransformConfig request) {
        if (sourceData.isArray()) {
            // 源数据是数组
            List<ObjectNode> transformedObjects = transformArray(sourceData, request);
            setValueToPath(result, transformedObjects, request.getTargetNodePath());
        } else {
            // 源数据是对象
            ObjectNode transformed = transformSingleObject(sourceData, request);
            if (transformed != null) {
                setValueToPath(result, transformed, request.getTargetNodePath());
            }
        }
    }
    
    /**
     * 转换数组数据
     */
    private List<ObjectNode> transformArray(JsonNode sourceArray, TransformConfig request) {
        List<ObjectNode> transformedObjects = new ArrayList<>();
        for (JsonNode sourceItem : sourceArray) {
            ObjectNode transformed = transformSingleObject(sourceItem, request);
            if (transformed != null) {
                transformedObjects.add(transformed);
            }
        }
        return transformedObjects;
    }
    
    /**
     * 统一设置值到路径（支持对象和数组模板）
     */
    private void setValueToPath(JsonNode result, Object value, String targetPath) {
        try {
            if (targetPath == null || targetPath.trim().isEmpty()) {
                log.warn("targetNodePath未配置，跳过设置");
                return;
            }

            if (result.isObject()) {
                setValueToObjectPath((ObjectNode) result, value, targetPath);
            } else if (result.isArray()) {
                setValueToArrayPath(result, value, targetPath);
            }
        } catch (Exception e) {
            log.warn("设置值到路径失败: {}, 错误: {}", targetPath, e.getMessage());
        }
    }
    
    /**
     * 设置值到对象路径
     */
    private void setValueToObjectPath(ObjectNode result, Object value, String targetPath) {
        JsonNode targetNode = pathNavigator.navigateToTarget(result, targetPath);
        String finalField = pathNavigator.getFinalFieldName(targetPath);
        ((ObjectNode) targetNode).set(finalField, objectMapper.valueToTree(value));
    }
    
    /**
     * 设置值到数组路径
     */
    private void setValueToArrayPath(JsonNode result, Object value, String targetPath) {
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set("result", result);
        
        String wrappedPath = "result" + targetPath.substring(1);
        JsonNode targetNode = pathNavigator.navigateToTarget(rootNode, wrappedPath);
        String finalField = pathNavigator.getFinalFieldName(wrappedPath);
        ((ObjectNode) targetNode).set(finalField, objectMapper.valueToTree(value));
    }

    /**
     * 处理模板字段转换
     */
    private void processTemplateMappings(ObjectNode result, TransformConfig request) {
        for (FieldMapping mapping : request.getTemplateMappings()) {
            try {
                String targetPath = mapping.getTargetPath();
                String transformExpression = mapping.getTransformExpression();
                String targetType = mapping.getTargetType();

                if (targetPath == null || transformExpression == null) {
                    continue;
                }

                // 执行转换表达式
                Object finalValue = evaluateExpression(transformExpression, null);

                // 应用类型转换（如果有指定targetType）
                if (targetType != null && !targetType.trim().isEmpty()) {
                    finalValue = convertToTargetType(finalValue, targetType);
                }

                // 根据targetPath设置值
                setValueByNestedPath(result, targetPath, finalValue);

            } catch (Exception e) {
                log.warn("处理模板映射失败: {} -> {}, 错误: {}", mapping.getSourcePath(), mapping.getTargetPath(), e.getMessage());
            }
        }
    }
    
    /**
     * 处理数组模板的字段转换
     */
    private void processArrayTemplateMappings(ArrayNode result, TransformConfig request) {
        result.forEach(element -> {
            if (element.isObject()) {
                processTemplateMappings((ObjectNode) element, request);
            }
        });
    }



    /**
     * 转换单个对象
     */
    private ObjectNode transformSingleObject(JsonNode sourceObject, TransformConfig request) {
        // 如果有targetJson，基于它创建目标对象；否则创建空对象
        ObjectNode transformed;
        if (request.getTargetJson() != null && !request.getTargetJson().trim().isEmpty()) {
            try {
                JsonNode targetTemplate = objectMapper.readTree(request.getTargetJson());
                transformed = targetTemplate.deepCopy();
            } catch (Exception e) {
                log.warn("解析targetJson失败，使用空对象: {}", e.getMessage());
                transformed = objectMapper.createObjectNode();
            }
        } else {
            transformed = objectMapper.createObjectNode();
        }

        for (FieldMapping mapping : request.getMappings()) {
            try {
                String sourcePath = mapping.getSourcePath();
                String targetPath = mapping.getTargetPath();
                String transformExpression = mapping.getTransformExpression();

                if (targetPath == null) {
                    continue;
                }

                // 从源对象获取值或直接生成值
                Object finalValue = null;

                if (sourcePath != null && !sourcePath.trim().isEmpty()) {
                    // 有sourcePath：从源数据获取值
                    JsonNode sourceValue = getValueByPath(sourceObject, sourcePath);
                    // 注意：这里不跳过null值，因为null也是有效值
                    finalValue = sourceValue;
                }

                // 应用转换表达式（必须的）
                if (transformExpression != null && !transformExpression.trim().isEmpty()) {
                    finalValue = evaluateExpression(transformExpression, finalValue);
                } else {
                    // 如果没有transformExpression，必须有sourcePath
                    if (sourcePath == null || sourcePath.trim().isEmpty()) {
                        log.warn("缺少transformExpression，跳过映射: {}", targetPath);
                        continue;
                    }
                }

                // 应用类型转换（如果有指定targetType）
                if (mapping.getTargetType() != null && !mapping.getTargetType().trim().isEmpty()) {
                    finalValue = convertToTargetType(finalValue, mapping.getTargetType());
                }

                // 根据targetPath创建嵌套结构并设置值
                setValueByNestedPath(transformed, targetPath, finalValue);

            } catch (Exception e) {
                log.warn("处理映射失败: {} -> {}, 错误: {}",
                        mapping.getSourcePath(), mapping.getTargetPath(), e.getMessage());
            }
        }

        return transformed;
    }
    


    /**
     * 从源对象获取值
     */
    private JsonNode getValueByPath(JsonNode source, String path) {
        try {
            // 使用JsonPathNavigator读取值
            Object value = pathNavigator.readValue(source, path);
            if (value instanceof JsonNode) {
                return (JsonNode) value;
            } else if (value != null) {
                // 如果不是JsonNode，转换为JsonNode
                return objectMapper.valueToTree(value);
            }
            return null;
        } catch (Exception e) {
            log.warn("获取值失败: {}, 错误: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 提取值并转换为合适的字符串形式（用于Groovy表达式）
     */
    private String extractValueAsString(Object value) {
        if (value == null) {
            return NULL_STRING;
        }
        
        if (value instanceof JsonNode) {
            return extractJsonNodeValue((JsonNode) value);
        }
        
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        
        return value.toString();
    }
    
    /**
     * 提取JsonNode的值
     */
    private String extractJsonNodeValue(JsonNode node) {
        if (node.isTextual()) {
            return "\"" + node.asText() + "\"";
        }
        if (node.isNumber()) {
            return node.numberValue().toString();
        }
        if (node.isBoolean()) {
            return String.valueOf(node.asBoolean());
        }
        if (node.isNull()) {
            return NULL_STRING;
        }
        return node.toString();
    }

    /**
     * 执行Groovy表达式
     */
    private Object evaluateExpression(String expression, Object value) {
        try {
            // 如果没有表达式，返回原值
            if (expression == null || expression.trim().isEmpty()) {
                return value;
            }

            // 检查是否是特殊表达式
            Object specialResult = specialExpressionManager.process(expression, value);
            if (specialResult != null) {
                return specialResult;
            }

            // 原有的 Groovy 表达式处理逻辑
            Script script = compiledExpressions.get(expression);
            if (script == null) {
                log.warn("表达式未预编译，动态编译: {}", expression);
                script = groovyShell.parse(expression);
                compiledExpressions.put(expression, script);
            }

            // 如果有value，将其嵌入到表达式中
            String finalExpression;
            if (value != null) {
                String valueStr = extractValueAsString(value);
                finalExpression = expression.replace(VALUE_PLACEHOLDER, valueStr);
            } else {
                // 如果value是null，将"value"替换为"null"
                finalExpression = expression.replace(VALUE_PLACEHOLDER, NULL_STRING);
            }

            // 执行预编译的表达式
            Object result = groovyShell.evaluate(finalExpression);
            return result;

        } catch (Exception e) {
            log.warn("表达式执行失败: {}, 错误: {}", expression, e.getMessage());
            return value;
        }
    }

    /**
     * 根据嵌套路径设置值（使用JsonPathNavigator）
     */
    private void setValueByNestedPath(ObjectNode target, String targetPath, Object value) {
        try {
            // 使用JsonPathNavigator导航到目标位置
            JsonNode targetNode = pathNavigator.navigateToTarget(target, targetPath);
            String finalField = pathNavigator.getFinalFieldName(targetPath);

            // 设置最终值
            ((ObjectNode) targetNode).set(finalField, objectMapper.valueToTree(value));
        } catch (Exception e) {
            log.warn("设置嵌套值失败: {}, 错误: {}", targetPath, e.getMessage());
        }
    }

    /**
     * 类型转换：将源值转换为指定的目标类型
     */
    private Object convertToTargetType(Object sourceValue, String targetType) {
        if (targetType == null || targetType.trim().isEmpty() || sourceValue == null) {
            return sourceValue;
        }

        try {
            Object processedValue = extractJsonNodeValue(sourceValue);
            return TypeConverterFactory.convert(processedValue, targetType);
        } catch (Exception e) {
            log.warn("类型转换异常: {} -> {}, 错误: {}, 保持原值", 
                    sourceValue.getClass().getSimpleName(), targetType, e.getMessage());
            return sourceValue;
        }
    }
    
    /**
     * 提取JsonNode的实际值
     */
    private Object extractJsonNodeValue(Object sourceValue) {
        if (!(sourceValue instanceof JsonNode)) {
            return sourceValue;
        }
        
        JsonNode jsonNode = (JsonNode) sourceValue;
        if (jsonNode.isNull()) {
            return null;
        }
        if (jsonNode.isTextual()) {
            return jsonNode.asText();
        }
        if (jsonNode.isNumber()) {
            return jsonNode.numberValue();
        }
        if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        }
        return jsonNode.asText();
    }
}
