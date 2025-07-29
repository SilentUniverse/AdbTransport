# ADB Transport - Android语音测试通信应用

这是一个基于ADB端口转发的Android语音测试通信应用，灵感来源于uiautomator2项目。该应用集成了语音测试SDK，可以让PC端通过ADB端口转发与Android设备进行语音测试相关的双向通信。

## 功能特性

- 🚀 TCP服务器：在Android设备上运行TCP服务器
- 🔄 ADB端口转发：支持通过ADB进行PC-Android通信
- 📱 友好界面：简洁的Android UI控制界面
- 📨 消息处理：支持JSON和文本消息格式
- 🔗 连接管理：自动管理客户端连接生命周期
- 📊 实时日志：显示服务器状态和通信日志
- 🎤 语音测试：集成语音测试SDK，支持语音测试功能
- 🎯 测试结果：支持语音测试结果查询和状态监控

## 快速开始

### 1. 安装应用

1. 克隆项目到本地
2. 使用Android Studio打开项目
3. 编译并安装到Android设备

### 2. 启动服务器

1. 打开ADB Transport应用
2. 设置端口号（默认9999）
3. 点击"启动服务器"按钮
4. 确认服务器启动成功

### 3. 设置ADB端口转发

在PC端执行以下命令设置端口转发：

```bash
# 基本语法
adb forward tcp:LOCAL_PORT tcp:DEVICE_PORT

# 示例：将PC的8888端口转发到Android的9999端口
adb forward tcp:8888 tcp:9999
```

### 4. 测试连接

使用telnet或其他工具测试连接：

```bash
# 使用telnet测试
telnet localhost 8888

# 发送测试消息
ping
hello
echo test message
```

## 通信协议

### 支持的消息格式

#### 1. 文本消息
直接发送文本命令：
- `ping` → 返回 `pong`
- `hello` → 返回 `Hello from Android ADB Server!`
- `status` → 返回 `Server is running`
- `echo <message>` → 返回 `<message>`

#### 2. JSON消息
发送JSON格式的结构化消息：

```json
{
  "type": "ping",
  "id": "request_001",
  "data": "ping",
  "timestamp": 1640995200000
}
```

**支持的消息类型：**

- `ping`: 心跳检测
- `echo`: 回显消息
- `command`: 执行命令
- `heartbeat`: 心跳包

**响应格式：**

```json
{
  "type": "response",
  "id": "request_001",
  "data": "响应数据",
  "timestamp": 1640995200000
}
```

### 内置命令

通过`command`类型消息可以执行以下命令：

- `get_device_info`: 获取设备信息
- `get_time`: 获取当前时间戳
- `test`: 测试命令

示例：
```json
{
  "type": "command",
  "id": "cmd_001",
  "data": "get_device_info"
}
```

响应：
```json
{
  "type": "response",
  "id": "cmd_001",
  "data": {
    "model": "Pixel 6",
    "manufacturer": "Google",
    "version": "13",
    "sdk": 33,
    "timestamp": 1640995200000
  },
  "timestamp": 1640995200000
}
```

## 测试示例

### Python客户端示例

```python
import socket
import json
import time

def test_adb_connection():
    # 连接到转发的端口
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(('localhost', 8888))
    
    try:
        # 测试文本消息
        sock.send(b'ping\n')
        response = sock.recv(1024).decode().strip()
        print(f"Text response: {response}")
        
        # 测试JSON消息
        message = {
            "type": "command",
            "id": "test_001",
            "data": "get_device_info"
        }
        sock.send((json.dumps(message) + '\n').encode())
        response = sock.recv(1024).decode().strip()
        print(f"JSON response: {response}")
        
    finally:
        sock.close()

if __name__ == "__main__":
    test_adb_connection()
```

### 使用curl测试

```bash
# 注意：curl不能直接用于TCP连接，这里仅作示例
# 实际应该使用nc (netcat)

# 使用netcat发送消息
echo "ping" | nc localhost 8888
echo '{"type":"ping","id":"test"}' | nc localhost 8888
```

## 项目结构

```
app/src/main/java/com/hys/adbtransport/
├── MainActivity.java          # 主界面Activity
├── AdbServer.java            # TCP服务器核心类
├── ConnectionManager.java    # 连接管理器
└── MessageHandler.java       # 消息处理器
```

## 权限说明

应用需要以下权限：
- `INTERNET`: 网络通信权限
- `ACCESS_NETWORK_STATE`: 网络状态访问权限

## 注意事项

1. **端口选择**: 建议使用1024以上的端口号
2. **防火墙**: 确保Android设备防火墙允许相应端口
3. **ADB调试**: 需要开启USB调试模式
4. **连接稳定性**: 长时间连接可能需要心跳机制维持
5. **错误处理**: 注意处理网络异常和连接断开

## 故障排除

### 常见问题

1. **服务器启动失败**
   - 检查端口是否被占用
   - 确认网络权限已授予

2. **ADB转发失败**
   - 确认ADB连接正常：`adb devices`
   - 检查端口转发状态：`adb forward --list`

3. **连接被拒绝**
   - 确认服务器已启动
   - 检查端口号是否正确

4. **消息发送失败**
   - 检查消息格式是否正确
   - 确认连接状态正常

### 调试技巧

1. 查看应用日志了解详细错误信息
2. 使用`adb logcat`查看系统日志
3. 测试时先使用简单的文本消息
4. 逐步增加消息复杂度

## 扩展开发

### 添加新命令

1. 在`MessageHandler.java`中添加新的命令处理逻辑
2. 更新`handleCommand`方法
3. 添加相应的响应格式

### 自定义协议

1. 修改`Message`类结构
2. 更新消息解析逻辑
3. 调整响应生成方法

## 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 贡献

欢迎提交Issue和Pull Request来改进项目！
