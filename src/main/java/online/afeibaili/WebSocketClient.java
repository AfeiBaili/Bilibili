package online.afeibaili;

import cn.hutool.core.util.ZipUtil;
import javazoom.jl.player.Player;
import online.afeibaili.kiroBot.Same;
import online.afeibaili.kiroBot.Voice;
import online.afeibaili.map.Package;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 继承WebSocketClient
 */
public class WebSocketClient extends org.java_websocket.client.WebSocketClient {
    public byte[] pkg;
    boolean flag = true;
    public Timer timer = new Timer();
    public Timer timer2 = new Timer();
    //定时器
    private ExecutorService messageExecutor = Executors.newCachedThreadPool();

    {
        //配置定时器
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                send(Package.createDataPackage(2, new byte[0]));
            }
        }, 30000, 30000);
    }
    {
        //配置定时器
        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                send(Package.createDataPackage(2, new byte[0]));
            }
        }, 60000, 60000);
    }

    public WebSocketClient(URI serverUri, byte[] pkg) {
        super(serverUri);
        this.pkg = pkg;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        this.send(pkg);
        System.out.println("连接成功!");
    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        String connect = unpack(bytes);
        if (connect != null) {
            if (flag) {
                messageExecutor.execute(() -> {
                    flag = false;
                    try {
                        voice(connect);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    flag = true;
                });
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        timer.cancel();
        System.out.println("onClose: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println(ex.getMessage());
    }

    /**
     * 解发送过来的包
     *
     * @param byteBuffer 字节缓存
     * @return
     */
    public static String unpack(ByteBuffer byteBuffer) {

        try {
            if (byteBuffer.remaining() < 16) {
                System.out.println("数据包不完整，需要等待更多数据");
                return null; // 或进行其他处理
            }
            int packageLen = byteBuffer.getInt();
            short headLength = byteBuffer.getShort();
            short packageVer = byteBuffer.getShort();
            int optCode = byteBuffer.getInt();
            int sequence = byteBuffer.getInt();
            if (2 == optCode) {
                System.out.println("这是服务器心跳回复");
            }
            byte[] contentBytes = new byte[packageLen - headLength];
            byteBuffer.get(contentBytes);
            //如果是zip包就进行解包
            if (2 == packageVer) {
                return unpack(ByteBuffer.wrap(ZipUtil.unZlib(contentBytes)));
            }

            String content = new String(contentBytes, StandardCharsets.UTF_8);
            if (8 == optCode) {
            }
            if (5 == optCode) {
//                content = CleanMessage.cleanMessage(content);
//                System.out.println("这是弹幕消息：" + content);
                return content;
                // todo 自定义处理

            }
            if (byteBuffer.position() < byteBuffer.limit()) {
                unpack(byteBuffer);
            }
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void voice(String content) {
        if(content.charAt(0)!='{'){
            return;
        }
        JSONObject jsonObject = new JSONObject(content);
        System.out.println("音频");
        if (jsonObject.has("info")) {
            CleanMessage cleanMessage = new CleanMessage();
            String message = cleanMessage.cleanMessage(content);
            System.out.println("弹幕消息：" + message);
            String ElysiaVoice = Same.getMessage(message, 2, "Elysia");
            File file;
            try {
                file = Voice.generateVoiceFile(ElysiaVoice, "Elysia");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Path path = file.toPath();
            System.out.println(path);
            try (FileInputStream fis = new FileInputStream(path.toString())) {
                Player player = new Player(fis);
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
