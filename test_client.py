#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ADB Transport 测试客户端
用于测试Android ADB Transport应用的通信功能
"""

import socket
import json
import time
import threading
import uuid
from typing import Optional, Dict, Any

class AdbTransportClient:
    """ADB Transport客户端类"""
    
    def __init__(self, host: str = 'localhost', port: int = 8888):
        """
        初始化客户端
        
        Args:
            host: 服务器地址（通过ADB转发后通常是localhost）
            port: 本地转发端口
        """
        self.host = host
        self.port = port
        self.socket: Optional[socket.socket] = None
        self.connected = False
        
    def connect(self) -> bool:
        """
        连接到服务器
        
        Returns:
            bool: 连接是否成功
        """
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.host, self.port))
            self.connected = True
            print(f"✓ 已连接到 {self.host}:{self.port}")
            return True
        except Exception as e:
            print(f"❌ 连接失败: {e}")
            return False
    
    def disconnect(self):
        """断开连接"""
        if self.socket:
            self.socket.close()
            self.connected = False
            print("✓ 连接已断开")
    
    def send_text(self, message: str) -> Optional[str]:
        """
        发送文本消息
        
        Args:
            message: 要发送的文本消息
            
        Returns:
            str: 服务器响应，如果失败返回None
        """
        if not self.connected or not self.socket:
            print("❌ 未连接到服务器")
            return None
        
        try:
            # 发送消息（添加换行符）
            self.socket.send((message + '\n').encode('utf-8'))
            
            # 接收响应
            response = self.socket.recv(1024).decode('utf-8').strip()
            return response
        except Exception as e:
            print(f"❌ 发送文本消息失败: {e}")
            return None
    
    def send_json(self, message_type: str, data: Any = None, message_id: str = None) -> Optional[Dict]:
        """
        发送JSON消息
        
        Args:
            message_type: 消息类型
            data: 消息数据
            message_id: 消息ID，如果不提供会自动生成
            
        Returns:
            dict: 解析后的JSON响应，如果失败返回None
        """
        if not self.connected or not self.socket:
            print("❌ 未连接到服务器")
            return None
        
        if message_id is None:
            message_id = str(uuid.uuid4())
        
        message = {
            "type": message_type,
            "id": message_id,
            "data": data,
            "timestamp": int(time.time() * 1000)
        }
        
        try:
            # 发送JSON消息
            json_str = json.dumps(message)
            self.socket.send((json_str + '\n').encode('utf-8'))
            
            # 接收响应
            response = self.socket.recv(1024).decode('utf-8').strip()
            
            # 尝试解析JSON响应
            try:
                return json.loads(response)
            except json.JSONDecodeError:
                # 如果不是JSON格式，返回原始文本
                return {"raw_response": response}
                
        except Exception as e:
            print(f"❌ 发送JSON消息失败: {e}")
            return None
    
    def ping(self) -> bool:
        """发送ping测试"""
        print("📤 发送ping...")
        response = self.send_text("ping")
        if response == "pong":
            print("✓ Ping成功")
            return True
        else:
            print(f"❌ Ping失败，响应: {response}")
            return False


    
    def echo_test(self, message: str) -> bool:
        """回显测试"""
        print(f"📤 发送echo: {message}")
        response = self.send_text(f"echo {message}")
        if response == message:
            print("✓ Echo测试成功")
            return True
        else:
            print(f"❌ Echo测试失败，期望: {message}，实际: {response}")
            return False
    
    def get_device_info(self) -> Optional[Dict]:
        """获取设备信息"""
        print("📤 获取设备信息...")
        response = self.send_json("command", "get_device_info")
        if response and response.get("type") == "response":
            print("✓ 设备信息获取成功:")
            device_info = response.get("data", {})
            for key, value in device_info.items():
                print(f"  {key}: {value}")
            return device_info
        else:
            print(f"❌ 获取设备信息失败: {response}")
            return None
    
    def json_ping(self) -> bool:
        """JSON格式的ping测试"""
        print("📤 发送JSON ping...")
        response = self.send_json("ping", "ping")
        if response and response.get("type") == "pong":
            print("✓ JSON Ping成功")
            return True
        else:
            print(f"❌ JSON Ping失败: {response}")
            return False

    # ========== 语音测试SDK方法 ==========

    def voice_init(self) -> bool:
        """初始化语音测试SDK"""
        print("📤 初始化语音测试SDK...")
        response = self.send_json("command", "voice_init")
        if response and response.get("type") == "response":
            print("✓ 语音测试SDK初始化成功")
            return True
        else:
            print(f"❌ 语音测试SDK初始化失败: {response}")
            return False

    def voice_start_test(self, title: str = "你好，这是语音测试", area: str = "1") -> bool:
        """开始语音测试"""
        print(f"📤 开始语音测试 - 话术: {title}, 音区: {area}")

        command_data = {
            "command": "voice_start_test",
            "title": title,
            "area": area
        }

        response = self.send_json("command", command_data)
        if response and response.get("type") == "response":
            print("✓ 语音测试已开始")
            return True
        else:
            print(f"❌ 语音测试启动失败: {response}")
            return False

    def voice_check_result(self) -> bool:
        """检查语音测试结果是否准备好"""
        print("📤 检查语音测试结果状态...")
        response = self.send_json("command", "voice_check_result")
        if response and response.get("type") == "response":
            data = response.get("data", {})
            has_result = data.get("hasResult", False)
            status = data.get("status", "unknown")
            print(f"✓ 结果状态: {status}, 是否有结果: {has_result}")
            return has_result
        else:
            print(f"❌ 检查结果状态失败: {response}")
            return False

    def voice_get_result(self) -> Optional[Dict]:
        """获取语音测试结果"""
        print("📤 获取语音测试结果...")
        response = self.send_json("command", "voice_get_result")
        if response and response.get("type") == "response":
            data = response.get("data", {})
            if "result" in data:
                print("✓ 语音测试结果获取成功:")
                print(f"  测试结果: {data.get('result')}")
                print(f"  执行ID: {data.get('exeID')}")
                print(f"  状态: {data.get('status')}")
                return data
            else:
                print(f"⏳ {data.get('message', '测试结果尚未准备好')}")
                return None
        else:
            print(f"❌ 获取语音测试结果失败: {response}")
            return None

    def voice_get_status(self) -> Optional[Dict]:
        """获取语音测试SDK状态"""
        print("📤 获取语音测试SDK状态...")
        response = self.send_json("command", "voice_get_status")
        if response and response.get("type") == "response":
            print("✓ 语音测试SDK状态获取成功:")
            status_data = response.get("data", {})
            for key, value in status_data.items():
                print(f"  {key}: {value}")
            return status_data
        else:
            print(f"❌ 语音测试SDK状态获取失败: {response}")
            return None


def run_basic_tests(client: AdbTransportClient):
    """运行基础测试"""
    print("\n=== 基础功能测试 ===")
    
    # 文本消息测试
    client.ping()
    client.echo_test("Hello Android!")
    
    # 状态查询
    response = client.send_text("status")
    print(f"📤 服务器状态: {response}")
    
    # JSON消息测试
    client.json_ping()
    client.get_device_info()
    
    # 获取时间戳
    time_response = client.send_json("command", "get_time")
    if time_response:
        print(f"📤 服务器时间戳: {time_response.get('data')}")



def run_voice_tests(client: AdbTransportClient):
    """运行语音测试功能测试"""
    print("\n=== 语音测试功能测试 ===")

    # 1. 获取语音SDK状态
    print("\n1. 查询语音测试SDK状态")
    client.voice_get_status()

    # 2. 初始化语音SDK
    print("\n2. 初始化语音测试SDK")
    if client.voice_init():
        time.sleep(1)  # 等待初始化完成
        client.voice_get_status()  # 再次查询状态

    # 3. 语音测试
    print("\n3. 语音测试")
    test_cases = [
        ("你好，这是语音测试", "2"),
        ("欢迎使用语音识别系统", "1"),
        ("请说出您的姓名", "3"),
        ("今天天气真不错", "4"),
        ("语音测试完成，谢谢配合", "2")
    ]

    for i, (title, area) in enumerate(test_cases, 1):
        print(f"\n  测试用例 {i}: {title} ({area})")
        if client.voice_start_test(title, area):
            # 等待测试完成
            print("  等待测试完成...")
            for _ in range(10):  # 最多等待10秒
                time.sleep(1)
                if client.voice_check_result():
                    result = client.voice_get_result()
                    if result:
                        break
                else:
                    print("    测试进行中...")

    # 4. 最终状态查询
    print("\n4. 最终状态查询")
    client.voice_get_status()


def interactive_mode(client: AdbTransportClient):
    """交互模式"""
    print("\n=== 交互模式 ===")
    print("输入消息发送到服务器，输入'quit'退出")
    print("快捷命令:")
    print("  voice_init     - 初始化语音测试SDK")
    print("  voice_test     - 开始语音测试")
    print("  voice_check    - 检查测试结果")
    print("  voice_result   - 获取测试结果")
    print("  voice_status   - 查询SDK状态")
    print("  help           - 显示帮助")

    while True:
        try:
            message = input(">>> ").strip()
            if message.lower() == 'quit':
                break
            elif message.lower() == 'help':
                print("可用命令:")
                print("  文本命令: ping, hello, status, echo <text>")
                print("  语音测试命令: voice_init, voice_test, voice_check, voice_result, voice_status")
                print("  JSON格式: {\"type\":\"command\",\"data\":\"voice_get_status\"}")
                continue
            elif message.lower() == 'voice_init':
                client.voice_init()
                continue
            elif message.lower() == 'voice_test':
                client.voice_start_test("你好，这是语音测试", "2")
                continue
            elif message.lower() == 'voice_check':
                client.voice_check_result()
                continue
            elif message.lower() == 'voice_result':
                client.voice_get_result()
                continue
            elif message.lower() == 'voice_status':
                client.voice_get_status()
                continue

            if message.startswith('{') and message.endswith('}'):
                # JSON消息
                try:
                    json_data = json.loads(message)
                    response = client.send_json(
                        json_data.get('type', 'unknown'),
                        json_data.get('data'),
                        json_data.get('id')
                    )
                    print(f"<<< {json.dumps(response, indent=2, ensure_ascii=False)}")
                except json.JSONDecodeError:
                    print("❌ 无效的JSON格式")
            else:
                # 文本消息
                response = client.send_text(message)
                print(f"<<< {response}")

        except KeyboardInterrupt:
            break
        except Exception as e:
            print(f"❌ 错误: {e}")

def main():
    """主函数"""
    print("ADB Transport 测试客户端")
    print("=" * 40)
    
    # 提示用户设置ADB端口转发
    print("请确保已设置ADB端口转发:")
    print("adb forward tcp:8888 tcp:9999")
    print()
    
    # 创建客户端
    client = AdbTransportClient()
    
    try:
        # 连接服务器
        if not client.connect():
            return
        
        # 选择测试类型
        print("请选择测试类型:")
        print("1. 基础功能测试")
        print("2. 语音测试功能测试")
        print("3. 交互模式")

        choice = input("请输入选择 (1-3): ").strip()

        if choice == '1':
            run_basic_tests(client)
        elif choice == '2':
            run_voice_tests(client)
        elif choice == '3':
            interactive_mode(client)
        else:
            print("无效选择，运行基础测试")
            run_basic_tests(client)
    
    finally:
        client.disconnect()

if __name__ == "__main__":
    main()
