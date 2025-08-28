package cn.april.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {
    private String sourcePath;      // 源JSON中的JSONPath（可选）
    private String targetPath;      // 目标JSON中的JSONPath
    private String transformExpression;
    private String targetType;      // 目标字段类型（string, int, long, double, boolean等）
}
