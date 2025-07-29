package com.hys.adbtransport;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * 消息处理器
 * 负责处理JSON消息的解析、响应生成和协议处理
 */
public class MessageHandler {
    private static final String TAG = "MessageHandler";
    private Gson gson;

    private final Map<String, String> pendingOperations = new ConcurrentHashMap<>();

    // 消息类型常量
    public static final String MSG_TYPE_PING = "ping";
    public static final String MSG_TYPE_ECHO = "echo";
    public static final String MSG_TYPE_COMMAND = "command";
    public static final String MSG_TYPE_RESPONSE = "response";
    public static final String MSG_TYPE_ERROR = "error";

    public static final String MSG_TYPE_VOICE_PROGRESS = "voice_progress";
    public static final String MSG_TYPE_VOICE_COMPLETE = "voice_complete";
    
    public MessageHandler() {
        this.gson = new Gson();
    }


    
    /**
     * 处理接收到的消息
     */
    public String handleMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return createErrorResponse("空消息");
        }
        
        try {
            // 尝试解析为JSON
            Message message = gson.fromJson(rawMessage, Message.class);
            return processMessage(message);
            
        } catch (JsonSyntaxException e) {
            // 如果不是JSON格式，作为普通文本处理
            Log.d(TAG, "收到非JSON消息，作为文本处理: " + rawMessage);
            return handleTextMessage(rawMessage);
        }
    }
    
    /**
     * 处理JSON消息
     */
    private String processMessage(Message message) {
        if (message == null || message.type == null) {
            return createErrorResponse("无效的消息格式");
        }
        
        Log.d(TAG, "处理消息类型: " + message.type);
        
        switch (message.type) {
            case MSG_TYPE_PING:
                return createPongResponse(message.id);
                
            case MSG_TYPE_ECHO:
                return createEchoResponse(message.id, message.data);
                
            case MSG_TYPE_COMMAND:
                return handleCommand(message);
                

                
            default:
                return createErrorResponse("未知的消息类型: " + message.type);
        }
    }
    
    /**
     * 处理文本消息
     */
    private String handleTextMessage(String text) {
        // 简单的文本命令处理
        String originalText = text.trim();
        String lowerText = originalText.toLowerCase();

        if (lowerText.equals("ping")) {
            return "pong";
        } else if (lowerText.equals("hello")) {
            return "Hello from Android ADB Server!";
        } else if (lowerText.equals("status")) {
            return "Server is running";
        } else if (lowerText.startsWith("echo ")) {
            return originalText.substring(5); // 返回echo后面的原始内容
        } else {
            return "Unknown command: " + originalText;
        }
    }
    
    /**
     * 处理命令消息
     */
    private String handleCommand(Message message) {
        if (message.data == null) {
            return createErrorResponse("命令数据为空");
        }

        String command;

        // 处理不同类型的命令数据
        if (message.data instanceof String) {
            // 简单字符串命令
            command = (String) message.data;
        } else if (message.data instanceof Map) {
            // 复杂JSON命令，提取command字段
            Map<?, ?> dataMap = (Map<?, ?>) message.data;
            Object commandObj = dataMap.get("command");
            if (commandObj != null) {
                command = commandObj.toString();
            } else {
                return createErrorResponse("JSON命令中缺少command字段");
            }
        } else {
            command = message.data.toString();
        }

        Log.d(TAG, "执行命令: " + command);

        // 这里可以扩展各种命令处理
        switch (command) {
            case "get_device_info":
                return createCommandResponse(message.id, getDeviceInfo());

            case "get_time":
                return createCommandResponse(message.id, System.currentTimeMillis());

            case "test":
                return createCommandResponse(message.id, "Test command executed successfully");

            // 语音测试SDK相关命令
            case "voice_init":
                return handleVoiceInit(message);

            case "voice_start_test":
                return handleVoiceStartTest(message);

            case "voice_get_result":
                return handleVoiceGetResult(message);

            case "voice_check_result":
                return handleVoiceCheckResult(message);

            case "voice_get_status":
                return handleVoiceGetStatus(message);

            default:
                return createErrorResponse("未知命令: " + command);
        }
    }
    
    /**
     * 获取设备信息
     */
    private Map<String, Object> getDeviceInfo() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("model", android.os.Build.MODEL);
        deviceInfo.put("manufacturer", android.os.Build.MANUFACTURER);
        deviceInfo.put("version", android.os.Build.VERSION.RELEASE);
        deviceInfo.put("sdk", android.os.Build.VERSION.SDK_INT);
        deviceInfo.put("timestamp", System.currentTimeMillis());
        return deviceInfo;
    }
    
    /**
     * 创建Pong响应
     */
    public String createPongResponse(String requestId) {
        Message response = new Message();
        response.type = "pong";
        response.id = requestId;
        response.data = "pong";
        response.timestamp = System.currentTimeMillis();
        return gson.toJson(response);
    }
    
    /**
     * 创建Echo响应
     */
    public String createEchoResponse(String requestId, Object data) {
        Message response = new Message();
        response.type = MSG_TYPE_RESPONSE;
        response.id = requestId;
        response.data = data;
        response.timestamp = System.currentTimeMillis();
        return gson.toJson(response);
    }
    
    /**
     * 创建命令响应
     */
    public String createCommandResponse(String requestId, Object result) {
        Message response = new Message();
        response.type = MSG_TYPE_RESPONSE;
        response.id = requestId;
        response.data = result;
        response.timestamp = System.currentTimeMillis();
        return gson.toJson(response);
    }
    
    /**
     * 创建错误响应
     */
    public String createErrorResponse(String error) {
        Message response = new Message();
        response.type = MSG_TYPE_ERROR;
        response.data = error;
        response.timestamp = System.currentTimeMillis();
        return gson.toJson(response);
    }

    
    /**
     * 将对象转换为JSON字符串
     */
    public String objectToJson(Object object) {
        return gson.toJson(object);
    }

    // ========== 语音测试SDK命令处理方法 ==========

    /**
     * 处理语音SDK初始化命令
     */
    private String handleVoiceInit(Message message) {
        try {
            // 注意：实际的SDK初始化应该在MainActivity中完成
            // 这里只是检查SDK是否已经初始化
            if (VoiceTestSDK.isSDKInitialized()) {
                Log.d(TAG, "语音测试SDK已初始化");
                return createCommandResponse(message.id, "语音测试SDK已初始化");
            } else {
                Log.w(TAG, "语音测试SDK未初始化，请在Android端先初始化");
                return createVoiceErrorResponse(message.id, "语音测试SDK未初始化，请在Android端先初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "检查语音测试SDK状态异常: " + e.getMessage());
            return createVoiceErrorResponse(message.id, "检查语音测试SDK状态异常: " + e.getMessage());
        }
    }

    /**
     * 处理语音测试开始命令
     */
    private String handleVoiceStartTest(Message message) {
        if (!VoiceTestSDK.isSDKInitialized()) {
            return createVoiceErrorResponse(message.id, "语音测试SDK未初始化");
        }

        // 解析测试参数
        String title = "默认话术";
        String area = "2";

        if (message.data instanceof Map) {
            Map<?, ?> params = (Map<?, ?>) message.data;
            if (params.containsKey("title")) {
                title = params.get("title").toString();
            }
            if (params.containsKey("area")) {
                area = params.get("area").toString();
            }
        }

        try {
            VoiceTestSDK.startTest(title, area);
            Log.d(TAG, "语音测试已开始 - 话术: " + title + ", 音区: " + area);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "语音测试已开始");
            response.put("title", title);
            response.put("area", area);
            response.put("status", "testing");

            return createCommandResponse(message.id, response);
        } catch (Exception e) {
            Log.e(TAG, "启动语音测试失败: " + e.getMessage());
            return createVoiceErrorResponse(message.id, "启动语音测试失败: " + e.getMessage());
        }
    }

    /**
     * 处理语音测试结果获取命令
     */
    private String handleVoiceGetResult(Message message) {
        if (!VoiceTestSDK.isSDKInitialized()) {
            return createVoiceErrorResponse(message.id, "语音测试SDK未初始化");
        }

        try {
            if (VoiceTestSDK.ifRetrunAns()) {
                String result = VoiceTestSDK.getAns();
                Log.d(TAG, "获取语音测试结果: " + result);

                // 解析结果格式: "结果,执行ID"
                String[] parts = result.split(",", 2);
                Map<String, Object> response = new HashMap<>();
                if (parts.length >= 2) {
                    response.put("result", parts[0]);
                    response.put("exeID", parts[1]);
                } else {
                    response.put("result", result);
                    response.put("exeID", "");
                }
                response.put("status", "completed");

                return createCommandResponse(message.id, response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "测试结果尚未准备好");
                response.put("status", "testing");
                return createCommandResponse(message.id, response);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取语音测试结果失败: " + e.getMessage());
            return createVoiceErrorResponse(message.id, "获取语音测试结果失败: " + e.getMessage());
        }
    }

    /**
     * 处理语音测试结果检查命令
     */
    private String handleVoiceCheckResult(Message message) {
        if (!VoiceTestSDK.isSDKInitialized()) {
            return createVoiceErrorResponse(message.id, "语音测试SDK未初始化");
        }

        try {
            boolean hasResult = VoiceTestSDK.ifRetrunAns();
            Map<String, Object> response = new HashMap<>();
            response.put("hasResult", hasResult);
            response.put("status", hasResult ? "completed" : "testing");

            Log.d(TAG, "检查语音测试结果状态: " + hasResult);
            return createCommandResponse(message.id, response);
        } catch (Exception e) {
            Log.e(TAG, "检查语音测试结果状态失败: " + e.getMessage());
            return createVoiceErrorResponse(message.id, "检查语音测试结果状态失败: " + e.getMessage());
        }
    }

    /**
     * 处理语音测试SDK状态查询命令
     */
    private String handleVoiceGetStatus(Message message) {
        try {
            Map<String, Object> status = VoiceTestSDK.getSDKStatus();
            Log.d(TAG, "获取语音测试SDK状态: " + status);
            return createCommandResponse(message.id, status);
        } catch (Exception e) {
            Log.e(TAG, "获取语音测试SDK状态失败: " + e.getMessage());
            return createVoiceErrorResponse(message.id, "获取语音测试SDK状态失败: " + e.getMessage());
        }
    }

    /**
     * 创建语音测试进度响应
     */
    private String createVoiceProgressResponse(String requestId, String message, int progress) {
        Message response = new Message();
        response.type = MSG_TYPE_VOICE_PROGRESS;
        response.id = requestId;

        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("progress", progress);
        data.put("status", "in_progress");

        response.data = data;
        response.timestamp = System.currentTimeMillis();
        return gson.toJson(response);
    }

    /**
     * 创建语音测试完成响应
     */
    private String createVoiceCompleteResponse(String requestId, Object result) {
        Message response = new Message();
        response.type = MSG_TYPE_VOICE_COMPLETE;
        response.id = requestId;
        response.data = result;
        response.timestamp = System.currentTimeMillis();
        return gson.toJson(response);
    }

    /**
     * 创建语音测试错误响应
     */
    private String createVoiceErrorResponse(String requestId, String error) {
        Message response = new Message();
        response.type = MSG_TYPE_ERROR;
        response.id = requestId;

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", error);
        errorData.put("category", "VOICE_TEST_ERROR");

        response.data = errorData;
        response.timestamp = System.currentTimeMillis();
        return gson.toJson(response);
    }

    /**
     * 获取待处理操作列表
     */
    public Map<String, String> getPendingOperations() {
        return new HashMap<>(pendingOperations);
    }
    
    /**
     * 消息数据类
     */
    public static class Message {
        public String type;      // 消息类型
        public String id;        // 消息ID（用于请求-响应匹配）
        public Object data;      // 消息数据
        public long timestamp;   // 时间戳
        
        public Message() {
            this.timestamp = System.currentTimeMillis();
        }
    }
}
