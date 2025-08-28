package cn.april.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间表达式处理器
 * 支持 @time: 开头的各种时间处理表达式
 *
 * @author April
 */
public class TimeExpressionProcessor implements SpecialExpressionProcessor {

    private static final Logger log = LoggerFactory.getLogger(TimeExpressionProcessor.class);
    public static final String TIME = "@time:";

    @Override
    public String getType() {
        return "time";
    }

    @Override
    public String getDescription() {
        return "时间表达式处理器，支持当前时间生成和时间戳格式化";
    }

    @Override
    public Object process(String expression, Object value) {
        try {
            // 去掉 @time: 前缀
            String timeCommand = expression.substring(TIME.length());

            if (timeCommand.startsWith("current")) {
                return processCurrentTime(timeCommand);
            } else {
                // 其他情况都是格式化时间戳
                return formatTimestamp(value, timeCommand);
            }
        } catch (Exception e) {
            log.warn("时间表达式处理失败: {}, 错误: {}", expression, e.getMessage());
            return value;
        }
    }

    /**
     * 处理当前时间生成
     */
    private Object processCurrentTime(String timeCommand) {
        if ("current:ms".equals(timeCommand)) {
            return System.currentTimeMillis();
        }
        if ("current:s".equals(timeCommand)) {
            return System.currentTimeMillis() / 1000;
        }
        return System.currentTimeMillis();
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(Object value, String format) {
        if (value == null) {
            return "";
        }
        
        try {
            String valueStr = value.toString().trim();
            long timestamp = Long.parseLong(valueStr);

            // 智能判断时间戳单位
            if (timestamp > 1000000000000L) {
                // 毫秒级时间戳，直接使用
                return new SimpleDateFormat(format).format(new Date(timestamp));
            } else {
                // 秒级时间戳，转换为毫秒
                return new SimpleDateFormat(format).format(new Date(timestamp * 1000));
            }
        } catch (Exception e) {
            log.warn("时间戳格式化失败: value={}, format={}, 错误: {}", value, format, e.getMessage());
            return value.toString();
        }
    }
}
