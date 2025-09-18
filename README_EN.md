# JSON-TRANSFORMER

A powerful JSON transformation tool that supports complex JSON structure conversion, field mapping, expression transformation, and type conversion.

![image](https://img.shields.io/badge/JDK-17-green)
![image](https://img.shields.io/badge/Owner-April-blue)

## Project Overview

JSON-TRANSFORMER is a Java-based JSON transformation tool library with the following main features:

- **JSON Structure Transformation**: Supports object-to-object and array-to-array conversion
- **Flexible Field Mapping**: Uses JSONPath syntax for precise field mapping
- **Expression Transformation**: Supports Groovy expressions and special expressions (e.g., time handling)
- **Type Conversion**: Automatic type conversion and validation
- **Template System**: Supports predefined templates and dynamic templates

## Project Structure

```
JSON-TRANSFORMER/
├── json-transformer-tool/          # Core tool library
│   ├── src/main/java/cn/april/
│   │   ├── model/                  # Data models
│   │   │   ├── TransformConfig.java
│   │   │   └── FieldMapping.java
│   │   └── service/                # Core services
│   │       ├── JsonTransformService.java
│   │       ├── JsonPathNavigator.java
│   │       ├── TypeConverter.java
│   │       └── SpecialExpressionManager.java
├── json-transformer-demo/          # Usage examples and tests
└── pom.xml                         # Maven configuration
```

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>cn.april</groupId>
    <artifactId>json-transformer-tool</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Basic Usage

```java
import cn.april.model.TransformConfig;
import cn.april.model.FieldMapping;
import cn.april.service.JsonTransformService;
import com.fasterxml.jackson.databind.JsonNode;

// 1. read the config file
String configPath = "/path/tieba_test_template.json";
String configJson = new String(Files.readAllBytes(Paths.get(configPath)));

// 2. parse the config
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> configMap = mapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});

// 3. Create a TransformConfig instance
TransformConfig config = createTransformConfig(configMap);

// 4. Initialize the JsonTransformService
JsonTransformService transformer = new JsonTransformService(config);

// 5. execute
String sourceJson = "...";
JsonNode result = transformer.transform(sourceJson);
```

## Core Concepts

### TransformConfig Configuration Structure

`TransformConfig` is the core class for transformation configuration, containing the following fields:

```java
public class TransformConfig {
    private String finalJsonTemplate;        // Final JSON template (optional)
    private List<FieldMapping> templateMappings;  // Template field transformation rules (optional)
    private String targetJson;               // Target object template (optional)
    private String targetNodePath;           // Target node path (optional)
    private List<FieldMapping> mappings;    // Field mapping rules (required)
}
```

**Important Notes**:
- `finalJsonTemplate` and `targetNodePath` are **optional fields**, can be null or empty
- `templateMappings` and `targetJson` are **optional fields**, can be null or empty
- `mappings` is a **required field**, must contain at least one field mapping rule

**Transformation Modes**:
- **Template Mode**: When `finalJsonTemplate` is not empty, transformation is based on template
- **Non-template Mode**: When `finalJsonTemplate` is empty, transforms source data directly according to `mappings` rules

### FieldMapping Field Mapping

`FieldMapping` defines transformation rules for a single field:

```java
public class FieldMapping {
    private String sourcePath;           // Source JSON path (optional)
    private String targetPath;           // Target JSON path
    private String transformExpression;   // Transformation expression
    private String targetType;           // Target type
}
```

## Detailed Usage Instructions

### 0. Configuration Scenarios

Different transformation needs require different configuration approaches:

#### Scenario 1: Direct Transformation Without Template
```json
{
  "targetJson": "{\"field1\":\"\",\"field2\":0}",
  "mappings": [...]
}
```
- **Do not set** `finalJsonTemplate` and `targetNodePath`
- System directly transforms source data according to `mappings` rules
- Output format is completely determined by `targetJson` and `mappings`
- Suitable for: Direct transformation of source data to target format without complex template structure

**Actual Configuration Example** (Array transformation):
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

#### Scenario 2: Complex Template Transformation (Recommended for object transformation)
```json
{
  "finalJsonTemplate": "{\"header\":{},\"data\":[]}",
  "targetNodePath": "$.data",
  "targetJson": "{\"field1\":\"\",\"field2\":0}",
  "mappings": [...]
}
```
- `finalJsonTemplate`: Defines the complete output JSON structure
- `targetNodePath`: Specifies where to insert the transformed data
- `targetJson`: Defines the structure of individual data items

### 1. Simple Field Mapping

The most basic usage is direct field mapping without transformation:

```json
{
  "sourcePath": "$.user.name",
  "targetPath": "$.username",
  "targetType": "string"
}
```

### 2. Using Transformation Expressions

Supports Groovy expressions and special expressions:
```json
// Groovy表达式
{
  "sourcePath": "$.ip_location",
  "targetPath": "$.extras.ip_location",
  "transformExpression": "\"IP:\" + value", // Add prefix before IP
  "targetType": "string"
}
```
```json
// 特殊表达式（时间处理）
{
  "sourcePath": "$.last_modify_ts",
  "targetPath": "$.extras.last_modify_ts",
  "transformExpression": "@time:yyyy-MM-dd HH:mm:ss", // Time formatting
  "targetType": "string"
}
```

### 3. Type Conversion

Automatic type conversion support:
```json
{
  "sourcePath": "$.total_replay_num",
  "targetPath": "$.extras.total_replay_num",
  "targetType": "int" // Automatically convert to integer type
}
```

### 4. Template System

#### Final JSON Template (finalJsonTemplate)

Defines the overall JSON structure after transformation:

```json
{
  "finalJsonTemplate": "{\"custom_data\":{\"channel_id\":1,\"game_id\":5,\"source_id\":9,\"version\":\"5.0.0\"},\"timestamp\":0,\"zone_offset\":8,\"payload\":[{\"data_type\":\"post\",\"data\":[]}]}"
}
```

#### Template Field Transformation (templateMappings)

Handles static fields in templates:
```json
[
    {
      "targetPath": "$.timestamp",
      "transformExpression": "@time:current", // Set current timestamp
      "targetType": "long"
    },
    {
      "targetPath": "$.custom_data.channel_id",
      "transformExpression": "10000", // Set fixed value
      "targetType": "long"
    }
]
```

#### Target Object Template (targetJson)

Defines the structure of individual transformation objects:

```json
{
  "targetJson": "{\"post_uuid\":\"\",\"user_name\":\"\",\"title\":\"\",\"content\":\"\",\"publish_time\":\"\",\"extras\":{\"note_url\":\"\",\"user_link\":\"\",\"user_avatar\":\"\",\"tieba_name\":\"\",\"tieba_link\":\"\",\"total_replay_num\":0,\"total_replay_page\":0,\"ip_location\":\"\",\"source_keyword\":\"\",\"last_modify_ts\":0}}"
}
```

#### Target Node Path (targetNodePath)

Specifies where to insert the transformed objects in the template:

```java
config.setTargetNodePath("$.payload[0].data");
```

## Complete Example

### Configuration Example

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

### Java Code Example
See json-transformer-demo

## transformExpression
### Groovy Expressions

Supports all valid Groovy expressions, such as:
- `"IP:" + value` - String concatenation
- `value.toUpperCase()` - String to uppercase
- `value ? "Has value" : "No value"` - Conditional judgment

### JSONPath Support

In `transformExpression`, besides using `value` to reference the field value specified by `sourcePath`, you can also **directly use JSONPath syntax to access any field in the source JSON**.

#### Basic Usage

Example: When transforming ip_location field, you can access other fields in source JSON
```json
{
    "sourcePath": "$.ip_location",
        "targetPath": "$.extras.ip_location",
        "transformExpression": "$.note_id == null ? value : $.note_id"
}
```

## Time Expressions (@time:)
Additionally, for convenient time type expressions, a special fixed time expression starting with `@time:` is added, supporting the following formats:

#### Current Time Generation
- `@time:current` - Current timestamp (milliseconds)
- `@time:current:ms` - Current timestamp (milliseconds)
- `@time:current:s` - Current timestamp (seconds)

#### Timestamp Formatting
- `@time:yyyy-MM-dd HH:mm:ss` - Format as "2025-01-20 15:30:45"
- `@time:yyyy-MM-dd` - Format as "2025-01-20"
- `@time:HH:mm:ss` - Format as "15:30:45"
- `@time:yyyy年MM月dd日` - Format as "2025年01月20日"
- `@time:MM/dd/yyyy` - Format as "01/20/2025"
- `@time:yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` - ISO 8601 format

**Intelligent Timestamp Recognition**:
- System automatically recognizes whether timestamp is millisecond-level (13 digits) or second-level (10 digits)
- Millisecond-level timestamp: > 1000000000000L
- Second-level timestamp: ≤ 1000000000000L

**Usage Examples**:
```json
// Set current timestamp
{
  "targetPath": "$.timestamp",
  "transformExpression":  "@time:current" // Generate current millisecond timestamp
}
```
```json
// Format timestamp to readable format
{
  "sourcePath": "$.last_modify_ts",
  "targetPath":  "$.formatted_time",
  "transformExpression": "@time:yyyy-MM-dd HH:mm:ss", // Generate current second timestamp
  "targetType": "string"
}
```
```json
// Generate second-level timestamp
{
  "targetPath":  "$.create_time",
  "transformExpression": "@time:current:s", // Generate current second timestamp
  "targetType": "long"
}
```

## Supported Type Conversions

- `string` - String type
- `int` - Integer type
- `long` - Long integer type
- `double` - Double precision floating point
- `float` - Single precision floating point
- `boolean` - Boolean type

## Default Value Handling

### Template Mode
Field values defined in `finalJsonTemplate` serve as default values after transformation.

### Non-template Mode
Field values defined in `targetJson` serve as default values after transformation.

**Important Notes**:
- Template mode: Default values come from `finalJsonTemplate`
- Non-template mode: Default values come from `targetJson`
- Both modes support overriding default values through `mappings` rules

## Performance Features

- **Pre-compilation**: All JSONPath and expressions are pre-compiled during initialization
- **Caching**: Compiled expressions are cached to avoid repeated compilation
- **Concurrency Safety**: Uses ConcurrentHashMap to ensure thread safety

## Notes

1. **JSONPath Syntax**: Uses standard JSONPath syntax, such as `$.field.subfield[0]`
2. **Expression Safety**: Groovy expressions execute in a secure environment
3. **Error Handling**: Transformation failures are logged as warnings without interrupting the entire transformation process
4. **Memory Management**: Pay attention to memory usage when transforming large amounts of data

## Build and Run
### Maven Build

```bash
mvn clean install
```

### Run Example

```bash
cd json-transformer-demo
mvn exec:java -Dexec.mainClass="cn.april.JsonTransformerDemo"
```

## License

This project uses an open source license. Please see the LICENSE file for details.

## Contributing

Welcome to submit Issues and Pull Requests to improve this project.