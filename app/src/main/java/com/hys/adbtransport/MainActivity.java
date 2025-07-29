package com.hys.adbtransport;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity implements AdbServer.ServerListener {

    private AdbServer adbServer;
    private Handler mainHandler;
    private AtomicInteger connectionCount = new AtomicInteger(0);
    private VoiceTestSDK voiceTestSDK;

    // UI组件
    private TextView tvStatus;
    private TextView tvConnections;
    private TextView tvLog;
    private TextView tvVoiceSDKStatus;
    private EditText etPort;
    private Button btnStart;
    private Button btnStop;
    private Button btnClearLog;

    private Button btnVoiceInit;
    private Button btnVoiceTest;
    private Button btnVoiceResult;
    private Button btnVoiceStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initServer();
        setupClickListeners();

        // 自动启动服务器
        autoStartServer();

        // 自动初始化语音SDK
        autoInitVoiceSDK();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvConnections = findViewById(R.id.tvConnections);
        tvLog = findViewById(R.id.tvLog);
        tvVoiceSDKStatus = findViewById(R.id.tvVoiceSDKStatus);
        etPort = findViewById(R.id.etPort);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnClearLog = findViewById(R.id.btnClearLog);

        btnVoiceInit = findViewById(R.id.btnVoiceInit);
        btnVoiceTest = findViewById(R.id.btnVoiceTest);
        btnVoiceResult = findViewById(R.id.btnVoiceResult);
        btnVoiceStatus = findViewById(R.id.btnVoiceStatus);

        // 设置日志区域可滚动
        tvLog.setMovementMethod(new ScrollingMovementMethod());

        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initServer() {
        adbServer = new AdbServer();
        adbServer.setServerListener(this);

        // 初始化VoiceTestSDK
        voiceTestSDK = new VoiceTestSDK();

        appendLog("ADB Transport Server 初始化完成");
        appendLog("语音测试SDK 已创建");
        appendLog("默认端口: " + adbServer.getPort());
        appendLog("使用说明:");
        appendLog("1. 设置端口号（默认9999）");
        appendLog("2. 点击'启动服务器'");
        appendLog("3. 在PC端执行: adb forward tcp:LOCAL_PORT tcp:" + adbServer.getPort());
        appendLog("4. PC端即可通过LOCAL_PORT与Android通信");
        appendLog("5. 使用语音测试按钮测试语音功能");
        appendLog("6. PC端可通过JSON命令调用语音测试功能");
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> startServer());
        btnStop.setOnClickListener(v -> stopServer());
        btnClearLog.setOnClickListener(v -> clearLog());


        // 语音测试按钮监听器
        btnVoiceInit.setOnClickListener(v -> initializeVoiceSDK());
        btnVoiceTest.setOnClickListener(v -> startVoiceTest());
        btnVoiceResult.setOnClickListener(v -> getVoiceResult());
        btnVoiceStatus.setOnClickListener(v -> queryVoiceSDKStatus());
    }

    /**
     * 自动启动服务器
     */
    private void autoStartServer() {
        // 延迟一点时间确保UI初始化完成
        mainHandler.postDelayed(() -> {
            try {
                int defaultPort = 9999; // 默认端口
                etPort.setText(String.valueOf(defaultPort));

                appendLog("🚀 应用启动，自动启动服务器...");
                adbServer.setPort(defaultPort);
                adbServer.start();

            } catch (Exception e) {
                appendLog("❌ 自动启动服务器失败: " + e.getMessage());
                Toast.makeText(this, "自动启动服务器失败", Toast.LENGTH_SHORT).show();
            }
        }, 500); // 延迟500毫秒
    }

    /**
     * 自动初始化语音SDK
     */
    private void autoInitVoiceSDK() {
        // 延迟一点时间确保服务器启动完成
        mainHandler.postDelayed(() -> {
            try {
                if (voiceTestSDK == null) {
                    appendLog("❌ 语音测试SDK未创建");
                    return;
                }

                appendLog("🎤 应用启动，自动初始化语音测试SDK...");

                voiceTestSDK.initSDK(getApplication(), this);

                if (VoiceTestSDK.isSDKInitialized()) {
                    appendLog("✅ 语音测试SDK自动初始化成功");
                    Toast.makeText(this, "语音测试SDK已就绪", Toast.LENGTH_SHORT).show();
                } else {
                    appendLog("❌ 语音测试SDK自动初始化失败");
                    Toast.makeText(this, "语音测试SDK初始化失败", Toast.LENGTH_SHORT).show();
                }

                updateUI();

            } catch (Exception e) {
                appendLog("❌ 自动初始化语音测试SDK异常: " + e.getMessage());
                Toast.makeText(this, "语音测试SDK初始化异常", Toast.LENGTH_SHORT).show();
            }
        }, 600); // 延迟600毫秒，确保服务器先启动
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(etPort.getText().toString().trim());
            if (port < 1024 || port > 65535) {
                Toast.makeText(this, "端口号必须在1024-65535之间", Toast.LENGTH_SHORT).show();
                return;
            }

            adbServer.setPort(port);
            adbServer.start();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的端口号", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            appendLog("启动服务器失败: " + e.getMessage());
            Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopServer() {
        if (adbServer != null) {
            adbServer.stop();
        }
    }

    private void clearLog() {
        tvLog.setText("");
    }



    private void updateUI() {
        mainHandler.post(() -> {
            boolean isRunning = adbServer != null && adbServer.isRunning();
            boolean voiceSDKInitialized = VoiceTestSDK.isSDKInitialized();

            String serverStatus = isRunning ? getString(R.string.server_running) : getString(R.string.server_stopped);
            tvStatus.setText(getString(R.string.server_status_format, serverStatus));
            tvConnections.setText(getString(R.string.connection_count_format, connectionCount.get()));

            // 更新语音SDK状态显示
            String voiceStatus = voiceSDKInitialized ? getString(R.string.voice_sdk_initialized) : getString(R.string.voice_sdk_not_initialized);
            tvVoiceSDKStatus.setText(getString(R.string.voice_sdk_status_format, voiceStatus));

            btnStart.setEnabled(!isRunning);
            btnStop.setEnabled(isRunning);

            etPort.setEnabled(!isRunning);

            // 语音测试按钮状态
            btnVoiceInit.setEnabled(!voiceSDKInitialized);
            btnVoiceTest.setEnabled(voiceSDKInitialized);
            btnVoiceResult.setEnabled(voiceSDKInitialized);
            btnVoiceStatus.setEnabled(voiceTestSDK != null);
        });
    }

    private void appendLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";

        mainHandler.post(() -> {
            tvLog.append(logEntry);
            // 自动滚动到底部
            int scrollAmount = tvLog.getLayout().getLineTop(tvLog.getLineCount()) - tvLog.getHeight();
            if (scrollAmount > 0) {
                tvLog.scrollTo(0, scrollAmount);
            }
        });
    }

    // AdbServer.ServerListener 接口实现
    @Override
    public void onServerStarted(int port) {
        appendLog("✓ 服务器启动成功，监听端口: " + port);
        appendLog("ADB端口转发命令: adb forward tcp:LOCAL_PORT tcp:" + port);
        updateUI();
    }

    @Override
    public void onServerStopped() {
        appendLog("✗ 服务器已停止");
        connectionCount.set(0);
        updateUI();
    }

    @Override
    public void onClientConnected(String clientAddress) {
        connectionCount.incrementAndGet();
        appendLog("✓ 客户端连接: " + clientAddress);
        updateUI();
    }

    @Override
    public void onClientDisconnected(String clientAddress) {
        connectionCount.decrementAndGet();
        appendLog("✗ 客户端断开: " + clientAddress);
        updateUI();
    }

    @Override
    public void onError(String error) {
        appendLog("❌ 错误: " + error);
        updateUI();
    }

    @Override
    public void onMessageReceived(String message, String clientAddress) {
        appendLog("📨 收到消息 [" + clientAddress + "]: " + message);
    }

    // ========== 语音测试SDK操作方法 ==========

    /**
     * 初始化语音测试SDK
     */
    private void initializeVoiceSDK() {
        if (voiceTestSDK == null) {
            appendLog("❌ 语音测试SDK未创建");
            return;
        }

        appendLog("🔄 开始初始化语音测试SDK...");
        btnVoiceInit.setEnabled(false);

        try {
            voiceTestSDK.initSDK(getApplication(), this);

            if (VoiceTestSDK.isSDKInitialized()) {
                appendLog("✅ 语音测试SDK初始化成功");
            } else {
                appendLog("❌ 语音测试SDK初始化失败");
                btnVoiceInit.setEnabled(true);
            }
        } catch (Exception e) {
            appendLog("❌ 语音测试SDK初始化异常: " + e.getMessage());
            btnVoiceInit.setEnabled(true);
        }

        updateUI();
    }

    /**
     * 开始语音测试
     */
    private void startVoiceTest() {
        if (!VoiceTestSDK.isSDKInitialized()) {
            appendLog("❌ 语音测试SDK未初始化");
            return;
        }

        // 使用默认测试参数（所有参数通过脚本传递）
        String title = "默认测试话术";
        String area = "1";

        appendLog("🎤 开始语音测试...");
        appendLog("  话术: " + title);
        appendLog("  音区: " + area);
        appendLog("  注意: 实际参数请通过PC端脚本传递");

        btnVoiceTest.setEnabled(false);

        try {
            VoiceTestSDK.startTest(title, area);
            appendLog("✅ 语音测试已启动，请等待结果...");

            // 启动结果检查定时器
            startResultCheckTimer();

        } catch (Exception e) {
            appendLog("❌ 启动语音测试失败: " + e.getMessage());
            btnVoiceTest.setEnabled(true);
        }

        updateUI();
    }

    /**
     * 获取语音测试结果
     */
    private void getVoiceResult() {
        if (!VoiceTestSDK.isSDKInitialized()) {
            appendLog("❌ 语音测试SDK未初始化");
            return;
        }

        try {
            if (VoiceTestSDK.ifRetrunAns()) {
                String result = VoiceTestSDK.getAns();
                appendLog("📋 语音测试结果:");

                // 解析结果格式: "结果,执行ID"
                String[] parts = result.split(",", 2);
                if (parts.length >= 2) {
                    appendLog("  🎯 测试结果: " + parts[0]);
                    appendLog("  🆔 执行ID: " + parts[1]);
                } else {
                    appendLog("  🎯 测试结果: " + result);
                }

                btnVoiceTest.setEnabled(true);
            } else {
                appendLog("⏳ 语音测试结果尚未准备好，请稍后再试");
            }
        } catch (Exception e) {
            appendLog("❌ 获取语音测试结果失败: " + e.getMessage());
        }

        updateUI();
    }

    /**
     * 查询语音测试SDK状态
     */
    private void queryVoiceSDKStatus() {
        try {
            Map<String, Object> status = VoiceTestSDK.getSDKStatus();
            appendLog("📋 语音测试SDK状态查询结果:");
            for (Map.Entry<String, Object> entry : status.entrySet()) {
                appendLog("  " + entry.getKey() + ": " + entry.getValue());
            }
        } catch (Exception e) {
            appendLog("❌ 查询语音测试SDK状态失败: " + e.getMessage());
        }
        updateUI();
    }

    /**
     * 启动结果检查定时器
     */
    private void startResultCheckTimer() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (VoiceTestSDK.ifRetrunAns()) {
                    appendLog("✅ 语音测试完成，可以获取结果了");
                    btnVoiceTest.setEnabled(true);
                    updateUI();
                } else {
                    // 继续检查
                    mainHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adbServer != null && adbServer.isRunning()) {
            adbServer.stop();
        }
        if (voiceTestSDK != null) {
            VoiceTestSDK.release();
        }
    }
}