package cn.april.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformConfig {
    // 最终JSON模板（包含目标结构）
    private String finalJsonTemplate;
    // 模板字段转换规则（用于finalJsonTemplate中的字段）
    private List<FieldMapping> templateMappings;
    // 目标对象模板（单个转换后对象的结构）
    private String targetJson;
    // 目标对象插入节点路径（如 $.payload[0].data）
    private String targetNodePath;
    // 字段映射规则
    private List<FieldMapping> mappings;
}
