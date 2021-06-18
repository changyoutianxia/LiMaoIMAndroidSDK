package com.xinbida.limaoim.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.entity.LiMChannel;
import com.xinbida.limaoim.entity.LiMChannelMember;
import com.xinbida.limaoim.entity.LiMMessageGroupByDate;
import com.xinbida.limaoim.entity.LiMMessageSearchResult;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.entity.LiMMsgReaction;
import com.xinbida.limaoim.entity.LiMSyncExtraMsg;
import com.xinbida.limaoim.interfaces.IGetOrSyncHistoryMsgBack;
import com.xinbida.limaoim.manager.LiMMsgManager;
import com.xinbida.limaoim.message.type.LiMChannelType;
import com.xinbida.limaoim.message.type.LiMSendMsgResult;
import com.xinbida.limaoim.protocol.LiMMessageContent;
import com.xinbida.limaoim.utils.LiMLoggerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 5/21/21 12:20 PM
 * 消息管理
 */
public class LiMMsgDbManager {
    private LiMMsgDbManager() {
    }

    private static class LiMMsgDbManagerBinder {
        static final LiMMsgDbManager db = new LiMMsgDbManager();
    }

    public static LiMMsgDbManager getInstance() {
        return LiMMsgDbManagerBinder.db;
    }

    private int requestCount;

    public void getOrSyncHistoryMessages(String channelId, byte channelType, long oldestOrderSeq, boolean contain, boolean dropDown, int limit, final IGetOrSyncHistoryMsgBack iGetOrSyncHistoryMsgBack) {
        //获取原始数据
        List<LiMMsg> list = getMessages(channelId, channelType, oldestOrderSeq, contain, dropDown, limit);
        //业务判断数据
        List<LiMMsg> tempList = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            tempList.add(list.get(i));
        }
        //先通过message_seq排序
        if (tempList.size() > 0)
            Collections.sort(tempList, (o1, o2) -> o1.messageSeq - o2.messageSeq);
        //获取最大和最小messageSeq
        long minMessageSeq = 0;
        long maxMessageSeq = 0;
        for (int i = 0, size = tempList.size(); i < size; i++) {
            if (tempList.get(i).messageSeq != 0) {
                if (minMessageSeq == 0) minMessageSeq = tempList.get(i).messageSeq;
                if (tempList.get(i).messageSeq > maxMessageSeq)
                    maxMessageSeq = tempList.get(i).messageSeq;
                if (tempList.get(i).messageSeq < minMessageSeq)
                    minMessageSeq = tempList.get(i).messageSeq;
            }
        }

        //是否同步消息
        boolean isSyncMsg = false;
        //reverse false：从区间大值开始拉取true：从区间小值开始拉取
        boolean reverse = false;
        //同步消息的最大messageSeq
        long syncMaxMsgSeq = 0;
        //同步消息最小messageSeq,
        long syncMinMsgSeq = 0;

        //判断页与页之间是否连续
        long oldestMsgSeq;
        //如果获取到的messageSeq为0说明oldestOrderSeq这条消息是本地消息则获取他上一条或下一条消息的messageSeq做为判断
        if (oldestOrderSeq % 1000 != 0)
            oldestMsgSeq = getMsgSeq(channelId, channelType, oldestOrderSeq, dropDown);
        else oldestMsgSeq = oldestOrderSeq / 1000;

        if (dropDown) {
            //下拉获取消息
            if (maxMessageSeq != 0 && oldestMsgSeq != 0 && oldestMsgSeq - maxMessageSeq > 1) {
                isSyncMsg = true;
                syncMaxMsgSeq = oldestMsgSeq;
                syncMinMsgSeq = maxMessageSeq;
                reverse = false;//区间大值开始获取
            }
        } else {
            //上拉获取消息
            if (minMessageSeq != 0 && oldestMsgSeq != 0 && minMessageSeq - oldestMsgSeq > 1) {
                isSyncMsg = true;
                syncMaxMsgSeq = minMessageSeq;
                syncMinMsgSeq = oldestMsgSeq;
                reverse = true;//区间小值开始获取
            }
        }

        if (!isSyncMsg) {
            //判断当前页是否连续
            for (int i = 0, size = tempList.size(); i < size; i++) {
                int nextIndex = i + 1;
                if (nextIndex < tempList.size()) {
                    if (tempList.get(nextIndex).messageSeq != 0 && tempList.get(i).messageSeq != 0 &&
                            tempList.get(nextIndex).messageSeq - tempList.get(i).messageSeq > 1) {
                        //判断该条消息是否被删除
                        int num = getDeletedCount(tempList.get(i).messageSeq, tempList.get(nextIndex).messageSeq, channelId, channelType);
                        if (num < (tempList.get(nextIndex).messageSeq - tempList.get(i).messageSeq) - 1) {
                            isSyncMsg = true;
                            syncMaxMsgSeq = tempList.get(nextIndex).messageSeq;
                            syncMinMsgSeq = tempList.get(i).messageSeq;
                            if (dropDown) reverse = false;//区间大值开始获取
                            else reverse = true;//区间小值开始获取
                            break;
                        }
                    }
                }
            }
        }

        if (!isSyncMsg) {
            if (minMessageSeq == 1) {
                requestCount = 0;
                iGetOrSyncHistoryMsgBack.onResult(list);
                return;
            }
        }

        //计算最后一页后是否还存在消息
        if (!isSyncMsg && tempList.size() < limit) {
            if (dropDown) {
                //如果下拉获取数据
                isSyncMsg = true;
                reverse = false;//从区间大值开始获取数据
                syncMinMsgSeq = 0;
                syncMaxMsgSeq = oldestMsgSeq;
            } else {
                //如果上拉获取数据
                isSyncMsg = true;
                reverse = true;//从区间小值开始获取数据
                syncMaxMsgSeq = 0;
                syncMinMsgSeq = maxMessageSeq;
            }
        }

        if (isSyncMsg && syncMaxMsgSeq != syncMinMsgSeq && syncMaxMsgSeq != 0 && requestCount < 5) {
            //同步消息
            requestCount++;
            LiMMsgManager.getInstance().setSyncChannelMsgListener(channelId, channelType, syncMinMsgSeq, syncMaxMsgSeq, limit, reverse, liMSyncChannelMsg -> {
                if (liMSyncChannelMsg != null && liMSyncChannelMsg.messages != null && liMSyncChannelMsg.messages.size() > 0) {
                    getOrSyncHistoryMessages(channelId, channelType, oldestOrderSeq, contain, dropDown, limit, iGetOrSyncHistoryMsgBack);
                } else {
                    Log.e("返回查询的数据", list.size() + "");
                    requestCount = 0;
                    iGetOrSyncHistoryMsgBack.onResult(list);
                }
            });
        } else {
            requestCount = 0;
            iGetOrSyncHistoryMsgBack.onResult(list);
        }

    }

    /**
     * 获取被删除的条数
     *
     * @param minMessageSeq 最大messageSeq
     * @param maxMessageSeq 最小messageSeq
     * @param channelID     频道ID
     * @param channelType   频道类型
     * @return 删除条数
     */
    private int getDeletedCount(long minMessageSeq, long maxMessageSeq, String channelID, byte channelType) {
        String sql = "select count(*) num from " + LiMDBTables.chat_msg_tab + " where " + LiMDBColumns.LiMMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " and " + LiMDBColumns.LiMMessageColumns.channel_type + "=" + channelType + " and " + LiMDBColumns.LiMMessageColumns.message_seq + ">" + minMessageSeq + " and " + LiMDBColumns.LiMMessageColumns.message_seq + "<" + maxMessageSeq + " and " + LiMDBColumns.LiMMessageColumns.is_deleted + "=1";
        Cursor cursor = null;
        int num = 0;
        try {
            cursor = LiMaoIMApplication
                    .getInstance()
                    .getDbHelper().rawQuery(sql);
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                num = cursor.getInt(cursor.getColumnIndex("num"));
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return num;
    }


    private List<LiMMsg> getMessages(String channelId, byte channelType, long oldestOrderSeq, boolean contain, boolean dropDown, int limit) {
        List<LiMMsg> liMMsgList = new ArrayList<>();
        String sql;
        if (oldestOrderSeq <= 0) {
            sql = "select * from " + LiMDBTables.chat_msg_tab + " where channel_id=" + "\"" + channelId + "\" and channel_type=" + channelType + " and " + LiMDBColumns.LiMMessageColumns.is_deleted + "=0 and type<>0 and type<>99 order by " + LiMDBColumns.LiMMessageColumns.order_seq + " desc limit 0," + limit;
        } else {
            if (dropDown) {
                sql = "select * from " + LiMDBTables.chat_msg_tab + " where channel_id=" + "\"" + channelId + "\" and channel_type=" + channelType + " and " + LiMDBColumns.LiMMessageColumns.is_deleted + "=0 and type<>0 and type<>99 and " + LiMDBColumns.LiMMessageColumns.order_seq + " < " + oldestOrderSeq + " order by " + LiMDBColumns.LiMMessageColumns.order_seq + " desc limit 0," + limit;
            } else {
                if (contain) {
                    sql = "select *  from " + LiMDBTables.chat_msg_tab + " where channel_id=" + "\"" + channelId + "\" and channel_type=" + channelType + " and " + LiMDBColumns.LiMMessageColumns.is_deleted + "=0 and type<>0 and type<>99 and " + LiMDBColumns.LiMMessageColumns.order_seq + " >= " + oldestOrderSeq + " order by " + LiMDBColumns.LiMMessageColumns.order_seq + " asc limit 0," + limit;
                } else {
                    sql = "select *  from " + LiMDBTables.chat_msg_tab + " where channel_id=" + "\"" + channelId + "\" and channel_type=" + channelType + " and " + LiMDBColumns.LiMMessageColumns.is_deleted + "=0 and type<>0 and type<>99 and " + LiMDBColumns.LiMMessageColumns.order_seq + " > " + oldestOrderSeq + " order by " + LiMDBColumns.LiMMessageColumns.order_seq + " asc limit 0," + limit;
                }
            }
        }
        Cursor cursor = null;
        List<String> messageIds = new ArrayList<>();
        List<String> fromUIDs = new ArrayList<>();
        try {
            cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql);
            if (cursor == null) {
                return liMMsgList;
            }
            LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(channelId, channelType);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMMsg liMMsg = serializeLiMMsg(cursor);
                liMMsg.setChannelInfo(liMChannel);
                messageIds.add(liMMsg.messageID);
                if (!TextUtils.isEmpty(liMMsg.fromUID))
                    fromUIDs.add(liMMsg.fromUID);
                if (channelType == LiMChannelType.GROUP) {
                    //查询群成员信息
                    LiMChannelMember member = LiMChannelMembersDbManager.getInstance().query(channelId, LiMChannelType.GROUP, liMMsg.fromUID);
                    liMMsg.setMemberOfFrom(member);
                }

//                LiMChannel fromLiMChannel = LiMChannelDbManager.getInstance().getChannel(liMMsg.fromUID, LiMChannelType.PERSONAL);
//                liMMsg.setFrom(fromLiMChannel);
                if (dropDown)
                    liMMsgList.add(0, liMMsg);
                else liMMsgList.add(liMMsg);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        //扩展消息
        List<LiMMsgReaction> list = LiMMsgReactionDBManager.getInstance().queryWithMessageIds(messageIds);
        if (list != null && list.size() > 0) {
            for (int i = 0, size = liMMsgList.size(); i < size; i++) {
                for (int j = 0, len = list.size(); j < len; j++) {
                    if (list.get(j).messageID.equals(liMMsgList.get(i).messageID)) {
                        if (liMMsgList.get(i).reactionList == null)
                            liMMsgList.get(i).reactionList = new ArrayList<>();
                        liMMsgList.get(i).reactionList.add(list.get(j));
                    }
                }
            }
        }
        //消息发送者信息
        List<LiMChannel> liMChannelList = LiMChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(fromUIDs, LiMChannelType.PERSONAL);
        if (liMChannelList != null && liMChannelList.size() > 0) {
            for (LiMChannel liMChannel : liMChannelList) {
                for (int i = 0, size = liMMsgList.size(); i < size; i++) {
                    if (!TextUtils.isEmpty(liMMsgList.get(i).fromUID) && liMMsgList.get(i).fromUID.equals(liMChannel.channelID)) {
                        liMMsgList.get(i).setFrom(liMChannel);
                    }
                }
            }
        }
        return liMMsgList;
    }

    public long getMaxOrderSeq(String channelID, byte channelType) {
        long maxOrderSeq = 0;
        String sql = "select * from " + LiMDBTables.chat_msg_tab + " where " + LiMDBColumns.LiMMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " and " + LiMDBColumns.LiMMessageColumns.channel_type + "=" + channelType + " order by " + LiMDBColumns.LiMMessageColumns.order_seq + " desc limit 1";
        try {
            if (LiMaoIMApplication.getInstance().getDbHelper() != null) {
                Cursor cursor = LiMaoIMApplication
                        .getInstance()
                        .getDbHelper()
                        .rawQuery(sql);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        LiMMsg liMMsg = serializeLiMMsg(cursor);
                        maxOrderSeq = liMMsg.orderSeq;
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return maxOrderSeq;
    }

    public synchronized int updateMsgSendStatus(long client_seq, long messae_seq, String message_id, int send_status) {

        String[] updateKey = new String[4];
        String[] updateValue = new String[4];

        updateKey[0] = LiMDBColumns.LiMMessageColumns.status;
        updateValue[0] = send_status + "";

        updateKey[1] = LiMDBColumns.LiMMessageColumns.message_id;
        updateValue[1] = message_id;

        updateKey[2] = LiMDBColumns.LiMMessageColumns.message_seq;
        updateValue[2] = messae_seq + "";

        LiMMsg liMMsg = getMsgWithClientSeq(client_seq);

        updateKey[3] = LiMDBColumns.LiMMessageColumns.order_seq;
        if (liMMsg != null)
            updateValue[3] = String.valueOf(LiMMsgManager.getInstance().getMessageOrderSeq(messae_seq, liMMsg.channelID, liMMsg.channelType));
        else updateValue[3] = "0";

        String where = LiMDBColumns.LiMMessageColumns.client_seq + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = client_seq + "";

        int row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where, whereValue);
        if (row > 0 && liMMsg != null) {
            liMMsg.status = send_status;
            liMMsg.messageID = message_id;
            liMMsg.messageSeq = (int) messae_seq;
            LiMaoIM.getInstance().getLiMMsgManager().setRefreshMsg(liMMsg, true);
        }
        return row;
    }

    public synchronized long insertMsg(LiMMsg msg) {
        boolean isSave = LiMaoIM.getInstance().getLiMMsgManager().setMessageStoreBeforeIntercept(msg);
        if (!isSave) {
            msg.isDeleted = 1;
        }
        //客户端id存在表示该条消息已存过库
        if (msg.clientSeq != 0) {
            updateMsg(msg);
            return msg.clientSeq;
        }
        if (!TextUtils.isEmpty(msg.clientMsgNO)) {
            LiMMsg tempMsg = getMsgWithClientMsgNo(msg.clientMsgNO);
            if (tempMsg != null) {
                msg.isDeleted = 1;
                msg.clientMsgNO = UUID.randomUUID().toString().replaceAll("-", "");
//                updateLiMMsg(msg);
//                return tempMsg.clientSeq;
            }
        }
        ContentValues cv = new ContentValues();
        try {
            cv = LiMSqlContentValues.getContentValuesWithLiMMsg(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long result = -1;
        try {
            result = LiMaoIMApplication.getInstance().getDbHelper()
                    .insert(LiMDBTables.chat_msg_tab, cv);
        } catch (Exception e) {
            LiMLoggerUtils.getInstance().e("插入数据库异常：" + e.getMessage());
        }

        return result;
    }

    public synchronized void updateMsg(LiMMsg msg) {
        String[] updateKey = new String[4];
        String[] updateValue = new String[4];
        updateKey[0] = LiMDBColumns.LiMMessageColumns.content;
        updateValue[0] = msg.content;

        updateKey[1] = LiMDBColumns.LiMMessageColumns.status;
        updateValue[1] = msg.status + "";

        updateKey[2] = LiMDBColumns.LiMMessageColumns.message_id;
        updateValue[2] = msg.messageID;

        updateKey[3] = LiMDBColumns.LiMMessageColumns.extra;
        if (msg.extraMap != null) {
            JSONObject jsonObject = new JSONObject();
            for (Object key : msg.extraMap.keySet()) {
                try {
                    jsonObject.put(key.toString(), msg.extraMap.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            updateValue[3] = jsonObject.toString();
        } else updateValue[3] = "";
        String where = LiMDBColumns.LiMMessageColumns.client_seq + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = msg.clientSeq + "";
        LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where, whereValue);

    }

    public LiMMsg getMsgWithClientMsgNo(String clientMsgNo) {
        LiMMsg liMMsg = null;
        String sql = "select * from " + LiMDBTables.chat_msg_tab + " where " + LiMDBColumns.LiMMessageColumns.client_msg_no + "=" + "\"" + clientMsgNo + "\"";
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                liMMsg = serializeLiMMsg(cursor);
            }
        }
        if (liMMsg != null)
            liMMsg.reactionList = LiMMsgReactionDBManager.getInstance().queryReactions(liMMsg.messageID);
        return liMMsg;
    }

    public LiMMsg getMsgWithClientSeq(long clientSeq) {
        LiMMsg liMMsg = null;
        String sql = "select * from " + LiMDBTables.chat_msg_tab + " where " + LiMDBColumns.LiMMessageColumns.client_seq + "=" + clientSeq;
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                liMMsg = serializeLiMMsg(cursor);
            }
        }
        if (liMMsg != null)
            liMMsg.reactionList = LiMMsgReactionDBManager.getInstance().queryReactions(liMMsg.messageID);
        return liMMsg;
    }

    public LiMMsg getMsgMaxOrderSeqWithChannel(String channelID, byte channelType) {
        String sql = "select * from " + LiMDBTables.chat_msg_tab + " where " + LiMDBColumns.LiMMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " and " + LiMDBColumns.LiMMessageColumns.channel_type + "=" + channelType + " and " + LiMDBColumns.LiMMessageColumns.is_deleted + "=0 and type<>0 and type<>99 order by " + LiMDBColumns.LiMMessageColumns.order_seq + " desc limit 1";
        Cursor cursor = null;
        LiMMsg liMMsg = null;
        try {
            cursor = LiMaoIMApplication
                    .getInstance()
                    .getDbHelper().rawQuery(sql);
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                liMMsg = serializeLiMMsg(cursor);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return liMMsg;
    }

    /**
     * 删除消息
     *
     * @param client_seq 消息客户端编号
     */
    public synchronized boolean deleteMsgWithClientSeq(long client_seq) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = LiMDBColumns.LiMMessageColumns.is_deleted;
        updateValue[0] = "1";
        String where = LiMDBColumns.LiMMessageColumns.client_seq + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = String.valueOf(client_seq);
        int row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            LiMMsg liMMsg = getMsgWithClientSeq(client_seq);
            if (liMMsg != null)
                LiMaoIM.getInstance().getLiMMsgManager().setDeleteMsg(liMMsg);
        }
        return row > 0;
    }

    public int getMsgRowNoWithOrderSeq(String channelID, byte channelType, long order_seq) {
        String sql = "select count(*) cn from " + LiMDBTables.chat_msg_tab + " where channel_id=" + "\"" + channelID + "\"" + " and channel_type=" + channelType + " and " + LiMDBColumns.LiMMessageColumns.type + "<>0 and " + LiMDBColumns.LiMMessageColumns.type + "<>99 and " + LiMDBColumns.LiMMessageColumns.order_seq + ">" + order_seq + " and " + LiMDBColumns.LiMMessageColumns.is_deleted + "=0 order by " + LiMDBColumns.LiMMessageColumns.order_seq + " desc";
        Cursor cursor = null;
        int rowNo = 0;
        try {
            cursor = LiMaoIMApplication
                    .getInstance()
                    .getDbHelper().rawQuery(sql);
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                rowNo = cursor.getInt(cursor.getColumnIndex("cn"));
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return rowNo;
    }

    public synchronized boolean deleteMsgWithMessageID(String messageID) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = LiMDBColumns.LiMMessageColumns.is_deleted;
        updateValue[0] = "1";
        String where = LiMDBColumns.LiMMessageColumns.message_id + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = messageID;
        int row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            LiMMsg liMMsg = getMsgWithMessageID(messageID);
            if (liMMsg != null)
                LiMaoIM.getInstance().getLiMMsgManager().setDeleteMsg(liMMsg);
        }
        return row > 0;

    }

    public LiMMsg updateMsgWithSyncExtraMsg(LiMSyncExtraMsg liMSyncExtraMsg) {
        LiMMsg liMMsg = getMsgWithMessageID(liMSyncExtraMsg.message_id);
        if (liMMsg == null || liMMsg.extraVersion > liMSyncExtraMsg.extra_version) return null;

        String[] update = new String[1];
        update[0] = liMSyncExtraMsg.message_id;
        ContentValues cv = new ContentValues();
        try {
            cv.put(LiMDBColumns.LiMMessageColumns.revoke, liMSyncExtraMsg.revoke);
            cv.put(LiMDBColumns.LiMMessageColumns.revoker, liMSyncExtraMsg.revoker);
            cv.put(LiMDBColumns.LiMMessageColumns.extra_version, liMSyncExtraMsg.extra_version);
            cv.put(LiMDBColumns.LiMMessageColumns.voice_status, liMSyncExtraMsg.voice_status);
            cv.put(LiMDBColumns.LiMMessageColumns.unread_count, liMSyncExtraMsg.unread_count);
            cv.put(LiMDBColumns.LiMMessageColumns.readed_count, liMSyncExtraMsg.readed_count);
            cv.put(LiMDBColumns.LiMMessageColumns.readed, liMSyncExtraMsg.readed);
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean result = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, cv, LiMDBColumns.LiMMessageColumns.message_id + "=?", update);
        if (result) {
            liMMsg.extraVersion = liMSyncExtraMsg.extra_version;
            liMMsg.revoker = liMSyncExtraMsg.revoker;
            liMMsg.revoke = liMSyncExtraMsg.revoke;
            liMMsg.readedCount = liMSyncExtraMsg.readed_count;
            liMMsg.readed = liMSyncExtraMsg.readed;
            liMMsg.unreadCount = liMSyncExtraMsg.unread_count;
            liMMsg.voiceStatus = liMSyncExtraMsg.voice_status;
        }
        return liMMsg;
    }

    public LiMMsg updateMsgReadWithMsgID(String messageID, int read) {
        String[] updateKey = new String[]{LiMDBColumns.LiMMessageColumns.readed};
        String[] updateValue = new String[]{String.valueOf(read)};
        String where = LiMDBColumns.LiMMessageColumns.message_id + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = messageID;
        int row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where, whereValue);
        LiMMsg liMMsg = null;
        if (row > 0) {
            liMMsg = getMsgWithMessageID(messageID);
        }
        return liMMsg;
    }
    /**
     * 查询按日期分组的消息数量
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return List<LiMMessageGroupByDate>
     */
    public List<LiMMessageGroupByDate> getMessageGroupByDateWithChannel(String channelID, byte channelType) {
        String sql = "SELECT DATE(" + LiMDBColumns.LiMMessageColumns.timestamp + ", 'unixepoch','localtime') AS days,COUNT(" + LiMDBColumns.LiMMessageColumns.client_msg_no + ") count,min(" + LiMDBColumns.LiMMessageColumns.order_seq + ") AS order_seq FROM " + LiMDBTables.chat_msg_tab + "  WHERE " + LiMDBColumns.LiMMessageColumns.channel_type + " = " + channelType + " and " + LiMDBColumns.LiMMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " GROUP BY " + LiMDBColumns.LiMMessageColumns.timestamp + "," + LiMDBColumns.LiMMessageColumns.order_seq + "";
        List<LiMMessageGroupByDate> list = new ArrayList<>();
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMMessageGroupByDate liMMsg = new LiMMessageGroupByDate();
                liMMsg.count = cursor.getLong(cursor.getColumnIndex("count"));
                liMMsg.orderSeq = cursor.getLong(cursor.getColumnIndex("order_seq"));
                liMMsg.date = cursor.getString(cursor.getColumnIndex("days"));
                list.add(liMMsg);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * 清空所有聊天消息
     */
    public synchronized void clearEmpty() {
        LiMaoIMApplication.getInstance().getDbHelper()
                .delete(LiMDBTables.chat_msg_tab, null, null);
    }
    /**
     * 获取某个类型的聊天数据
     *
     * @param type            消息类型
     * @param oldestClientSeq 最后一次消息客户端ID
     * @param limit           数量
     * @return list
     */
    public List<LiMMsg> getMessagesWithType(int type, long oldestClientSeq, int limit) {
        String sql;
        if (oldestClientSeq <= 0) {
            sql = "select * from " + LiMDBTables.chat_msg_tab + " where type =" + type + " order by " + LiMDBColumns.LiMMessageColumns.timestamp + " desc limit 0," + limit;
        } else
            sql = "select * from " + LiMDBTables.chat_msg_tab + " where type =" + type + " and " + LiMDBColumns.LiMMessageColumns.client_seq + " < " + oldestClientSeq + " order by " + LiMDBColumns.LiMMessageColumns.timestamp + " desc limit 0," + limit;
        List<LiMMsg> liMMsgList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql);
            if (cursor == null) {
                return liMMsgList;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMMsg liMMsg = serializeLiMMsg(cursor);
                if (liMMsg.channelType == LiMChannelType.GROUP) {
                    //查询群成员信息
                    LiMChannelMember member = LiMChannelMembersDbManager.getInstance().query(liMMsg.channelID, LiMChannelType.GROUP, liMMsg.fromUID);
                    liMMsg.setMemberOfFrom(member);
                    LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(liMMsg.fromUID, LiMChannelType.PERSONAL);
                    liMMsg.setFrom(liMChannel);
                } else {
                    LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(liMMsg.fromUID, LiMChannelType.PERSONAL);
                    liMMsg.setFrom(liMChannel);
                }
                liMMsgList.add(0, liMMsg);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return liMMsgList;
    }

    public List<LiMMsg> searchLiMMessageWithChannel(String channelID, byte channelType, String searchKey) {
        List<LiMMsg> liMMsgList = new ArrayList<>();
        String sql = "select * from " + LiMDBTables.chat_msg_tab + " WHERE searchable_word LIKE \"%" + searchKey + "%\" and " + LiMDBColumns.LiMMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " and " + LiMDBColumns.LiMMessageColumns.channel_type + "=" + channelType;
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return liMMsgList;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMMsg liMMsg = serializeLiMMsg(cursor);
                if (liMMsg.channelType == LiMChannelType.GROUP) {
                    //查询群成员信息
                    LiMChannelMember member = LiMChannelMembersDbManager.getInstance().query(liMMsg.channelID, LiMChannelType.GROUP, liMMsg.fromUID);
                    liMMsg.setMemberOfFrom(member);
                    LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(liMMsg.fromUID, LiMChannelType.PERSONAL);
                    liMMsg.setFrom(liMChannel);
                } else {
                    LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(liMMsg.fromUID, LiMChannelType.PERSONAL);
                    liMMsg.setFrom(liMChannel);
                }
                liMMsgList.add(0, liMMsg);
            }
        } catch (Exception ignored) {
        }
        return liMMsgList;
    }

    public List<LiMMessageSearchResult> searchLiMMessage(String searchKey) {
        List<LiMMessageSearchResult> list = new ArrayList<>();

        String sql = "select distinct c.*, count(*) message_count, case count(*) WHEN 1 then" +
                " m.client_seq else ''END client_seq, CASE count(*) WHEN 1 THEN m.searchable_word else '' end searchable_word " +
                "from " + LiMDBTables.channel_tab + " c LEFT JOIN " + LiMDBTables.chat_msg_tab + " m ON m.channel_id = c.channel_id and " +
                "m.channel_type = c.channel_type WHERE searchable_word LIKE  \"%" + searchKey + "%\" GROUP BY " +
                "c.channel_id, c.channel_type ORDER BY m.created_at DESC limit 100";
        Cursor cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql);
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            LiMChannel liMChannel = LiMChannelDBManager.getInstance().serializableChannel(cursor);
            LiMMessageSearchResult result = new LiMMessageSearchResult();
            result.liMChannel = liMChannel;
            result.messageCount = cursor.getInt(cursor.getColumnIndex("message_count"));
            result.searchableWord = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.searchable_word));
            list.add(result);
        }
        cursor.close();
        return list;
    }

    /**
     * 删除某个会话
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     */
    public synchronized boolean deleteMsgWithChannel(String channelId, byte channelType) {
        String where = LiMDBColumns.LiMMessageColumns.channel_id + "=? and " + LiMDBColumns.LiMMessageColumns.channel_type + "=?";
        String[] whereValue = new String[2];
        whereValue[0] = channelId + "";
        whereValue[1] = channelType + "";
        return LiMaoIMApplication.getInstance().getDbHelper()
                .delete(LiMDBTables.chat_msg_tab, where, whereValue);
    }

    /**
     * 查询固定类型的消息记录
     *
     * @param channelID      频道ID
     * @param channelType    频道类型
     * @param oldestOrderSeq 排序编号
     * @param limit          查询数量
     * @param contentTypes   内容类型
     * @return List<LiMMsg>
     */
    public List<LiMMsg> searchChatMsgWithChannelAndTypes(String channelID, byte channelType, long oldestOrderSeq, int limit, int[] contentTypes) {
        if (TextUtils.isEmpty(channelID) || contentTypes == null || contentTypes.length == 0) {
            return null;
        }
        String whereStr = "";
        for (int contentType : contentTypes) {
            if (TextUtils.isEmpty(whereStr)) {
                whereStr = LiMDBColumns.LiMMessageColumns.type + "=" + contentType;
            } else {
                whereStr = " OR " + LiMDBColumns.LiMMessageColumns.type + "=" + contentType;
            }
        }
        String sql;
        if (oldestOrderSeq <= 0) {
            sql = "select * from " + LiMDBTables.chat_msg_tab
                    + " WHERE " + LiMDBColumns.LiMMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " and "
                    + LiMDBColumns.LiMMessageColumns.channel_type + "=" + channelType
                    + " and " + LiMDBColumns.LiMMessageColumns.is_deleted
                    + "=0 and type<>0 and type<>99 and (" + whereStr + ")"
                    + " order by " + LiMDBColumns.LiMMessageColumns.order_seq + " desc limit 0," + limit;
        } else {
            sql = "select * from " + LiMDBTables.chat_msg_tab
                    + " WHERE " + LiMDBColumns.LiMMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " and "
                    + LiMDBColumns.LiMMessageColumns.channel_type + "=" + channelType
                    + " and " + LiMDBColumns.LiMMessageColumns.is_deleted
                    + "=0 and type<>0 and type<>99 and (" + whereStr + ") and"
                    + LiMDBColumns.LiMMessageColumns.order_seq + "<" + oldestOrderSeq + " order by " + LiMDBColumns.LiMMessageColumns.order_seq + " desc limit 0," + limit;
        }

        List<LiMMsg> liMMsgList = new ArrayList<>();
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return liMMsgList;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMMsg liMMsg = serializeLiMMsg(cursor);
                if (liMMsg.channelType == LiMChannelType.GROUP) {
                    //查询群成员信息
                    LiMChannelMember member = LiMChannelMembersDbManager.getInstance().query(liMMsg.channelID, LiMChannelType.GROUP, liMMsg.fromUID);
                    liMMsg.setMemberOfFrom(member);
                    LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(liMMsg.fromUID, LiMChannelType.PERSONAL);
                    liMMsg.setFrom(liMChannel);
                } else {
                    LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(liMMsg.fromUID, LiMChannelType.PERSONAL);
                    liMMsg.setFrom(liMChannel);
                }
                liMMsgList.add(liMMsg);
            }
        } catch (Exception ignored) {
        }
        return liMMsgList;
    }

    /**
     * 获取最大扩展编号消息
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return LiMMsg
     */
    public LiMMsg getMsgMaxExtraVersionWithChannel(String channelID, byte channelType) {
        String sql = "select * from " + LiMDBTables.chat_msg_tab + " where " + LiMDBColumns.LiMMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " and " + LiMDBColumns.LiMMessageColumns.channel_type + "=" + channelType + " and type<>0 and type<>99 order by " + LiMDBColumns.LiMMessageColumns.extra_version + " desc limit 1";
        Cursor cursor = null;
        LiMMsg liMMsg = null;
        try {
            cursor = LiMaoIMApplication
                    .getInstance()
                    .getDbHelper().rawQuery(sql);
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                liMMsg = serializeLiMMsg(cursor);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return liMMsg;
    }

    public synchronized boolean updateMsgWithClientMsgNo(String clientMsgNo, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = LiMDBColumns.LiMMessageColumns.client_msg_no + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = clientMsgNo;
        int row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            LiMMsg liMMsg = getMsgWithClientMsgNo(clientMsgNo);
            if (liMMsg != null)
                LiMaoIM.getInstance().getLiMMsgManager().setRefreshMsg(liMMsg, true);
        }
        return row > 0;
    }

    public synchronized boolean updateMsgWithMessageID(String messageID, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = LiMDBColumns.LiMMessageColumns.message_id + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = messageID;
        int row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            LiMMsg liMMsg = getMsgWithMessageID(messageID);
            if (liMMsg != null)
                LiMaoIM.getInstance().getLiMMsgManager().setRefreshMsg(liMMsg, true);
        }
        return row > 0;

    }


    public LiMMsg getMsgWithMessageID(String messageID) {
        LiMMsg liMMsg = null;
        String sql = "select * from " + LiMDBTables.chat_msg_tab + " where " + LiMDBColumns.LiMMessageColumns.message_id + "=" + messageID;
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                liMMsg = serializeLiMMsg(cursor);
            }
        }
        if (liMMsg != null)
            liMMsg.reactionList = LiMMsgReactionDBManager.getInstance().queryReactions(liMMsg.messageID);
        return liMMsg;
    }


    public long getMaxMessageSeq(String channelID, byte channelType) {
        String sql = "select * from " + LiMDBTables.chat_msg_tab + " where " + LiMDBColumns.LiMMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " and " + LiMDBColumns.LiMMessageColumns.channel_type + "=" + channelType + " order by " + LiMDBColumns.LiMMessageColumns.message_seq + " desc limit 1";
        long messageSeq = 0;
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                LiMMsg liMMsg = serializeLiMMsg(cursor);
                messageSeq = liMMsg.messageSeq;
            }
        }
        return messageSeq;
    }

    private long getMsgSeq(String channelID, byte channelType, long oldestOrderSeq, boolean dropDown) {
        String sql;
        long messageSeq = 0;
        if (!dropDown) {
            sql = "select * from " + LiMDBTables.chat_msg_tab + " where channel_id=" + "\"" + channelID + "\"" + " and channel_type=" + channelType + " and  order_seq>" + oldestOrderSeq + " and message_seq<>0 order by message_seq desc limit 1";
        } else
            sql = "select * from " + LiMDBTables.chat_msg_tab + " where channel_id=" + "\"" + channelID + "\"" + " and channel_type=" + channelType + " and  order_seq<" + oldestOrderSeq + " and message_seq<>0 order by message_seq asc limit 1";
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                LiMMsg liMMsg = serializeLiMMsg(cursor);
                messageSeq = liMMsg.messageSeq;
            }
        }
        return messageSeq;
    }

    public List<LiMMsg> queryWithMsgIds(List<String> messageIds) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0, size = messageIds.size(); i < size; i++) {
            if (!TextUtils.isEmpty(stringBuffer)) {
                stringBuffer.append(",");
            }
            stringBuffer.append(messageIds.get(i));
        }
        String sql = "select * from " + LiMDBTables.chat_msg_tab + " where " + LiMDBColumns.LiMMessageColumns.message_id + " in (" + stringBuffer + ")";
        List<LiMMsg> list = new ArrayList<>();
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMMsg liMMsg = serializeLiMMsg(cursor);

                if (liMMsg.channelType == LiMChannelType.GROUP) {
                    //查询群成员信息
                    LiMChannelMember member = LiMChannelMembersDbManager.getInstance().query(liMMsg.channelID, LiMChannelType.GROUP, liMMsg.fromUID);
                    liMMsg.setMemberOfFrom(member);
                    LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(liMMsg.channelID, LiMChannelType.GROUP);
                    liMMsg.setChannelInfo(liMChannel);
                } else {
                    LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(liMMsg.channelID, LiMChannelType.PERSONAL);
                    liMMsg.setChannelInfo(liMChannel);
                }
                LiMChannel liMChannel = LiMChannelDBManager.getInstance().getChannel(liMMsg.fromUID, LiMChannelType.PERSONAL);
                liMMsg.setFrom(liMChannel);

                list.add(liMMsg);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * 删除消息
     *
     * @param client_msg_no 消息ID
     */
    public synchronized LiMMsg deleteMsgWithClientMsgNo(String client_msg_no) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = LiMDBColumns.LiMMessageColumns.is_deleted;
        updateValue[0] = "1";
        String where = LiMDBColumns.LiMMessageColumns.client_msg_no + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = client_msg_no;
        LiMMsg liMMsg = null;
        int row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            liMMsg = getMsgWithClientMsgNo(client_msg_no);
        }
        return liMMsg;
    }

    public long getMaxSeqWithChannel(String channelID, byte channelType) {
        return LiMMsgReactionDBManager.getInstance().getMaxSeqWithChannel(channelID, channelType);
    }

    public void saveMsgReaction(List<LiMMsgReaction> list) {
        LiMMsgReactionDBManager.getInstance().insertReaction(list);
    }

    public List<LiMMsgReaction> queryMsgReactionWithMsgIds(List<String> messageIds) {
        return LiMMsgReactionDBManager.getInstance().queryWithMessageIds(messageIds);
    }

    public synchronized void updateAllMsgSendFail() {
        String[] updateKey = new String[1];
        updateKey[0] = LiMDBColumns.LiMMessageColumns.status;
        String[] updateValue = new String[1];
        updateValue[0] = LiMSendMsgResult.send_fail + "";
        String where = LiMDBColumns.LiMMessageColumns.status + "=? ";
        String[] whereValue = new String[1];
        whereValue[0] = "0";

        try {
            if (LiMaoIMApplication.getInstance().getDbHelper() != null) {
                LiMaoIMApplication
                        .getInstance()
                        .getDbHelper()
                        .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where,
                                whereValue);
                LiMConversationDbManager.getInstance().updateSendingMsgfail();
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized void updateMsgStatus(long client_seq, int status) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = LiMDBColumns.LiMMessageColumns.status;
        updateValue[0] = status + "";

        String where = LiMDBColumns.LiMMessageColumns.client_seq + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = client_seq + "";

        int row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_tab, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            LiMMsg liMMsg = getMsgWithClientSeq(client_seq);
            if (liMMsg != null)
                LiMaoIM.getInstance().getLiMMsgManager().setRefreshMsg(liMMsg, true);
        }
    }

    public int getMaxMessageSeq() {
        int maxMessageSeq = 0;
        String sql = "select max(message_seq) message_seq from " + LiMDBTables.chat_msg_tab + " limit 0, 1";
        //String sql = "select * from " + LiMDBTables.chat_msg_tab + " order by " + LiMDBColumns.LiMMessageColumns.message_seq + " desc limit 1";
        try {
            if (LiMaoIMApplication.getInstance().getDbHelper() != null) {
                Cursor cursor = LiMaoIMApplication
                        .getInstance()
                        .getDbHelper()
                        .rawQuery(sql);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        maxMessageSeq = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.message_seq));
                        // LiMMsg liMMsg = serializeLiMMsg(cursor);
                        //   maxClientSeq = liMMsg.messageSeq;
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return maxMessageSeq;
    }

    private LiMMsg serializeLiMMsg(Cursor cursor) {
        LiMMsg liMMsg = new LiMMsg();
        liMMsg.messageID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.message_id));
        liMMsg.messageSeq = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.message_seq));
        liMMsg.clientSeq = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.client_seq));
        liMMsg.timestamp = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.timestamp));
        liMMsg.fromUID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.from_uid));
        liMMsg.channelID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.channel_id));
        liMMsg.channelType = (byte) cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.channel_type));
        liMMsg.type = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.type));
        liMMsg.content = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.content));
        liMMsg.status = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.status));
        liMMsg.voiceStatus = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.voice_status));
        liMMsg.createdAt = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.created_at));
        liMMsg.updatedAt = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.updated_at));
        liMMsg.searchableWord = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.searchable_word));
        liMMsg.clientMsgNO = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.client_msg_no));
        liMMsg.isDeleted = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.is_deleted));
        liMMsg.orderSeq = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.order_seq));
        liMMsg.revoke = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.revoke));
        liMMsg.revoker = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.revoker));
        liMMsg.extraVersion = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.extra_version));
        liMMsg.readedCount = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.readed_count));
        liMMsg.unreadCount = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.unread_count));
        liMMsg.receipt = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.receipt));
        liMMsg.readed = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.readed));
        String extra = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageColumns.extra));
        if (!TextUtils.isEmpty(extra)) {
            HashMap<String, Object> hashMap = new HashMap<>();
            try {
                JSONObject jsonObject = new JSONObject(extra);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    hashMap.put(key, jsonObject.opt(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            liMMsg.extraMap = hashMap;
        }
        //获取附件
        liMMsg.baseContentMsgModel = getMsgModel(liMMsg);
        if (liMMsg.baseContentMsgModel != null) {
            liMMsg.baseContentMsgModel.receipt = liMMsg.receipt;
        }

        //查询消息回应
        //   liMMsg.reactionList = LiMMsgReactionDBManager.getInstance().queryReactions(liMMsg.messageID);
        return liMMsg;
    }

    private LiMMessageContent getMsgModel(LiMMsg liMMsg) {
        JSONObject jsonObject = null;
        if (!TextUtils.isEmpty(liMMsg.content)) {
            try {
                jsonObject = new JSONObject(liMMsg.content);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return LiMaoIM.getInstance()
                .getLiMMsgManager().getMsgContentModel(liMMsg.type, jsonObject);
    }

}
