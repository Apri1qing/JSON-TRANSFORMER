package cn.april;

import cn.april.model.FieldMapping;
import cn.april.model.TransformConfig;
import cn.april.service.JsonTransformService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.util.logging.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON转换器测试类
 * 演示如何使用JsonTransformService作为工具库
 * 
 * @author April
 */
@Slf4j
public class JsonTransformerDemo {
    
    public static void main(String[] args) {
        try {
            // 1. 读取配置文件
            String configPath = "/Users/april/Documents/working/happy-webhook/json-transformer-test/tieba_test_template.json";
            String configJson = new String(Files.readAllBytes(Paths.get(configPath)));

            // 2. 读取测试数据
            String dataPath = "/Users/april/Documents/working/happy-webhook/json-transformer-test/tieba_test_data.json";
            String sourceJson = new String(Files.readAllBytes(Paths.get(dataPath)));

            // 3. 解析配置
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> configMap = mapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
            
            // 检查模板映射是否存在
            Object templateMappingsObj = configMap.get("templateMappings");
            if (templateMappingsObj != null) {
                System.out.println("- 模板映射数量: " + ((List<?>) templateMappingsObj).size());
            } else {
                System.out.println("- 模板映射数量: 0 (未配置)");
            }
            
            // 4. 创建TransformConfig对象
            TransformConfig config = createTransformConfig(configMap);
            
            // 5. 创建JsonTransformService实例
            JsonTransformService transformer = new JsonTransformService(config);
            System.out.println("JsonTransformService创建成功");
            
            // 6. 执行转换
            JsonNode result = transformer.transform(sourceJson);

            // 7. 输出结果
            String resultJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            System.out.println("\n转换结果:" + resultJson);
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从Map创建TransformConfig对象
     */
    private static TransformConfig createTransformConfig(Map<String, Object> configMap) {
        // 创建字段映射列表
        List<Map<String, Object>> mappingsList = (List<Map<String, Object>>) configMap.get("mappings");
        List<FieldMapping> mappings = new ArrayList<>();
        for (Map<String, Object> mappingMap : mappingsList) {
            FieldMapping mapping = new FieldMapping(
                (String) mappingMap.get("sourcePath"),
                (String) mappingMap.get("targetPath"),
                (String) mappingMap.get("transformExpression"),
                (String) mappingMap.get("targetType")
            );
            mappings.add(mapping);
        }
        
        // 创建模板映射列表
        List<FieldMapping> templateMappings = new ArrayList<>();
        Object templateMappingsObj = configMap.get("templateMappings");
        if (templateMappingsObj != null) {
            List<Map<String, Object>> templateMappingsList = (List<Map<String, Object>>) templateMappingsObj;
            for (Map<String, Object> mappingMap : templateMappingsList) {
                FieldMapping mapping = new FieldMapping(
                    (String) mappingMap.get("sourcePath"),
                    (String) mappingMap.get("targetPath"),
                    (String) mappingMap.get("transformExpression"),
                    (String) mappingMap.get("targetType")
                );
                templateMappings.add(mapping);
            }
        }
        
        // 创建TransformConfig对象
        TransformConfig config = new TransformConfig(
            (String) configMap.get("finalJsonTemplate"),
            templateMappings,
            (String) configMap.get("targetJson"),
            (String) configMap.get("targetNodePath"),
            mappings
        );
        
        return config;
    }
}
