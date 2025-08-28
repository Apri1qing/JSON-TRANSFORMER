package cn.april.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {
    // 源JSON中的JSONPath（可选）
    private String sourcePath;
    // 目标JSON中的JSONPath
    private String targetPath;
    // groovy表达式或特殊表达式
    private String transformExpression;
    // 目标字段类型（string, int, long, double, boolean等）
    private String targetType;
}
