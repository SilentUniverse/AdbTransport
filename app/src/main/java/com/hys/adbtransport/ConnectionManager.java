package com.hys.adbtransport;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 连接管理器
 * 负责管理单个客户端连接的生命周期，处理消息收发
 */
public class ConnectionManager implements Runnable {
    private static final String TAG = "ConnectionManager";
    
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private ConnectionListener listener;
    private MessageHandler messageHandler;
    
    /**
     * 连接监听器
     */
    public interface ConnectionListener {
        void onMessageReceived(String message);
        void onConnectionClosed();
        void onError(String error);
    }
    
    public ConnectionManager(Socket clientSocket, ConnectionListener listener) {
        this(clientSocket, listener, null);
    }

    public ConnectionManager(Socket clientSocket, ConnectionListener listener, VoiceTestSDK voiceTestSDK) {
        this.clientSocket = clientSocket;
        this.listener = listener;
        this.messageHandler = new MessageHandler();

        // VoiceTestSDK参数已不再使用，MessageHandler直接使用静态方法

        try {
            this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.writer = new PrintWriter(clientSocket.getOutputStream(), true);
            this.isConnected.set(true);
        } catch (IOException e) {
            Log.e(TAG, "初始化连接管理器失败: " + e.getMessage());
            if (listener != null) {
                listener.onError("初始化连接失败: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void run() {
        if (!isConnected.get()) {
            return;
        }
        
        try {
            String inputLine;
            while (isConnected.get() && (inputLine = reader.readLine()) != null) {
                Log.d(TAG, "收到消息: " + inputLine);
                
                // 处理接收到的消息
                String response = messageHandler.handleMessage(inputLine);
                
                // 发送响应
                if (response != null) {
                    sendMessage(response);
                }
                
                // 通知监听器
                if (listener != null) {
                    listener.onMessageReceived(inputLine);
                }
            }
        } catch (IOException e) {
            if (isConnected.get()) {
                Log.e(TAG, "读取消息时出错: " + e.getMessage());
                if (listener != null) {
                    listener.onError("读取消息失败: " + e.getMessage());
                }
            }
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 发送消息到客户端
     */
    public boolean sendMessage(String message) {
        if (!isConnected.get() || writer == null) {
            Log.w(TAG, "连接已断开，无法发送消息");
            return false;
        }
        
        try {
            writer.println(message);
            writer.flush();
            Log.d(TAG, "发送消息: " + message);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "发送消息失败: " + e.getMessage());
            if (listener != null) {
                listener.onError("发送消息失败: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 发送JSON响应
     */
    public boolean sendJsonResponse(Object responseObject) {
        String jsonResponse = messageHandler.objectToJson(responseObject);
        return sendMessage(jsonResponse);
    }
    
    /**
     * 关闭连接
     */
    public void closeConnection() {
        if (!isConnected.get()) {
            return;
        }
        
        isConnected.set(false);
        
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭输入流时出错: " + e.getMessage());
        }
        
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭输出流时出错: " + e.getMessage());
        }
        
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭客户端Socket时出错: " + e.getMessage());
        }
        
        Log.i(TAG, "连接已关闭");
        if (listener != null) {
            listener.onConnectionClosed();
        }
    }
    
    /**
     * 检查连接是否活跃
     */
    public boolean isConnected() {
        return isConnected.get() && clientSocket != null && !clientSocket.isClosed();
    }
    
    /**
     * 获取客户端地址
     */
    public String getClientAddress() {
        if (clientSocket != null) {
            return clientSocket.getRemoteSocketAddress().toString();
        }
        return "未知";
    }
    

}
