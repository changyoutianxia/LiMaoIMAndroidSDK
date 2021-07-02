package com.xinbida.limaoim.message;

import android.text.TextUtils;
import android.util.Log;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.db.LiMConversationDbManager;
import com.xinbida.limaoim.db.LiMDBColumns;
import com.xinbida.limaoim.db.LiMMsgDbManager;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.entity.LiMSyncMsg;
import com.xinbida.limaoim.entity.LiMUIConversationMsg;
import com.xinbida.limaoim.interfaces.IReceivedMsgListener;
import com.xinbida.limaoim.manager.LiMCMDManager;
import com.xinbida.limaoim.manager.LiMConversationManager;
import com.xinbida.limaoim.message.type.LiMChannelType;
import com.xinbida.limaoim.message.type.LiMMsgContentType;
import com.xinbida.limaoim.message.type.LiMMsgType;
import com.xinbida.limaoim.message.type.LiMSendMsgResult;
import com.xinbida.limaoim.protocol.LiMBaseMsg;
import com.xinbida.limaoim.protocol.LiMConnectAckMsg;
import com.xinbida.limaoim.protocol.LiMConnectMsg;
import com.xinbida.limaoim.protocol.LiMDisconnectMsg;
import com.xinbida.limaoim.protocol.LiMPingMsg;
import com.xinbida.limaoim.protocol.LiMPongMsg;
import com.xinbida.limaoim.protocol.LiMReceivedAckMsg;
import com.xinbida.limaoim.protocol.LiMSendAckMsg;
import com.xinbida.limaoim.protocol.LiMSendMsg;
import com.xinbida.limaoim.utils.LiMLoggerUtils;
import com.xinbida.limaoim.utils.LiMTypeUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.xsocket.connection.INonBlockingConnection;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.BufferOverflowException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 5/21/21 11:25 AM
 * msg handler
 */
public class LiMMessageHandler {
    private LiMMessageHandler() {
    }

    private static class LiMMessageHandlerBinder {
        static final LiMMessageHandler handler = new LiMMessageHandler();
    }

    public static LiMMessageHandler getInstance() {
        return LiMMessageHandlerBinder.handler;
    }

    int sendMessage(INonBlockingConnection connection, LiMBaseMsg msg) {
        if (msg == null) {
            return 1;
        }
        byte[] bytes;
        if (msg.packetType == LiMMsgType.CONNECT) {
            // 连接
            bytes = MessageConvertHandler.getInstance().getConnectMsgBytes((LiMConnectMsg) msg);
        } else if (msg.packetType == LiMMsgType.REVACK) {
            // 收到消息回执
            bytes = MessageConvertHandler.getInstance().getReceivedAckMsgBytes((LiMReceivedAckMsg) msg);
        } else if (msg.packetType == LiMMsgType.SEND) {
            // 发送聊天消息
            bytes = MessageConvertHandler.getInstance().getSendMsgBytes((LiMSendMsg) msg);
        } else if (msg.packetType == LiMMsgType.PING) {
            // 发送心跳
            bytes = MessageConvertHandler.getInstance().getPingMsgBytes((LiMPingMsg) msg);
        } else {
            // 其他消息
            LiMLoggerUtils.getInstance().e("未知消息类型");
            return 1;
        }

        if (connection != null && connection.isOpen()) {
            try {
                connection.write(bytes, 0, bytes.length);
                connection.flush();
                return 1;
            } catch (BufferOverflowException e) {
                e.printStackTrace();
                LiMLoggerUtils.getInstance().e("sendMessages Exception BufferOverflowException"
                        + e.getMessage());
                return 0;
            } catch (ClosedChannelException e) {
                e.printStackTrace();
                LiMLoggerUtils.getInstance().e("sendMessages Exception ClosedChannelException"
                        + e.getMessage());
                return 0;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                LiMLoggerUtils.getInstance().e("sendMessages Exception SocketTimeoutException"
                        + e.getMessage());
                return 0;
            } catch (IOException e) {
                e.printStackTrace();
                LiMLoggerUtils.getInstance().e("sendMessages Exception IOException" + e.getMessage());
                return 0;
            }
        } else {
            LiMLoggerUtils.getInstance().e("sendMessages Exception sendMessage conn null:"
                    + connection);
            return 0;
        }
    }


    private List<LiMSyncMsg> receivedMsgList;
    private byte[] cacheData = null;

    synchronized void cutAcceptMsg(int available_len, byte[] available_bytes,
                                   IReceivedMsgListener mIReceivedMsgListener) {
        if (available_len == -1) {
            return;
        }
        if (cacheData == null || cacheData.length == 0) cacheData = available_bytes;
        else {
            //如果上次还存在未解析完的消息将新数据追加到缓存数据中
            byte[] temp = new byte[available_bytes.length + cacheData.length];
            try {
                System.arraycopy(cacheData, 0, temp, 0, cacheData.length);
                System.arraycopy(available_bytes, 0, temp, cacheData.length, available_bytes.length);
                cacheData = temp;
            } catch (Exception e) {
                LiMLoggerUtils.getInstance().e("多条消息合并错误" + e.getMessage());
            }

        }
        byte[] lastMsgBytes = cacheData;
        int readLength = 0;

        while (lastMsgBytes.length > 0 && readLength != lastMsgBytes.length) {

            readLength = lastMsgBytes.length;
            int packetType = LiMTypeUtils.getInstance().getHeight4(lastMsgBytes[0]);
            // 是否不持久化：0。 是否显示红点：1。是否只同步一次：0
            //是否持久化[是否保存在数据库]
            int no_persist = LiMTypeUtils.getInstance().getBit(lastMsgBytes[0], 0);
            //是否显示红点
            int red_dot = LiMTypeUtils.getInstance().getBit(lastMsgBytes[0], 1);
            //是否只同步一次
            int sync_once = LiMTypeUtils.getInstance().getBit(lastMsgBytes[0], 2);
            LiMLoggerUtils.getInstance().e("是否持久化：" + no_persist + "是否显示红点：" + red_dot + "是否只同步一次：" + sync_once);
            if (packetType == LiMMsgType.PONG) {
                //心跳ack
                mIReceivedMsgListener.heartbeatMsg(new LiMPongMsg());
                LiMLoggerUtils.getInstance().e("心跳ack:--->");
                //lastMsgBytes = Arrays.copyOfRange(lastMsgBytes, 1, lastMsgBytes.length);
                byte[] bytes = Arrays.copyOfRange(lastMsgBytes, 1, lastMsgBytes.length);
                cacheData = lastMsgBytes = bytes;

            } else {
                if (packetType < 10) {
                    // TODO: 2019-12-21 计算剩余长度
                    if (lastMsgBytes.length < 5) {
                        cacheData = lastMsgBytes;
                        break;
                    }
                    //其他消息类型
                    //int remainingLength = TypeUtils.getInstance().getReminLength(Arrays.copyOfRange(lastMsgBytes, 1, lastMsgBytes.length));
                    int remainingLength = LiMTypeUtils.getInstance().getRemainingLength(Arrays.copyOfRange(lastMsgBytes, 1, lastMsgBytes.length));
                    if (remainingLength == -1) {
                        //剩余长度被分包
                        cacheData = lastMsgBytes;
                        break;
                    }
                    if (remainingLength > 1 << 21) {
                        cacheData = null;
                        break;
                    }
                    byte[] bytes = LiMTypeUtils.getInstance().getRemainingLengthByte(remainingLength);
                    if (remainingLength + 1 + bytes.length > lastMsgBytes.length) {
                        //半包情况
                        cacheData = lastMsgBytes;
                    } else {
                        byte[] msg = Arrays.copyOfRange(lastMsgBytes, 0, remainingLength + 1 + bytes.length);
                        acceptMsg(msg, no_persist, sync_once, red_dot, mIReceivedMsgListener);
                        byte[] temps = Arrays.copyOfRange(lastMsgBytes, msg.length, lastMsgBytes.length);
                        cacheData = lastMsgBytes = temps;
                    }

                } else {
                    cacheData = null;
                    mIReceivedMsgListener.reconnect();
                    break;
                }
            }
        }
        saveReceiveMsg();
    }

    private void acceptMsg(byte[] bytes, int no_persist, int sync_once, int red_dot,
                           IReceivedMsgListener mIReceivedMsgListener) {
        // 字节数组转成消息数组
        if (bytes != null && bytes.length > 0) {
            LiMBaseMsg g_msg;
            g_msg = MessageConvertHandler.getInstance().cutBytesToMsg(bytes);
            if (g_msg != null) {
                //连接ack
                if (g_msg.packetType == LiMMsgType.CONNACK) {
                    LiMConnectAckMsg loginStatusMsg = (LiMConnectAckMsg) g_msg;
                    mIReceivedMsgListener.loginStatusMsg(loginStatusMsg.reasonCode);
                } else if (g_msg.packetType == LiMMsgType.SENDACK) {
                    //发送ack
                    LiMSendAckMsg talkSendStatus = (LiMSendAckMsg) g_msg;
                    int count = LiMMsgDbManager.getInstance().updateMsgSendStatus(talkSendStatus.clientSeq, talkSendStatus.messageSeq, talkSendStatus.messageID, talkSendStatus.reasonCode);
                    if (count > 0) {
                        LiMConversationDbManager.getInstance().updateMsgStatus(talkSendStatus.clientSeq, talkSendStatus.reasonCode);
                    }

                    LiMaoIM.getInstance().getLiMMsgManager().setSendMsgAck(talkSendStatus.clientSeq, talkSendStatus.messageID, talkSendStatus.messageSeq, talkSendStatus.reasonCode);

                    mIReceivedMsgListener
                            .sendAckMsg(talkSendStatus);
                } else if (g_msg.packetType == LiMMsgType.RECVEIVED) {
                    //收到消息
                    LiMMsg message = MessageConvertHandler.getInstance().baseMsg2LimMsg(g_msg);
                    message.no_persist = no_persist == 1;
                    message.red_dot = red_dot == 1;
                    message.sync_once = sync_once == 1;
                    handleReceiveMsg(message);
                    // mIReceivedMsgListener.receiveMsg(message);
                } else if (g_msg.packetType == LiMMsgType.DISCONNECT) {
                    //被踢消息
                    LiMDisconnectMsg liMDisconnectMsg = (LiMDisconnectMsg) g_msg;
                    mIReceivedMsgListener.kickMsg(liMDisconnectMsg);
                } else if (g_msg.packetType == LiMMsgType.PONG) {
                    mIReceivedMsgListener.heartbeatMsg((LiMPongMsg) g_msg);
                }
            }
        }
    }

    private void handleReceiveMsg(LiMMsg message) {
        message = parsingMsg(message);
        if (!message.no_persist) {
            addReceivedMsg(message);
        } else {
            //无需保存的消息直接push给UI层
            LiMaoIM.getInstance().getLiMMsgManager().pushNewMsg(message);
        }
    }

    private synchronized void addReceivedMsg(LiMMsg liMMsg) {
        liMMsg.status = LiMSendMsgResult.send_success;
        if (receivedMsgList == null) receivedMsgList = new ArrayList<>();
        LiMSyncMsg liMSyncMsg = new LiMSyncMsg();
        liMSyncMsg.no_persist = liMMsg.no_persist ? 1 : 0;
        liMSyncMsg.sync_once = liMMsg.sync_once ? 1 : 0;
        liMSyncMsg.red_dot = liMMsg.red_dot ? 1 : 0;
        liMSyncMsg.liMMsg = liMMsg;
        receivedMsgList.add(liMSyncMsg);
    }

    public synchronized void saveReceiveMsg() {

        if (receivedMsgList != null && receivedMsgList.size() > 0) {
            saveSyncMsg(receivedMsgList);

            List<LiMReceivedAckMsg> list = new ArrayList<>();
            for (int i = 0, size = receivedMsgList.size(); i < size; i++) {
                LiMReceivedAckMsg liMReceivedAckMsg = new LiMReceivedAckMsg();
                liMReceivedAckMsg.messageID = receivedMsgList.get(i).liMMsg.messageID;
                liMReceivedAckMsg.messageSeq = receivedMsgList.get(i).liMMsg.messageSeq;
                liMReceivedAckMsg.no_persist = receivedMsgList.get(i).no_persist == 1;
                liMReceivedAckMsg.red_dot = receivedMsgList.get(i).red_dot == 1;
                liMReceivedAckMsg.sync_once = receivedMsgList.get(i).red_dot == 1;
                list.add(liMReceivedAckMsg);
            }
            sendAck(list);
            receivedMsgList.clear();
        }
    }

    //回复消息ack
    private void sendAck(List<LiMReceivedAckMsg> list) {
        if (list.size() == 1) {
            LiMConnectionHandler.getInstance().sendMessage(list.get(0));
            return;
        }
        final Timer sendAckTimer = new Timer();
        sendAckTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (list.size() > 0) {
                    LiMConnectionHandler.getInstance().sendMessage(list.get(0));
                    list.remove(0);
                } else {
                    sendAckTimer.cancel();
                }
            }
        }, 0, 100);
    }


    /**
     * 保存同步消息
     *
     * @param list 同步消息对象
     */
    public synchronized void saveSyncMsg(List<LiMSyncMsg> list) {
        List<LiMMsg> liMMsgList = new ArrayList<>();
        try {
            //先将所有消息插入数据库
            if (list.size() > 0) {
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .beginTransaction();
                for (int i = 0, size = list.size(); i < size; i++) {
                    //将要存库的最后一条消息更新到会话记录表
                    if (list.get(i).no_persist == 0 && list.get(i).sync_once == 0) {
                        long row = LiMMsgDbManager.getInstance().insertMsg(list.get(i).liMMsg);
                        if (row > 0) {
                            liMMsgList.add(list.get(i).liMMsg);
                        }
                    }
                }
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .setTransactionSuccessful();
            }
        } catch (Exception ignored) {
        } finally {
            if (list.size() > 0 && LiMaoIMApplication.getInstance().getDbHelper().getDb().inTransaction()) {
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .endTransaction();
            }
        }
        //将消息push给UI
        LiMaoIM.getInstance().getLiMMsgManager().pushNewMsg(liMMsgList);
        groupMsg(list);
    }

    private void groupMsg(List<LiMSyncMsg> list) {
        LinkedHashMap<String, SavedLiMMsg> savedList = new LinkedHashMap<>();
        //再将消息分组
        for (int i = 0, size = list.size(); i < size; i++) {
            LiMMsg lastMsg = null;
            int count;

            if (list.get(i).liMMsg.channelType == LiMChannelType.PERSONAL) {
                //如果是单聊先将channelId改成发送者ID
                if (!TextUtils.isEmpty(list.get(i).liMMsg.channelID) && !TextUtils.isEmpty(list.get(i).liMMsg.fromUID) && list.get(i).liMMsg.channelID.equals(LiMaoIMApplication.getInstance().getUid())) {
                    list.get(i).liMMsg.channelID = list.get(i).liMMsg.fromUID;
                }
            }

            //将要存库的最后一条消息更新到会话记录表
            if (list.get(i).no_persist == 0
                    && list.get(i).liMMsg.type != LiMMsgContentType.LIMAO_INSIDE_MSG
                    && list.get(i).liMMsg.isDeleted == 0) {
                lastMsg = list.get(i).liMMsg;
            }
            count = list.get(i).red_dot;
            if (lastMsg == null) {
//                Log.e("消息不存在", "---->");
                continue;
            }
            JSONObject jsonObject = null;
            if (!TextUtils.isEmpty(list.get(i).liMMsg.content)) {
                try {
                    jsonObject = new JSONObject(list.get(i).liMMsg.content);
                } catch (JSONException e) {
                    e.printStackTrace();
                    jsonObject = new JSONObject();
                }
            }
            lastMsg.baseContentMsgModel = LiMaoIM.getInstance().getLiMMsgManager().getMsgContentModel(lastMsg.type, jsonObject);
            boolean isSave = false;
            if (lastMsg.baseContentMsgModel != null && lastMsg.baseContentMsgModel.mention_all == 1 && list.get(i).red_dot == 1) {
                isSave = true;
            } else {
                if (lastMsg.baseContentMsgModel != null && lastMsg.baseContentMsgModel.mentionInfo != null && lastMsg.baseContentMsgModel.mentionInfo.uids.size() > 0 && count == 1) {
                    for (int j = 0, len = lastMsg.baseContentMsgModel.mentionInfo.uids.size(); j < len; j++) {
                        if (!TextUtils.isEmpty(lastMsg.baseContentMsgModel.mentionInfo.uids.get(j)) && !TextUtils.isEmpty(LiMaoIMApplication.getInstance().getUid()) && lastMsg.baseContentMsgModel.mentionInfo.uids.get(j).equalsIgnoreCase(LiMaoIMApplication.getInstance().getUid())) {
                            isSave = true;
                        }
                    }
                }
            }
            if (isSave) {
                //如果存在艾特情况直接将消息存储
                LiMConversationDbManager.getInstance().saveOrUpdateTopMsg(lastMsg, count, true);
                continue;
            }

            SavedLiMMsg savedLiMMsg = null;
            if (savedList.containsKey(lastMsg.channelID + "_" + lastMsg.channelType)) {
                savedLiMMsg = savedList.get(lastMsg.channelID + "_" + lastMsg.channelType);
            }
            if (savedLiMMsg == null) {
                savedLiMMsg = new SavedLiMMsg(lastMsg, count);
            } else {
                savedLiMMsg.liMMsg = lastMsg;
                savedLiMMsg.redDot = savedLiMMsg.redDot + count;
            }
            savedList.put(lastMsg.channelID + "_" + lastMsg.channelType, savedLiMMsg);
        }

        List<LiMUIConversationMsg> refreshList = new ArrayList<>();
        // TODO: 4/27/21 这里未开事物是因为消息太多太快。事物来不及关闭
        for (Map.Entry<String, SavedLiMMsg> entry : savedList.entrySet()) {
            LiMUIConversationMsg liMUIConversationMsg = LiMConversationDbManager.getInstance().saveOrUpdateMsg(entry.getValue().liMMsg, entry.getValue().redDot, false);
            if (liMUIConversationMsg != null) {
                refreshList.add(liMUIConversationMsg);
            }
        }
        for (int i = 0, size = refreshList.size(); i < size; i++) {
            LiMConversationManager.getInstance().setOnRefreshMsg(refreshList.get(i), i == refreshList.size() - 1);
        }
    }

    public LiMMsg parsingMsg(LiMMsg message) {
        JSONObject json = null;
        try {
            if (TextUtils.isEmpty(message.content)) return message;
            json = new JSONObject(message.content);
            if (json.has(LiMDBColumns.LiMMessageColumns.type)) {
                message.content = json.toString();
                message.type = json.optInt(LiMDBColumns.LiMMessageColumns.type);
            }
            if (TextUtils.isEmpty(message.fromUID)) {
                if (json.has(LiMDBColumns.LiMMessageColumns.from_uid)) {
                    message.fromUID = json.optString(LiMDBColumns.LiMMessageColumns.from_uid);
                } else {
                    message.fromUID = message.channelID;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            LiMLoggerUtils.getInstance().e("operRecvMsg JSONException:" + e.getMessage());
        }

        if (json == null) {
            LiMLoggerUtils.getInstance().e("消息json为null----->");
            return message;
        }

        if (message.type == LiMMsgContentType.LIMAO_INSIDE_MSG) {
            Log.e("收到cmd消息",json.toString()+"_"+message.channelID);
            LiMCMDManager.getInstance().handleCMD(json, message.channelID, message.channelType);
        }
        message.baseContentMsgModel = LiMaoIM.getInstance().getLiMMsgManager().getMsgContentModel(message.type, json);
//        if (message.baseContentMsgModel != null)
//            message.searchableWord = message.baseContentMsgModel.getSearchableWord();

        //如果是单聊先将channelId改成发送者ID
        if (!TextUtils.isEmpty(message.channelID)
                && !TextUtils.isEmpty(message.fromUID)
                && message.channelType == LiMChannelType.PERSONAL
                && message.channelID.equals(LiMaoIMApplication.getInstance().getUid())) {
            message.channelID = message.fromUID;
        }
        return message;
    }

    public void updateLastSendingMsgFail() {
        LiMMsgDbManager.getInstance().updateAllMsgSendFail();
    }


}
