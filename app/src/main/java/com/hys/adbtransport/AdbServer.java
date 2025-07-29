package com.hys.adbtransport;

import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ADB通信服务器核心类
 * 负责创建TCP服务器，监听指定端口，处理客户端连接
 */
public class AdbServer {
    private static final String TAG = "AdbServer";
    private static final int DEFAULT_PORT = 9999;
    
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private int port;
    private ServerListener listener;


    
    /**
     * 服务器状态监听器
     */
    public interface ServerListener {
        void onServerStarted(int port);
        void onServerStopped();
        void onClientConnected(String clientAddress);
        void onClientDisconnected(String clientAddress);
        void onError(String error);
        void onMessageReceived(String message, String clientAddress);
    }
    
    public AdbServer() {
        this(DEFAULT_PORT);
    }
    
    public AdbServer(int port) {
        this.port = port;
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * 设置服务器监听器
     */
    public void setServerListener(ServerListener listener) {
        this.listener = listener;
    }


    
    /**
     * 启动服务器
     */
    public void start() {
        if (isRunning.get()) {
            Log.w(TAG, "服务器已经在运行中");
            return;
        }
        
        executorService.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning.set(true);
                
                Log.i(TAG, "ADB服务器启动成功，监听端口: " + port);
                if (listener != null) {
                    listener.onServerStarted(port);
                }
                
                // 监听客户端连接
                while (isRunning.get() && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        String clientAddress = clientSocket.getRemoteSocketAddress().toString();

                        Log.i(TAG, "客户端连接: " + clientAddress);

                        // 为每个客户端创建连接管理器
                        ConnectionManager connectionManager = new ConnectionManager(
                            clientSocket,
                            new ConnectionManager.ConnectionListener() {
                                @Override
                                public void onMessageReceived(String message) {
                                    if (listener != null) {
                                        listener.onMessageReceived(message, clientAddress);
                                    }
                                }

                                @Override
                                public void onConnectionClosed() {
                                    Log.i(TAG, "客户端断开连接: " + clientAddress);
                                    if (listener != null) {
                                        listener.onClientDisconnected(clientAddress);
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "连接错误: " + error);
                                    if (listener != null) {
                                        AdbServer.this.listener.onError(error);
                                    }
                                }
                            },
                            null  // 不再传递VoiceTestSDK
                        );

                        // 通知连接建立
                        if (listener != null) {
                            listener.onClientConnected(clientAddress);
                        }

                        // 在新线程中处理客户端连接
                        executorService.execute(connectionManager);
                        
                    } catch (IOException e) {
                        if (isRunning.get()) {
                            Log.e(TAG, "接受客户端连接时出错: " + e.getMessage());
                            if (listener != null) {
                                listener.onError("接受连接失败: " + e.getMessage());
                            }
                        }
                    }
                }
                
            } catch (IOException e) {
                Log.e(TAG, "启动服务器失败: " + e.getMessage());
                if (listener != null) {
                    listener.onError("启动服务器失败: " + e.getMessage());
                }
            } finally {
                isRunning.set(false);
                if (listener != null) {
                    listener.onServerStopped();
                }
            }
        });
    }
    
    /**
     * 停止服务器
     */
    public void stop() {
        if (!isRunning.get()) {
            return;
        }
        
        isRunning.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭服务器时出错: " + e.getMessage());
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        Log.i(TAG, "ADB服务器已停止");
    }
    
    /**
     * 检查服务器是否正在运行
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * 获取服务器端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 设置服务器端口
     */
    public void setPort(int port) {
        if (isRunning.get()) {
            throw new IllegalStateException("无法在服务器运行时更改端口");
        }
        this.port = port;
    }




}
