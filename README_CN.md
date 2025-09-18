# JSON-TRANSFORMER

一个功能强大的JSON转换工具，支持复杂的JSON结构转换、字段映射、表达式转换和类型转换。

![image](https://img.shields.io/badge/JDK-17-green)
![image](https://img.shields.io/badge/Owner-April-blue)
## 项目概述

JSON-TRANSFORMER是一个基于Java的JSON转换工具库，主要功能包括：

- **JSON结构转换**：支持对象到对象、数组到数组的转换
- **灵活字段映射**：使用JSONPath语法进行精确的字段映射
- **表达式转换**：支持Groovy表达式和特殊表达式（如时间处理）
- **类型转换**：自动类型转换和验证
- **模板系统**：支持预定义模板和动态模板

## 项目结构

```
JSON-TRANSFORMER/
├── json-transformer-tool/          # 核心工具库
│   ├── src/main/java/cn/april/
│   │   ├── model/                  # 数据模型
│   │   │   ├── TransformConfig.java
│   │   │   └── FieldMapping.java
│   │   └── service/                # 核心服务
│   │       ├── JsonTransformService.java
│   │       ├── JsonPathNavigator.java
│   │       ├── TypeConverter.java
│   │       └── SpecialExpressionManager.java
├── json-transformer-demo/          # 使用示例和测试
└── pom.xml                         # Maven配置
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.april</groupId>
    <artifactId>json-transformer-tool</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 基本使用

```java
import cn.april.model.TransformConfig;
import cn.april.model.FieldMapping;
import cn.april.service.JsonTransformService;
import com.fasterxml.jackson.databind.JsonNode;

// 1. 配置转换规则 读取配置文件
String configPath = "/path/tieba_test_template.json";
String configJson = new String(Files.readAllBytes(Paths.get(configPath)));

// 2. 解析配置
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> configMap = mapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});

// 3. 创建TransformConfig对象
TransformConfig config = createTransformConfig(configMap);

// 4. 创建JsonTransformService实例
JsonTransformService transformer = new JsonTransformService(config);

// 5. 执行转换
String sourceJson = "...";
JsonNode result = transformer.transform(sourceJson);
```

## 核心概念

### TransformConfig 配置结构

`TransformConfig` 是转换配置的核心类，包含以下字段：

```java
public class TransformConfig {
    private String finalJsonTemplate;        // 最终JSON模板（可选）
    private List<FieldMapping> templateMappings;  // 模板字段转换规则（可选）
    private String targetJson;               // 目标对象模板（可选）
    private String targetNodePath;           // 目标节点路径（可选）
    private List<FieldMapping> mappings;    // 字段映射规则（必需）
}
```

**重要说明**：
- `finalJsonTemplate` 和 `targetNodePath` 是**可选字段**，可以为null或空
- `templateMappings` 和 `targetJson` 是**可选字段**，可以为null或空
- `mappings` 是**必需字段**，必须包含至少一个字段映射规则

**转换模式**：
- **有模板模式**：当 `finalJsonTemplate` 不为空时，基于模板进行转换
- **无模板模式**：当 `finalJsonTemplate` 为空时，直接按照 `mappings` 规则转换源数据

### FieldMapping 字段映射

`FieldMapping` 定义单个字段的转换规则：

```java
public class FieldMapping {
    private String sourcePath;           // 源JSON路径（可选）
    private String targetPath;           // 目标JSON路径
    private String transformExpression;   // 转换表达式
    private String targetType;           // 目标类型
}
```

## 详细使用说明

### 0. 配置场景说明

根据不同的转换需求，有不同的配置方式：

#### 场景1：无模板直接转换
```json
{
  "targetJson": "{\"field1\":\"\",\"field2\":0}",
  "mappings": [...]
}
```
- **不设置** `finalJsonTemplate` 和 `targetNodePath`
- 系统直接按照 `mappings` 规则转换源数据
- 输出格式完全由 `targetJson` 和 `mappings` 决定
- 适合：源数据直接转换为目标格式，无需复杂模板结构

**实际配置示例**（数组转换）：
```json
{
  "targetJson": "{\"post_uuid\":\"\",\"user_name\":\"\",\"title\":\"\",\"content\":\"\",\"publish_time\":\"\",\"extras\":{\"note_url\":\"\",\"user_link\":\"\",\"user_avatar\":\"\",\"tieba_name\":\"\",\"tieba_link\":\"\",\"total_replay_num\":0,\"total_replay_page\":0,\"ip_location\":\"\",\"source_keyword\":\"\",\"last_modify_ts\":0}}",
  "mappings": [
    {
      "sourcePath": "$.user_nickname",
      "targetPath": "$.user_name",
      "targetType": "string"
    },
    {
      "sourcePath": "$.title",
      "targetPath": "$.title",
      "targetType": "string"
    }
  ]
}
```

#### 场景2：复杂模板转换（推荐用于对象转换）
```json
{
  "finalJsonTemplate": "{\"header\":{},\"data\":[]}",
  "targetNodePath": "$.data",
  "targetJson": "{\"field1\":\"\",\"field2\":0}",
  "mappings": [...]
}
```
- `finalJsonTemplate`: 定义完整的输出JSON结构
- `targetNodePath`: 指定转换后的数据插入位置
- `targetJson`: 定义单个数据项的结构

### 1. 简单字段映射

最基本的用法是直接映射字段，无需转换：
```json
{
  "sourcePath": "$.user.name",
  "targetPath": "$.username",
  "targetType": "string"
}
```

### 2. 使用转换表达式

支持Groovy表达式和特殊表达式：
```json
// Groovy表达式
{
  "sourcePath": "$.ip_location",
  "targetPath": "$.extras.ip_location",
  "transformExpression": "\"IP:\" + value", // 在IP前添加前缀
  "targetType": "string"
}
```
```json
// 特殊表达式（时间处理）
{
  "sourcePath": "$.last_modify_ts",
  "targetPath": "$.extras.last_modify_ts",
  "transformExpression": "@time:yyyy-MM-dd HH:mm:ss", // 时间格式化
  "targetType": "string"
}
```

### 3. 类型转换

自动类型转换支持：
```json
{
  "sourcePath": "$.total_replay_num",
  "targetPath": "$.extras.total_replay_num",
  "targetType": "int" // 自动转换为整数类型
}
```

### 4. 模板系统

#### 最终JSON模板 (finalJsonTemplate)

定义转换后的整体JSON结构：

```json
{
  "finalJsonTemplate": "{\"custom_data\":{\"channel_id\":1,\"game_id\":5,\"source_id\":9,\"version\":\"5.0.0\"},\"timestamp\":0,\"zone_offset\":8,\"payload\":[{\"data_type\":\"post\",\"data\":[]}]}"
}
```

#### 模板字段转换 (templateMappings)

处理模板中的静态字段：
```json
[
    {
      "targetPath": "$.timestamp",
      "transformExpression": "@time:current", // 设置当前时间戳
      "targetType": "long"
    },
    {
      "targetPath": "$.custom_data.channel_id",
      "transformExpression": "10000", // 设置固定值
      "targetType": "long"
    }
]
```

#### 目标对象模板 (targetJson)

定义单个转换对象的结构：

```json
{
  "targetJson": "{\"post_uuid\":\"\",\"user_name\":\"\",\"title\":\"\",\"content\":\"\",\"publish_time\":\"\",\"extras\":{\"note_url\":\"\",\"user_link\":\"\",\"user_avatar\":\"\",\"tieba_name\":\"\",\"tieba_link\":\"\",\"total_replay_num\":0,\"total_replay_page\":0,\"ip_location\":\"\",\"source_keyword\":\"\",\"last_modify_ts\":0}}"
}
```

#### 目标节点路径 (targetNodePath)

指定转换后的对象插入到模板中的位置：

```java
config.setTargetNodePath("$.payload[0].data");
```

## 完整示例

### 配置示例

```json
{
  "finalJsonTemplate": "{\"custom_data\":{\"channel_id\":1,\"game_id\":5,\"source_id\":9,\"version\":\"5.0.0\"},\"timestamp\":0,\"zone_offset\":8,\"payload\":[{\"data_type\":\"post\",\"data\":[]}]}",
  "templateMappings": [
    {
      "targetPath": "$.timestamp",
      "transformExpression": "@time:current"
    },
    {
      "targetPath": "$.custom_data.channel_id",
      "transformExpression": "10000",
      "targetType": "long"
    }
  ],
  "targetNodePath": "$.payload[0].data",
  "targetJson": "{\"post_uuid\":\"\",\"user_name\":\"\",\"title\":\"\",\"content\":\"\",\"publish_time\":\"\",\"extras\":{\"note_url\":\"\",\"user_link\":\"\",\"user_avatar\":\"\",\"tieba_name\":\"\",\"tieba_link\":\"\",\"total_replay_num\":0,\"total_replay_page\":0,\"ip_location\":\"\",\"source_keyword\":\"\",\"last_modify_ts\":0}}",
  "mappings": [
    {
      "sourcePath": "$.note_id",
      "targetPath": "$.post_uuid"
    },
    {
      "sourcePath": "$.user_nickname",
      "targetPath": "$.user_name"
    },
    {
      "sourcePath": "$.title",
      "targetPath": "$.title"
    },
    {
      "sourcePath": "$.desc",
      "targetPath": "$.content"
    },
    {
      "sourcePath": "$.ip_location",
      "targetPath": "$.extras.ip_location",
      "transformExpression": "\"IP:\" + value"
    },
    {
      "sourcePath": "$.total_replay_num",
      "targetPath": "$.extras.total_replay_num",
      "targetType": "int"
    }
  ]
}
```

### Java代码示例
见json-transformer-demo

## transformExpression
### groovy表达式

支持所有有效的groovy表达式，如
- `"IP:" + value` - 字符串拼接
- `value.toUpperCase()` - 字符串转大写
- `value ? "有值" : "无值"` - 条件判断

### JSONPath支持

在`transformExpression`中，除了可以使用`value`来引用`sourcePath`指定的字段值外，还可以**直接使用JSONPath语法访问源JSON中的任意字段**。

#### 基本用法
示例：在转换ip_location字段时，可以访问源JSON中的其他字段
```json
    {
      "sourcePath": "$.ip_location",
      "targetPath": "$.extras.ip_location",
      "transformExpression": "$.note_id == null ? value : $.note_id"
    }
```

## 时间表达式 (@time:)
同时，为了方便时间类型的表达式，增加了一种特殊的固定时间表达式，以 `@time:` 开头，支持以下格式：

#### 当前时间生成
- `@time:current` - 当前时间戳（毫秒）
- `@time:current:ms` - 当前时间戳（毫秒）
- `@time:current:s` - 当前时间戳（秒）

#### 时间戳格式化
- `@time:yyyy-MM-dd HH:mm:ss` - 格式化为 "2025-01-20 15:30:45"
- `@time:yyyy-MM-dd` - 格式化为 "2025-01-20"
- `@time:HH:mm:ss` - 格式化为 "15:30:45"
- `@time:yyyy年MM月dd日` - 格式化为 "2025年01月20日"
- `@time:MM/dd/yyyy` - 格式化为 "01/20/2025"
- `@time:yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` - ISO 8601格式

**智能时间戳识别**：
- 系统会自动识别时间戳是毫秒级（13位）还是秒级（10位）
- 毫秒级时间戳：> 1000000000000L
- 秒级时间戳：≤ 1000000000000L

**使用示例**：
```json
// 设置当前时间戳
{
  "targetPath": "$.timestamp",
  "transformExpression":  "@time:current" // 生成当前毫秒时间戳
}
```
```json
// 格式化时间戳为可读格式
{
  "sourcePath": "$.last_modify_ts",
  "targetPath":  "$.formatted_time",
  "transformExpression": "@time:yyyy-MM-dd HH:mm:ss", // 格式化时间
  "targetType": "string"
}
```
```json
// 生成秒级时间戳
{
  "targetPath":  "$.create_time",
  "transformExpression": "@time:current:s", //生成当前秒时间戳
  "targetType": "long"
}
```

## 支持的类型转换

- `string` - 字符串类型
- `int` - 整数类型
- `long` - 长整数类型
- `double` - 双精度浮点数
- `float` - 单精度浮点数
- `boolean` - 布尔类型

## 默认值处理

### 有模板模式
在`finalJsonTemplate`中定义的字段值会作为转换后的默认值。

### 无模板模式
在`targetJson`中定义的字段值会作为转换后的默认值。

**重要说明**：
- 有模板模式：默认值来自`finalJsonTemplate`
- 无模板模式：默认值来自`targetJson`
- 两种模式都支持通过`mappings`规则覆盖默认值


## 性能特性

- **预编译**：所有JSONPath和表达式在初始化时预编译
- **缓存**：编译后的表达式缓存，避免重复编译
- **并发安全**：使用ConcurrentHashMap保证线程安全

## 注意事项

1. **JSONPath语法**：使用标准的JSONPath语法，如 `$.field.subfield[0]`
2. **表达式安全**：Groovy表达式在安全环境中执行
3. **错误处理**：转换失败时会记录警告日志，不会中断整个转换过程
4. **内存管理**：大量数据转换时注意内存使用

## 构建和运行
### Maven构建

```bash
mvn clean install
```

### 运行示例

```bash
cd json-transformer-demo
mvn exec:java -Dexec.mainClass="cn.april.JsonTransformerDemo"
```

## 许可证

本项目采用开源许可证，具体请查看LICENSE文件。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。
