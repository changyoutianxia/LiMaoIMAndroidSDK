package com.xinbida.limaoim.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.db.LiMMsgDbManager;
import com.xinbida.limaoim.entity.LiMChannel;
import com.xinbida.limaoim.entity.LiMChannelMember;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.entity.LiMSyncMsgMode;
import com.xinbida.limaoim.interfaces.IReceivedMsgListener;
import com.xinbida.limaoim.manager.LiMConnectionManager;
import com.xinbida.limaoim.message.type.LiMChannelType;
import com.xinbida.limaoim.message.type.LiMConnectStatus;
import com.xinbida.limaoim.message.type.LiMMsgType;
import com.xinbida.limaoim.message.type.LiMSendMsgResult;
import com.xinbida.limaoim.message.type.LiMSendingMsg;
import com.xinbida.limaoim.msgmodel.LiMImageContent;
import com.xinbida.limaoim.msgmodel.LiMMediaMessageContent;
import com.xinbida.limaoim.msgmodel.LiMVideoContent;
import com.xinbida.limaoim.protocol.LiMBaseMsg;
import com.xinbida.limaoim.protocol.LiMConnectMsg;
import com.xinbida.limaoim.protocol.LiMDisconnectMsg;
import com.xinbida.limaoim.protocol.LiMMessageContent;
import com.xinbida.limaoim.protocol.LiMPingMsg;
import com.xinbida.limaoim.protocol.LiMPongMsg;
import com.xinbida.limaoim.protocol.LiMSendAckMsg;
import com.xinbida.limaoim.protocol.LiMSendMsg;
import com.xinbida.limaoim.utils.LiMDateUtils;
import com.xinbida.limaoim.utils.LiMFileUtils;
import com.xinbida.limaoim.utils.LiMLoggerUtils;

import org.xsocket.connection.IConnection;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * 5/21/21 10:51 AM
 * IM connect
 */
public class LiMConnectionHandler {
    private LiMConnectionHandler() {
    }

    private static class LiMConnectHandleBinder {
        private static final LiMConnectionHandler connect = new LiMConnectionHandler();
    }

    public static LiMConnectionHandler getInstance() {
        return LiMConnectHandleBinder.connect;
    }

    //正在发送的消息
    private final HashMap<Long, LiMSendingMsg> sendingMsgHashMap = new HashMap<>();
    // 正在重连中
    private boolean isReConnecting = false;
    // 连接状态
    private int connectStatus;
    private long lastMsgTime = 0;
    private String ip;
    private int port;
    INonBlockingConnection connection;
    LiMClientHandler clientHandler;

    public void reconnection() {
        if (isReConnecting) {
            return;
        }
        connectStatus = LiMConnectStatus.fail;
        boolean isHaveNetwork = LiMaoIMApplication.getInstance().isNetworkConnected();
        if (isHaveNetwork) {
            closeConnect();
            isReConnecting = true;
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    getIPAndPort();
                }
            }.start();
        } else {
            isReConnecting = false;
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    reconnection();
                }
            }.start();

        }
    }

    private void getIPAndPort() {
        if (!LiMaoIMApplication.getInstance().isNetworkConnected()) {
            isReConnecting = false;
            reconnection();
            return;
        }
        if (LiMaoIMApplication.getInstance().connectStatus != ConnectStatus.connect) {
            LiMLoggerUtils.getInstance().e("关闭链接...");
            stopAll();
            return;
        }

        LiMConnectionManager.getInstance().getIpAndPort((ip, port) -> {
            if (TextUtils.isEmpty(ip) || port == 0) {
                LiMLoggerUtils.getInstance().e("返回连接IP或port错误，" + String.format("ip：%s & port ：%s", ip, port));
                isReConnecting = false;
                reconnection();
            } else {
                this.ip = ip;
                this.port = port;
                new Thread(this::connSocket).start();
            }
        });
    }

    private void connSocket() {
        closeConnect();
        try {
            clientHandler = new LiMClientHandler();
            connection = new NonBlockingConnection(ip, port, clientHandler);
            connection.setIdleTimeoutMillis(1000 * 3);
            connection.setConnectionTimeoutMillis(1000 * 3);
            connection.setFlushmode(IConnection.FlushMode.ASYNC);
            isReConnecting = false;
            if (connection != null)
                connection.setAutoflush(true);
        } catch (Exception e) {
            isReConnecting = false;
            LiMLoggerUtils.getInstance().e("连接异常:" + e.getMessage());
            reconnection();
            e.printStackTrace();
        }
    }

    //发送连接消息
    void sendConnectMsg() {
        sendMessage(new LiMConnectMsg());
    }

    void receivedData(int length, byte[] data) {
        LiMMessageHandler.getInstance().cutAcceptMsg(length, data,
                new IReceivedMsgListener() {

                    public void sendAckMsg(
                            LiMSendAckMsg talkSendStatus) {
                        // 删除队列中正在发送的消息对象
                        sendingMsgHashMap.remove((long) talkSendStatus.clientSeq);
                    }

                    @Override
                    public void receiveMsg(LiMMsg message) {
                        // 收到在线消息，回服务器ack
                        LiMMsg liMMsg = new LiMMsg();
                        liMMsg.messageID = message.messageID;
                        liMMsg.messageSeq = message.messageSeq;
                        liMMsg.clientMsgNO = message.clientMsgNO;
                        sendMessage(MessageConvertHandler.getInstance().getSendBaseMsg(LiMMsgType.REVACK, liMMsg));
                    }

                    @Override
                    public void reconnect() {
                        LiMaoIMApplication.getInstance().connectStatus = ConnectStatus.connect;
                        reconnection();
                    }

                    @Override
                    public void loginStatusMsg(short status_code) {
                        handleLoginStatus(status_code);
                    }

                    @Override
                    public void heartbeatMsg(LiMPongMsg msgHeartbeat) {
                        // 心跳消息
                        lastMsgTime = LiMDateUtils.getInstance().getCurrentSeconds();
                    }

                    @Override
                    public void kickMsg(LiMDisconnectMsg liMDisconnectMsg) {
                        //被踢消息
                        LiMMessageHandler.getInstance().updateLastSendingMsgFail();
                        //通知客户端账号被踢
                        LiMaoIM.getInstance().getLiMConnectionManager().setConnectionStatus(LiMConnectStatus.kicked);
                        //更改连接状态
                        LiMaoIMApplication.getInstance().connectStatus = ConnectStatus.logOut;
                        stopAll();
                    }

                });
    }

    //重发未发送成功的消息
    public void resendMsg() {
        for (Long key : sendingMsgHashMap.keySet()) {
            if (Objects.requireNonNull(Objects.requireNonNull(sendingMsgHashMap.get(key)).liMMsg).status != LiMSendMsgResult.send_success) {
                sendMessage(MessageConvertHandler.getInstance().getSendBaseMsg(LiMMsgType.SEND, Objects.requireNonNull(sendingMsgHashMap.get(key)).liMMsg));
            }
        }
    }

    //将要发送的消息添加到队列
    private void addSendingMsg(LiMMsg sendingMsg) {
        sendingMsgHashMap.put(sendingMsg.clientSeq, new LiMSendingMsg(1, sendingMsg));
    }

    //处理登录消息状态
    private void handleLoginStatus(short status) {
        LiMLoggerUtils.getInstance().e("连接返回状态:" + status);
        LiMaoIM.getInstance().getLiMConnectionManager().setConnectionStatus(status);
        if (status == LiMConnectStatus.success) {
            //等待中
            connectStatus = LiMConnectStatus.success;
           // LiMaoIMApplication.getInstance().connectStatus = ConnectStatus.waiting;
            LiMConnectionTimerHandler.getInstance().startAll();
            resendMsg();
            LiMaoIM.getInstance().getLiMConnectionManager().setConnectionStatus(LiMConnectStatus.syncMsging);
            // 判断同步模式
            if (LiMaoIMApplication.getInstance().getSyncMsgMode() == LiMSyncMsgMode.WRITE) {
                LiMaoIM.getInstance().getLiMMsgManager().setSyncOfflineMsg((isEnd, list) -> {
                    if (isEnd) {
                        LiMMessageHandler.getInstance().saveReceiveMsg();
                        LiMaoIMApplication.getInstance().connectStatus = ConnectStatus.connect;
                        LiMaoIM.getInstance().getLiMConnectionManager().setConnectionStatus(LiMConnectStatus.success);
                    }
                });
            } else {
                LiMaoIM.getInstance().getLiMMsgManager().setSyncConversationListener(liMSyncChat -> {
                    LiMaoIMApplication.getInstance().connectStatus = ConnectStatus.connect;
                    LiMaoIM.getInstance().getLiMConnectionManager().setConnectionStatus(LiMConnectStatus.success);
                });
            }
        } else if (status == LiMConnectStatus.kicked) {
            LiMMessageHandler.getInstance().updateLastSendingMsgFail();
            LiMaoIM.getInstance().getLiMConnectionManager().setConnectionStatus(LiMConnectStatus.kicked);
            LiMaoIMApplication.getInstance().connectStatus = ConnectStatus.logOut;
            stopAll();
        } else {
            stopAll();
        }
    }

    void sendMessage(LiMBaseMsg mBaseMsg) {
        if (mBaseMsg == null) return;
        if (mBaseMsg.packetType != LiMMsgType.CONNECT) {
            if (connectStatus != LiMConnectStatus.success) {
                return;
            }
        }
        if (connection == null || !connection.isOpen()) {
            reconnection();
            return;
        }
        int status = LiMMessageHandler.getInstance().sendMessage(connection, mBaseMsg);
        if (status == 0) {
            LiMLoggerUtils.getInstance().e("发送消息失败");
            reconnection();
        }
    }

    // 查看心跳是否超时
    void checkHeartIsTimeOut() {
        long nowTime = LiMDateUtils.getInstance().getCurrentSeconds();
        if (nowTime - lastMsgTime >= 60) {
            sendMessage(new LiMPingMsg());
        }
    }


    //检测正在发送的消息
    void checkSendingMsg() {
        if (sendingMsgHashMap.size() > 0) {
            for (Iterator<Map.Entry<Long, LiMSendingMsg>> it = sendingMsgHashMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, LiMSendingMsg> item = it.next();

                LiMSendingMsg liMSendingMsg = sendingMsgHashMap.get(item.getKey());
                if (liMSendingMsg != null) {
                    if (liMSendingMsg.sendCount == 5) {
                        //标示消息发送失败
                        LiMMsgDbManager.getInstance().updateMsgStatus(item.getKey(), LiMSendMsgResult.send_fail);
                        it.remove();
                        LiMLoggerUtils.getInstance().e("消息发送失败...");
                    } else {
                        long nowTime = LiMDateUtils.getInstance().getCurrentSeconds();
                        if (nowTime - liMSendingMsg.sendTime > 10) {
                            liMSendingMsg.sendTime = LiMDateUtils.getInstance().getCurrentSeconds();
                            sendingMsgHashMap.put(item.getKey(), liMSendingMsg);
                            liMSendingMsg.sendCount++;
                            sendMessage(MessageConvertHandler.getInstance().getSendBaseMsg(LiMMsgType.SEND, Objects.requireNonNull(sendingMsgHashMap.get(item.getKey())).liMMsg));
                            LiMLoggerUtils.getInstance().e("消息发送失败...");
                        }
                    }
                }

            }

        }
    }


    /**
     * 发送消息
     *
     * @param baseContentModel 消息model
     * @param channelID        频道ID
     * @param channelType      频道类型
     */
    public void sendMessage(LiMMessageContent baseContentModel, String channelID, byte channelType) {
        final LiMMsg liMMsg = new LiMMsg();
        if (!TextUtils.isEmpty(LiMaoIMApplication.getInstance().getUid())) {
            liMMsg.fromUID = LiMaoIMApplication.getInstance().getUid();
        }
//        liMMsg.content = baseContentModel.content;
        liMMsg.type = baseContentModel.type;
        liMMsg.receipt = baseContentModel.receipt;
        //设置会话信息
        liMMsg.channelID = channelID;
        liMMsg.channelType = channelType;
        //检查频道信息
        LiMaoIM.getInstance().getLiMChannelManager().checkChannelInfo(liMMsg);
        liMMsg.baseContentMsgModel = baseContentModel;
        liMMsg.baseContentMsgModel.from_uid = liMMsg.fromUID;

        sendMessage(liMMsg);
    }

    public void sendMessage(LiMMsg liMMsg) {
        boolean hasAttached = false;
        //如果是图片消息
        if (liMMsg.baseContentMsgModel instanceof LiMImageContent) {
            hasAttached = true;
            LiMImageContent liMImageContent = (LiMImageContent) liMMsg.baseContentMsgModel;
            Bitmap bitmap = BitmapFactory.decodeFile(liMImageContent.localPath);
            if (bitmap != null) {
                liMImageContent.width = bitmap.getWidth();
                liMImageContent.height = bitmap.getHeight();
                liMMsg.baseContentMsgModel = liMImageContent;
            }
        }
        //视频消息
        if (liMMsg.baseContentMsgModel instanceof LiMVideoContent) {
            hasAttached = true;
            LiMVideoContent liMVideoContent = (LiMVideoContent) liMMsg.baseContentMsgModel;
            Bitmap bitmap = BitmapFactory.decodeFile(liMVideoContent.coverLocalPath);
            if (bitmap != null) {
                liMVideoContent.width = bitmap.getWidth();
                liMVideoContent.height = bitmap.getHeight();
                liMMsg.baseContentMsgModel = liMVideoContent;
            }
        }
        LiMBaseMsg base = MessageConvertHandler.getInstance().getSendBaseMsg(LiMMsgType.SEND, liMMsg);
        if (base != null && liMMsg.clientSeq != 0) {
            liMMsg.clientSeq = ((LiMSendMsg) base).clientSeq;
        }

        if (LiMMediaMessageContent.class.isAssignableFrom(liMMsg.baseContentMsgModel.getClass())) {
            //如果是多媒体消息类型说明存在附件
            hasAttached = true;
            ((LiMMediaMessageContent) liMMsg.baseContentMsgModel).localPath = LiMFileUtils.getInstance().saveFile(((LiMMediaMessageContent) liMMsg.baseContentMsgModel).localPath, liMMsg.channelID, liMMsg.channelType, liMMsg.clientSeq + "");
            if (liMMsg.baseContentMsgModel instanceof LiMVideoContent) {
                ((LiMVideoContent) liMMsg.baseContentMsgModel).coverLocalPath = LiMFileUtils.getInstance().saveFile(((LiMVideoContent) liMMsg.baseContentMsgModel).coverLocalPath, liMMsg.channelID, liMMsg.channelType, liMMsg.clientSeq + "_1");
            }
            liMMsg.content = liMMsg.baseContentMsgModel.encodeMsg().toString();
            LiMMsgDbManager.getInstance().insertMsg(liMMsg);
        }
        //获取发送者信息
        LiMChannel from = LiMaoIM.getInstance().getLiMChannelManager().getLiMChannel(LiMaoIMApplication.getInstance().getUid(), LiMChannelType.PERSONAL);
        if (from == null) {
            LiMaoIM.getInstance().getLiMChannelManager().getChannelInfo(LiMaoIMApplication.getInstance().getUid(), LiMChannelType.PERSONAL, liMChannel -> LiMaoIM.getInstance().getLiMChannelManager().addOrUpdateChannel(liMChannel));
        } else {
            liMMsg.setFrom(from);
        }
        LiMChannelMember member = LiMaoIM.getInstance().getLiMChannelMembersManager().getLiMChannelMember(liMMsg.channelID, liMMsg.channelType, LiMaoIMApplication.getInstance().getUid());
        liMMsg.setMemberOfFrom(member);
        //将消息push回UI层
        LiMaoIM.getInstance().getLiMMsgManager().setSendMsgCallback(liMMsg);
        if (hasAttached) {
            //存在附件处理
            LiMaoIM.getInstance().getLiMMsgManager().setUploadAttachment(liMMsg, (isSuccess, liMMessageContent) -> {
                if (isSuccess) {
                    if (!sendingMsgHashMap.containsKey(liMMsg.clientSeq)) {
                        liMMsg.baseContentMsgModel = liMMessageContent;
                        LiMBaseMsg base1 = MessageConvertHandler.getInstance().getSendBaseMsg(LiMMsgType.SEND, liMMsg);
                        addSendingMsg(liMMsg);
                        sendMessage(base1);
                    }
                } else {
                    liMMsg.status = LiMSendMsgResult.send_fail;
                    LiMMsgDbManager.getInstance().updateMsgStatus(liMMsg.clientSeq, liMMsg.status);
                }
            });
        } else {
            addSendingMsg(liMMsg);
            sendMessage(base);
        }
    }

    public boolean connectionIsNull() {
        return connection == null || !connection.isOpen();
    }

    public void stopAll() {
        clientHandler = null;
        LiMConnectionTimerHandler.getInstance().stopAll();
        closeConnect();
        connectStatus = LiMConnectStatus.fail;
        isReConnecting = false;
        System.gc();
    }

    private void closeConnect() {
        if (connection != null && connection.isOpen()) {
            try {
                LiMLoggerUtils.getInstance().e("stop connection" + connection.getId());
                connection.flush();
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
                LiMLoggerUtils.getInstance().e("stop connection IOException" + e.getMessage());
            }
        }
        connection = null;
    }
}
