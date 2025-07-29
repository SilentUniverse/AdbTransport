# ADB Transport 语音测试通信协议

## 概述

本文档描述了ADB Transport应用中集成VoiceTestSDK后的语音测试通信协议。该协议支持语音测试的初始化、测试执行、结果查询等功能。

## 语音测试SDK接口

### 核心方法

```java
// 初始化SDK
public void initSDK(Application application, Context context)

// 执行语音测试
public static void startTest(String title, String area)

// 获取测试结果
public static String getAns()

// 检查是否有结果
public static boolean ifRetrunAns()
```

## 通信协议命令

### 1. 语音SDK初始化 (`voice_init`)

**请求：**
```json
{
  "type": "command",
  "id": "init_001",
  "data": "voice_init"
}
```

**响应（成功）：**
```json
{
  "type": "response",
  "id": "init_001",
  "data": "语音测试SDK初始化成功",
  "timestamp": 1640995200000
}
```

**响应（失败）：**
```json
{
  "type": "error",
  "id": "init_001",
  "data": {
    "error": "语音测试SDK初始化失败",
    "category": "VOICE_TEST_ERROR"
  },
  "timestamp": 1640995200000
}
```

### 2. 开始语音测试 (`voice_start_test`)

**请求：**
```json
{
  "type": "command",
  "id": "test_001",
  "data": {
    "command": "voice_start_test",
    "title": "你好，这是语音测试",
    "area": "1"
  }
}
```

**支持的音区类型：**
- `1` - 音区1
- `2` - 音区2
- `3` - 音区3
- `4` - 音区4

**响应：**
```json
{
  "type": "response",
  "id": "test_001",
  "data": {
    "message": "语音测试已开始",
    "title": "你好，这是语音测试",
    "area": "1",
    "status": "testing"
  },
  "timestamp": 1640995200000
}
```

### 3. 检查测试结果 (`voice_check_result`)

**请求：**
```json
{
  "type": "command",
  "id": "check_001",
  "data": "voice_check_result"
}
```

**响应（有结果）：**
```json
{
  "type": "response",
  "id": "check_001",
  "data": {
    "hasResult": true,
    "status": "completed"
  },
  "timestamp": 1640995200000
}
```

**响应（无结果）：**
```json
{
  "type": "response",
  "id": "check_001",
  "data": {
    "hasResult": false,
    "status": "testing"
  },
  "timestamp": 1640995200000
}
```

### 4. 获取测试结果 (`voice_get_result`)

**请求：**
```json
{
  "type": "command",
  "id": "result_001",
  "data": "voice_get_result"
}
```

**响应（有结果）：**
```json
{
  "type": "response",
  "id": "result_001",
  "data": {
    "result": "语音识别成功, 音区2稳定准确, 短句发音清晰, 评分: 89",
    "exeID": "VOICE_TEST_1_1640995200000",
    "status": "completed"
  },
  "timestamp": 1640995200000
}
```

**响应（无结果）：**
```json
{
  "type": "response",
  "id": "result_001",
  "data": {
    "message": "测试结果尚未准备好",
    "status": "testing"
  },
  "timestamp": 1640995200000
}
```

### 5. 查询SDK状态 (`voice_get_status`)

**请求：**
```json
{
  "type": "command",
  "id": "status_001",
  "data": "voice_get_status"
}
```

**响应：**
```json
{
  "type": "response",
  "id": "status_001",
  "data": {
    "initialized": true,
    "hasResult": false,
    "currentExeID": "VOICE_TEST_1_1640995200000",
    "testCount": 5,
    "timestamp": 1640995200000
  },
  "timestamp": 1640995200000
}
```

## 错误处理

### 错误响应格式

```json
{
  "type": "error",
  "id": "request_id",
  "data": {
    "error": "错误描述",
    "category": "VOICE_TEST_ERROR"
  },
  "timestamp": 1640995200000
}
```

### 常见错误

1. **SDK未初始化**
   ```json
   {
     "error": "语音测试SDK未初始化",
     "category": "VOICE_TEST_ERROR"
   }
   ```

2. **SDK初始化失败**
   ```json
   {
     "error": "语音测试SDK初始化失败",
     "category": "VOICE_TEST_ERROR"
   }
   ```

3. **测试启动失败**
   ```json
   {
     "error": "启动语音测试失败: 参数无效",
     "category": "VOICE_TEST_ERROR"
   }
   ```

## 使用流程

### 典型的语音测试流程

1. **初始化SDK**
   ```json
   {"type": "command", "data": "voice_init"}
   ```

2. **开始语音测试**
   ```json
   {
     "type": "command",
     "data": {
       "command": "voice_start_test",
       "title": "测试话术",
       "area": "2"
     }
   }
   ```

3. **轮询检查结果**
   ```json
   {"type": "command", "data": "voice_check_result"}
   ```

4. **获取测试结果**
   ```json
   {"type": "command", "data": "voice_get_result"}
   ```

## 测试结果格式

### 结果字符串格式
```
"测试结果描述, 执行ID"
```

### 结果示例
```
"语音识别成功, 音区2稳定准确, 短句发音清晰, 评分: 89,VOICE_TEST_1_1640995200000"
```

### 结果组成部分
- **基础评价**: 语音识别成功、语音质量良好等
- **音区评价**: 根据指定音区给出的专项评价
- **话术评价**: 根据话术长度和内容的评价
- **评分**: 75-99分的随机评分
- **执行ID**: 唯一标识符，格式为 `VOICE_TEST_{序号}_{时间戳}`

## 注意事项

1. **异步操作**: 语音测试是异步执行的，需要轮询检查结果
2. **状态管理**: 获取结果后会重置结果状态
3. **参数验证**: 确保话术和音区参数的有效性
4. **错误处理**: 妥善处理各种错误情况
5. **资源管理**: 及时释放SDK资源

## 扩展建议

1. **实时进度**: 可以添加测试进度回调
2. **批量测试**: 支持批量语音测试
3. **结果历史**: 保存测试历史记录
4. **音频文件**: 支持音频文件上传测试
