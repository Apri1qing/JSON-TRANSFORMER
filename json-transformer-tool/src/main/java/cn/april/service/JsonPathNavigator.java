package cn.april.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * JSON路径导航器，使用专业的json-path库
 * 
 * @author April
 */
public class JsonPathNavigator {
    
    private static final Logger log = LoggerFactory.getLogger(JsonPathNavigator.class);
    
    // 常量定义
    private static final String PATH_PREFIX = "$.";
    private static final ConcurrentMap<String, JsonPath> JSON_PATH = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper;

    public JsonPathNavigator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 预编译路径，如果编译失败则抛出异常
     */
    public void precompilePath(String path) {
        try {
            String cleanPath = cleanPath(path);
            JsonPath jsonPath = JsonPath.compile(cleanPath);
            JSON_PATH.put(cleanPath, jsonPath);
            log.debug("路径预编译成功: {}", path);
        } catch (Exception e) {
            log.error("路径预编译失败: {}, 错误: {}", path, e.getMessage());
            throw new RuntimeException("路径预编译失败: " + path, e);
        }
    }
    
    /**
     * 清理路径，去掉$前缀
     */
    private String cleanPath(String path) {
        return path.startsWith(PATH_PREFIX) ? path.substring(PATH_PREFIX.length()) : path;
    }

    /**
     * 导航到目标位置，如果不存在则创建
     */
    public JsonNode navigateToTarget(ObjectNode root, String path) {
        try {
            String cleanPath = cleanPath(path);
            JsonPath jsonPath = JSON_PATH.computeIfAbsent(cleanPath, JsonPath::compile);

            // 尝试读取现有路径
            if (tryReadExistingPath(jsonPath, root)) {
                return (JsonNode) jsonPath.read(root.toString());
            }

            // 创建路径并返回目标节点
            return createPathAndReturnTarget(root, cleanPath);

        } catch (Exception e) {
            log.warn("路径导航失败: {}, 错误: {}", path, e.getMessage());
            return root;
        }
    }
    
    /**
     * 尝试读取现有路径
     */
    private boolean tryReadExistingPath(JsonPath jsonPath, ObjectNode root) {
        try {
            Object existingValue = jsonPath.read(root.toString());
            return existingValue instanceof JsonNode;
        } catch (PathNotFoundException e) {
            log.debug("路径不存在，将创建");
            return false;
        }
    }

    /**
     * 创建路径并返回目标节点
     */
    private JsonNode createPathAndReturnTarget(ObjectNode root, String cleanPath) {
        String[] pathParts = cleanPath.split("\\.");
        ObjectNode current = root;

        // 遍历路径的每一部分（除了最后一部分）
        for (int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            current = navigateToPart(current, part);
        }

        return current;
    }

    /**
     * 导航到路径的单个部分
     */
    private ObjectNode navigateToPart(ObjectNode current, String part) {
        if (part.contains("[")) {
            return navigateToArrayElement(current, part);
        } else {
            return navigateToObjectField(current, part);
        }
    }

    /**
     * 导航到数组元素
     */
    private ObjectNode navigateToArrayElement(ObjectNode current, String part) {
        String fieldName = part.substring(0, part.indexOf("["));
        int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));

        // 确保字段存在且是数组
        if (!current.has(fieldName)) {
            current.set(fieldName, objectMapper.createArrayNode());
        }

        ArrayNode array = (ArrayNode) current.get(fieldName);

        // 确保数组有足够的元素
        while (array.size() <= index) {
            array.add(objectMapper.createObjectNode());
        }

        return (ObjectNode) array.get(index);
    }

    /**
     * 导航到对象字段
     */
    private ObjectNode navigateToObjectField(ObjectNode current, String part) {
        if (!current.has(part)) {
            current.set(part, objectMapper.createObjectNode());
        }
        return (ObjectNode) current.get(part);
    }

    /**
     * 获取路径的最后一部分（字段名）
     */
    public String getFinalFieldName(String path) {
        String cleanPath = cleanPath(path);
        String[] pathParts = cleanPath.split("\\.");
        return pathParts[pathParts.length - 1];
    }

    /**
     * 使用json-path读取值
     */
    public Object readValue(JsonNode root, String path) {
        try {
            String cleanPath = cleanPath(path);
            JsonPath jsonPath = JSON_PATH.computeIfAbsent(cleanPath, JsonPath::compile);
            return jsonPath.read(root.toString());
        } catch (Exception e) {
            log.warn("读取路径失败: {}, 错误: {}", path, e.getMessage());
            return null;
        }
    }
}
