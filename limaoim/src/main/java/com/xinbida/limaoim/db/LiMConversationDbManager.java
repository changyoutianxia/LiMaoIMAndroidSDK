package com.xinbida.limaoim.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.entity.LiMChannel;
import com.xinbida.limaoim.entity.LiMChannelMember;
import com.xinbida.limaoim.entity.LiMConversationMsg;
import com.xinbida.limaoim.entity.LiMMentionType;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.entity.LiMReminder;
import com.xinbida.limaoim.entity.LiMUIConversationMsg;
import com.xinbida.limaoim.manager.LiMConversationManager;
import com.xinbida.limaoim.message.type.LiMChannelType;
import com.xinbida.limaoim.message.type.LiMSendMsgResult;
import com.xinbida.limaoim.utils.LiMLoggerUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 5/21/21 12:14 PM
 * 最近会话
 */
public class LiMConversationDbManager {
    private LiMConversationDbManager() {
    }

    private static class LiMConversationDbManagerBinder {
        static final LiMConversationDbManager db = new LiMConversationDbManager();
    }

    public static LiMConversationDbManager getInstance() {
        return LiMConversationDbManagerBinder.db;
    }

    public synchronized List<LiMUIConversationMsg> getAll() {
        List<LiMUIConversationMsg> list = new ArrayList<>();
        String sql = "SELECT " + LiMDBTables.chat_msg_conversation_tab + ".*,"
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.channel_remark + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.channel_name + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.top + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.mute + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.save + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.status + " as channel_status,"
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.forbidden + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.invite + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.follow + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.is_deleted + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.show_nick + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.avatar + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.online + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.last_offline + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.category + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.receipt + ","
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.version + " AS channel_version,"
                + LiMDBTables.channel_tab + "." + LiMDBColumns.LiMChannelColumns.extra + " AS channel_extra FROM "
                + LiMDBTables.chat_msg_conversation_tab + " LEFT JOIN "
                + LiMDBTables.channel_tab + " ON "
                + LiMDBTables.chat_msg_conversation_tab + ".channel_id = "
                + LiMDBTables.channel_tab + ".channel_id AND "
                + LiMDBTables.chat_msg_conversation_tab + ".channel_type = "
                + LiMDBTables.channel_tab + ".channel_type order by "
                + LiMDBColumns.LiMCoverMessageColumns.last_msg_timestamp + " desc";
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMConversationMsg msg = serializeLiMMsg(cursor);
                LiMUIConversationMsg uiMsg = new LiMUIConversationMsg();
                uiMsg.unreadCount = msg.unreadCount;
                uiMsg.mentionList = getReminders(msg.reminders);
                uiMsg.channelID = msg.channelID;
                uiMsg.channelType = msg.channelType;
                uiMsg.clientMsgNo = msg.lastClientMsgNO;
                uiMsg.lastMsgTimestamp = msg.lastMsgTimestamp;
                uiMsg.status = msg.status;
                LiMChannel liMChannel = LiMChannelDBManager.getInstance().serializableChannel(cursor);
                if (liMChannel != null) {
                    uiMsg.top = liMChannel.top;
                    uiMsg.mute = liMChannel.mute;
                    String extra = cursor.getString(cursor.getColumnIndex("channel_extra"));
                    liMChannel.extraMap = LiMChannelDBManager.getInstance().getChannelExtra(extra);
                    liMChannel.status = cursor.getInt(cursor.getColumnIndex("channel_status"));
                    liMChannel.version = cursor.getLong(cursor.getColumnIndex("channel_version"));
                    liMChannel.channelID = msg.channelID;
                    liMChannel.channelType = msg.channelType;
                    uiMsg.setLiMChannel(liMChannel);
                }
                list.add(uiMsg);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public long getMaxVersion() {
        long maxVersion = 0;
        String sql = "select max(version) version from " + LiMDBTables.chat_msg_conversation_tab + " limit 0, 1";
        Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                maxVersion = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.version));
            }
            cursor.close();
        }
        return maxVersion;
    }

    public synchronized LiMUIConversationMsg insertSyncMsg(LiMConversationMsg conversationMsg) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.channel_id, conversationMsg.channelID);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.channel_type, conversationMsg.channelType);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.from_uid, conversationMsg.fromUID);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.last_client_msg_no, conversationMsg.lastClientMsgNO);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.last_msg_timestamp, conversationMsg.lastMsgTimestamp);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.unread_count, conversationMsg.unreadCount);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.reminders, conversationMsg.reminders);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.extra, conversationMsg.getExtras());
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.version, conversationMsg.version);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.is_deleted, conversationMsg.isDeleted);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.status, conversationMsg.status);
        long messageSeq = getMsgBrowseTo(conversationMsg.channelID, conversationMsg.channelType);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.browse_to, messageSeq);
        long row = LiMaoIMApplication.getInstance().getDbHelper().insertSql(LiMDBTables.chat_msg_conversation_tab, contentValues);
        if (row > 0) {
            return getUIMsg(conversationMsg);
        }
        return null;
    }

    public synchronized String getLastMsgSeqs() {
        String lastMsgSeqs = "";
        String sql = "select GROUP_CONCAT(channel_id||':'||channel_type||':'||last_msg_seq,'|') synckey from (select *,(select max(message_seq) from chat_msg_tab where chat_msg_tab.channel_id=chat_msg_conversation_tab.channel_id and chat_msg_tab.channel_type=chat_msg_conversation_tab.channel_type limit 1) last_msg_seq from chat_msg_conversation_tab) cn where channel_id<>''";
        Cursor cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql);
        if (cursor == null) {
            return lastMsgSeqs;
        }
        if (cursor.moveToFirst()) {
            lastMsgSeqs = cursor.getString(cursor.getColumnIndex("synckey"));
        }
        cursor.close();

        return lastMsgSeqs;
    }

    public synchronized void updateMsg(String channelID, byte channelType, String clientMsgNo, int count, long clientSeq) {
        String[] update = new String[2];
        update[0] = channelID;
        update[1] = String.valueOf(channelType);
        ContentValues cv = new ContentValues();
        try {
            cv.put(LiMDBColumns.LiMCoverMessageColumns.client_seq, clientSeq);
            cv.put(LiMDBColumns.LiMCoverMessageColumns.last_client_msg_no, clientMsgNo);
            cv.put(LiMDBColumns.LiMCoverMessageColumns.unread_count, count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_conversation_tab, cv, LiMDBColumns.LiMCoverMessageColumns.channel_id + "=? and " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "=?", update);
        if (row) {
            LiMConversationMsg conversationMsg = getConversationMsg(channelID, channelType);
            Log.e("刷新最近会话", "updateConversationMsg 398");
            refreshMsg(conversationMsg, null);
        }
    }

    public LiMConversationMsg getConversationMsg(String channelID, byte channelType) {
        String sql = "select * from " + LiMDBTables.chat_msg_conversation_tab + " where " + LiMDBColumns.LiMCoverMessageColumns.channel_id + "=" + "\"" + channelID + "\"" + " and " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "=" + channelType;
        Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql);
        LiMConversationMsg conversationMsg = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                conversationMsg = serializeLiMMsg(cursor);
            }
            cursor.close();
        }
        return conversationMsg;
    }

    public synchronized boolean deleteMsg(String channelId, byte channelType) {
        String where = LiMDBColumns.LiMCoverMessageColumns.channel_id + "=? and " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "=?";
        String[] whereValue = new String[2];
        whereValue[0] = channelId + "";
        whereValue[1] = channelType + "";
        boolean result = LiMaoIMApplication.getInstance().getDbHelper()
                .delete(LiMDBTables.chat_msg_conversation_tab, where, whereValue);
        if (result) {
            LiMConversationManager.getInstance().setDeleteMsg(channelId, channelType);
        }
        return result;
    }

    public synchronized boolean updateMsgBrowseTo(String channelID, byte channelType, long messageSeq) {
        String[] update = new String[2];
        update[0] = channelID;
        update[1] = String.valueOf(channelType);
        ContentValues cv = new ContentValues();
        try {
            cv.put(LiMDBColumns.LiMCoverMessageColumns.browse_to, messageSeq);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_conversation_tab, cv, LiMDBColumns.LiMCoverMessageColumns.channel_id + "=? and " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "=?", update);
    }

    public synchronized long getMsgBrowseTo(String channelId, byte channelType) {
        String sql = "select * from " + LiMDBTables.chat_msg_conversation_tab + " where " + LiMDBColumns.LiMCoverMessageColumns.channel_id + " = '" + channelId + "' AND " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "="
                + channelType;
        Cursor cursor = null;
        long messageSeq = 0;
        try {
            cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql);
            if (cursor == null) {
                return messageSeq;
            }
            if (cursor.getCount() > 0) {
                cursor.moveToNext();
                LiMConversationMsg msg = serializeLiMMsg(cursor);
                messageSeq = msg.browseTo;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return messageSeq;
    }

    public synchronized void updateMsgCount(String channelId, byte channelType, int count) {
        ContentValues cv = new ContentValues();
        try {
            cv.put(LiMDBColumns.LiMCoverMessageColumns.unread_count, count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] update = new String[2];
        update[0] = channelId;
        update[1] = String.valueOf(channelType);
        if (LiMaoIMApplication.getInstance().getDbHelper() != null) {
            boolean result = LiMaoIMApplication.getInstance().getDbHelper()
                    .update(LiMDBTables.chat_msg_conversation_tab, cv, LiMDBColumns.LiMCoverMessageColumns.channel_id + "=? and " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "=?", update);
            if (result) {
                LiMConversationMsg msg = queryLastLiMConversationMsg(channelId, channelType);
                if (msg != null) {
                    Log.e("刷新最近会话", "updateMsgCount 565");
                    refreshMsg(msg, null);
                }
            }
        }
    }

    public synchronized void updateMsg(String channelID, byte channelType, List<String> fields, List<String> values) {
        String[] updateKey = new String[fields.size()];
        String[] updateValue = new String[values.size()];
        for (int i = 0, size = fields.size(); i < size; i++) {
            updateKey[i] = fields.get(i);
            updateValue[i] = values.get(i);
        }
        String where = LiMDBColumns.LiMCoverMessageColumns.channel_id + "=? and " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "=?";
        String[] whereValue = new String[2];
        whereValue[0] = channelID;
        whereValue[1] = channelType + "";
        int result = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_conversation_tab, updateKey, updateValue, where, whereValue);
        if (result > 0) {
            LiMConversationMsg msg = queryLastLiMConversationMsg(channelID, channelType);
            Log.e("刷新最近会话", "updateConverMsg 785");
            refreshMsg(msg, null);
        }
    }

    public synchronized boolean updateMsg(String channelID, byte channelType, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = LiMDBColumns.LiMCoverMessageColumns.channel_id + "=? and " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "=?";
        String[] whereValue = new String[2];
        whereValue[0] = channelID;
        whereValue[1] = String.valueOf(channelType);
        int result = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_conversation_tab, updateKey, updateValue, where, whereValue);
        if (result > 0) {
            LiMConversationMsg msg = queryLastLiMConversationMsg(channelID, channelType);
            Log.e("刷新最近会话", "updateConverMsg 756");
            refreshMsg(msg, null);
        }
        return result > 0;
    }

    public synchronized LiMConversationMsg queryLastLiMConversationMsg(String channelId, int channelType) {
        String sql = "select * from " + LiMDBTables.chat_msg_conversation_tab + " where " + LiMDBColumns.LiMCoverMessageColumns.channel_id + " = '" + channelId + "' AND " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "="
                + channelType;
        Cursor cursor = null;
        LiMConversationMsg liMConversationMsg = null;
        try {
            cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql);
            if (cursor == null) {
                return null;
            }
            if (cursor.getCount() > 0) {
                cursor.moveToNext();
                liMConversationMsg = serializeLiMMsg(cursor);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return liMConversationMsg;
    }

    public synchronized void updateMsgStatus(long client_seq, int status) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = LiMDBColumns.LiMCoverMessageColumns.status;
        updateValue[0] = status + "";

        String where = LiMDBColumns.LiMCoverMessageColumns.client_seq + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = client_seq + "";

        int b = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_conversation_tab, updateKey, updateValue, where, whereValue);
        LiMLoggerUtils.getInstance().e("修改消息状态：" + b);
        if (b > 0) {
            LiMConversationMsg msg = queryMsgByClientSeq(client_seq);
            if (msg != null) {
                Log.e("刷新最近会话", "update");
                refreshMsg(msg, null);
            }
        }
    }

    public synchronized boolean saveOrUpdateTopMsg(LiMMsg liMMsg, boolean isAddNum, boolean isRefreshUI) {
        if (liMMsg.channelID.equals(LiMaoIMApplication.getInstance().getUid())) return false;
        LiMConversationMsg liMConversationMsg = new LiMConversationMsg();

        LiMConversationMsg tempMsg = queryLastLiMConversationMsg(liMMsg.channelID, liMMsg.channelType);
        if (tempMsg != null) {
            if (tempMsg.clientSeq == liMMsg.clientSeq || tempMsg.lastClientMsgNO.equals(liMMsg.clientMsgNO))
                return true;
            liMConversationMsg.version = tempMsg.version;
        }
        liMConversationMsg.channelID = liMMsg.channelID;
        liMConversationMsg.channelType = liMMsg.channelType;
        liMConversationMsg.lastMsgContent = liMMsg.content;
        liMConversationMsg.extraMap = liMMsg.extraMap;
        liMConversationMsg.status = liMMsg.status;
        liMConversationMsg.clientSeq = liMMsg.clientSeq;
        liMConversationMsg.lastMsgID = liMMsg.messageID;
        liMConversationMsg.lastMsgTimestamp = liMMsg.timestamp;
        liMConversationMsg.lastClientMsgNO = liMMsg.clientMsgNO;
        liMConversationMsg.type = liMMsg.type;
        liMConversationMsg.unreadCount = isAddNum ? 1 : 0;//自己发送消息数量为0
        liMConversationMsg.fromUID = liMMsg.fromUID;

        if (liMMsg.baseContentMsgModel != null)
            liMConversationMsg.mention = liMMsg.baseContentMsgModel.mention_all;
        if (liMMsg.baseContentMsgModel != null && liMMsg.baseContentMsgModel.mentionInfo != null && liMMsg.baseContentMsgModel.mentionInfo.uids.size() > 0) {
            for (int i = 0, size = liMMsg.baseContentMsgModel.mentionInfo.uids.size(); i < size; i++) {
                if (liMMsg.baseContentMsgModel.mentionInfo.uids.get(i).equals(LiMaoIMApplication.getInstance().getUid())) {
                    liMConversationMsg.mention = 1;
                    break;
                }
            }
        }
        return saveOrUpdateMsg(liMConversationMsg, liMMsg, isAddNum, isRefreshUI);// 插入消息列表数据表
    }

    public synchronized boolean saveOrUpdateMsg(LiMConversationMsg conversationMsg, LiMMsg liMMsg, boolean isAddNum, boolean isRefreshUI) {
        boolean isRefreshMsg = false;
        JSONArray jsonArray;
        JSONObject jsonObject = null;
        if (conversationMsg.mention == 1) {
            jsonArray = new JSONArray();
            jsonObject = new JSONObject();
            try {
                jsonObject.put("type", LiMMentionType.liMReminderTypeMentionMe);
                jsonObject.put("text", "有人@你");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(jsonObject);
            conversationMsg.reminders = jsonArray.toString();
        }
        LiMConversationMsg lastMsg = queryMsgByMsgChannelId(conversationMsg.channelID, conversationMsg.channelType);
        if (lastMsg == null || TextUtils.isEmpty(lastMsg.channelID)) {
            //如果服务器自增id为0则表示是本地数据|直接保存
            long row = insert(conversationMsg);
            isRefreshMsg = row > 0;
        } else {
            if (conversationMsg.clientSeq == 0 || lastMsg.clientSeq < conversationMsg.clientSeq) {
                if (isAddNum) {
                    conversationMsg.unreadCount = lastMsg.unreadCount + conversationMsg.unreadCount;
                } else conversationMsg.unreadCount = 0;

                //将新消息和老消息和提醒内容合并
                if (!TextUtils.isEmpty(lastMsg.reminders)) {
                    try {
                        JSONArray temp = new JSONArray(lastMsg.reminders);
                        boolean isAt = false;
                        for (int i = 0, size = temp.length(); i < size; i++) {
                            int type = temp.getJSONObject(i).optInt("type");
                            if (type == 1) {
                                isAt = true;
                                break;
                            }
                        }
                        if (!isAt) {
                            if (jsonObject != null) {
                                temp.put(jsonObject);
                                conversationMsg.reminders = temp.toString();
                            } else {
                                conversationMsg.reminders = lastMsg.reminders;
                            }
                        } else {
                            conversationMsg.reminders = lastMsg.reminders;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                isRefreshMsg = update(conversationMsg, true);
            } else {
                LiMLoggerUtils.getInstance().e("更改消息错误：---->");
            }
        }

        if (isRefreshMsg && isRefreshUI) {
            Log.e("刷新最近会话", "saveOrUpdateMsg 468");
            refreshMsg(conversationMsg, liMMsg);
        }
        return isRefreshMsg;
    }

    public synchronized long insert(LiMConversationMsg msg) {
        ContentValues cv = new ContentValues();
        try {
            cv = LiMSqlContentValues.getContentValuesWithLiMCoverMsg(msg, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msg.id != 0) return 0;
        long result = -1;
        try {
            result = LiMaoIMApplication.getInstance().getDbHelper()
                    .insert(LiMDBTables.chat_msg_conversation_tab, cv);
        } catch (Exception e) {
            LiMLoggerUtils.getInstance().e("插入数据库异常：" + e.getMessage());
        }
        return result;
    }

    /**
     * 更新会话记录消息
     *
     * @param msg 会话消息
     * @return 修改结果
     */
    public synchronized boolean update(LiMConversationMsg msg, boolean isUpdateStatus) {
        String[] update = new String[2];
        update[0] = msg.channelID;
        update[1] = String.valueOf(msg.channelType);
        ContentValues cv = new ContentValues();
        try {
            cv = LiMSqlContentValues.getContentValuesWithLiMCoverMsg(msg, isUpdateStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //        if (result) {
//            Log.e("刷新最近会话","update 536");
//            refreshConverMsg(msg, null);
//        }
        return LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_conversation_tab, cv, LiMDBColumns.LiMCoverMessageColumns.channel_id + "=? and " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "=?", update);
    }

    public LiMUIConversationMsg saveOrUpdateMsg(LiMMsg liMMsg, int count, boolean isRefreshUI) {
        LiMConversationMsg msg = saveOrUpdateTopMsg(liMMsg, count, isRefreshUI);
        LiMUIConversationMsg uiMsg = new LiMUIConversationMsg();
        if (msg == null) return uiMsg;
        uiMsg.unreadCount = msg.unreadCount;
        uiMsg.mentionList = getReminders(msg.reminders);
        uiMsg.channelID = msg.channelID;
        uiMsg.channelType = msg.channelType;
        uiMsg.clientMsgNo = msg.lastClientMsgNO;
        uiMsg.lastMsgTimestamp = msg.lastMsgTimestamp;
        uiMsg.status = msg.status;
        if (!TextUtils.isEmpty(msg.fromUID)) {
            LiMChannel liMChannel = LiMaoIM.getInstance().getLiMChannelManager().getLiMChannel(msg.fromUID, LiMChannelType.PERSONAL);
            if (uiMsg.getLiMMsg() != null)
                uiMsg.getLiMMsg().setFrom(liMChannel);
            LiMChannelMember member = LiMaoIM.getInstance().getLiMChannelMembersManager().getLiMChannelMember(msg.channelID, msg.channelType, msg.fromUID);
            if (uiMsg.getLiMMsg() != null)
                uiMsg.getLiMMsg().setMemberOfFrom(member);
        }

        return uiMsg;
    }

    public synchronized LiMConversationMsg saveOrUpdateTopMsg(LiMMsg liMMsg, int count, boolean isRefreshUI) {
        if (TextUtils.isEmpty(liMMsg.channelID)) return null;
        if (liMMsg.channelID.equals(LiMaoIMApplication.getInstance().getUid())) {
            LiMLoggerUtils.getInstance().e("自己给自己发送了信息----->");
            return null;
        }
        LiMConversationMsg liMConversationMsg = new LiMConversationMsg();
        liMConversationMsg.channelID = liMMsg.channelID;
        liMConversationMsg.channelType = liMMsg.channelType;
        liMConversationMsg.lastMsgContent = liMMsg.content;
        liMConversationMsg.extraMap = liMMsg.extraMap;
        liMConversationMsg.status = liMMsg.status;
        liMConversationMsg.clientSeq = liMMsg.clientSeq;
        liMConversationMsg.lastClientMsgNO = liMMsg.clientMsgNO;
        liMConversationMsg.lastMsgID = liMMsg.messageID;
        liMConversationMsg.lastMsgTimestamp = liMMsg.timestamp;
        liMConversationMsg.type = liMMsg.type;
        liMConversationMsg.unreadCount = count;
        liMConversationMsg.fromUID = liMMsg.fromUID;
        LiMConversationMsg tempMsg = queryLastLiMConversationMsg(liMMsg.channelID, liMMsg.channelType);
        if (tempMsg != null) {
            liMConversationMsg.version = tempMsg.version;
        }
        if (liMMsg.baseContentMsgModel != null) {
            liMConversationMsg.mention = liMMsg.baseContentMsgModel.mention_all;
        }
        if (liMMsg.baseContentMsgModel != null && liMMsg.baseContentMsgModel.mentionInfo != null
                && liMMsg.baseContentMsgModel.mentionInfo.uids.size() > 0 && count > 0) {
            for (int i = 0, size = liMMsg.baseContentMsgModel.mentionInfo.uids.size(); i < size; i++) {
                if (liMMsg.baseContentMsgModel.mentionInfo.uids.get(i).equals(LiMaoIMApplication.getInstance().getUid())) {
                    liMConversationMsg.mention = 1;
                    break;
                }
            }
        }

        saveOrUpdateMsg(liMConversationMsg, liMMsg, true, isRefreshUI);// 插入消息列表数据表
        return liMConversationMsg;
    }

    private synchronized LiMConversationMsg queryMsgByMsgChannelId(String channelId, byte channelType) {
        LiMConversationMsg liMConversationMsg = null;
        String selection = LiMDBColumns.LiMCoverMessageColumns.channel_id + " = ? and " + LiMDBColumns.LiMCoverMessageColumns.channel_type + "=?";
        String[] selectionArgs = new String[]{channelId, channelType + ""};
        Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .select(LiMDBTables.chat_msg_conversation_tab, selection, selectionArgs,
                        null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                liMConversationMsg = serializeLiMMsg(cursor);
            }
            cursor.close();
        }
        return liMConversationMsg;
    }

    private synchronized LiMConversationMsg queryMsgByClientSeq(long clientSeq) {
        LiMConversationMsg liMConversationMsg = null;
        String selection = LiMDBColumns.LiMCoverMessageColumns.client_seq + " = ? ";
        String[] selectionArgs = new String[]{String.valueOf(clientSeq)};
        Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .select(LiMDBTables.chat_msg_conversation_tab, selection, selectionArgs,
                        null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                liMConversationMsg = serializeLiMMsg(cursor);
            }
            cursor.close();
        }
        return liMConversationMsg;
    }

    public synchronized boolean clearEmpty() {
        return LiMaoIMApplication.getInstance().getDbHelper()
                .delete(LiMDBTables.chat_msg_conversation_tab, null, null);
    }

    synchronized void updateSendingMsgfail() {
        String[] updateKey = new String[1];
        updateKey[0] = LiMDBColumns.LiMCoverMessageColumns.status;
        String[] updateValue = new String[1];
        updateValue[0] = LiMSendMsgResult.send_fail + "";
        String where = LiMDBColumns.LiMCoverMessageColumns.status + "=? ";
        String[] whereValue = new String[1];
        whereValue[0] = "0";

        try {
            if (LiMaoIMApplication.getInstance().getDbHelper() != null) {
                LiMaoIMApplication
                        .getInstance()
                        .getDbHelper()
                        .update(LiMDBTables.chat_msg_conversation_tab, updateKey, updateValue, where,
                                whereValue);
            }
        } catch (Exception ignored) {
        }
    }

    private synchronized LiMConversationMsg serializeLiMMsg(Cursor cursor) {
        LiMConversationMsg liMConversationMsg = new LiMConversationMsg();
        liMConversationMsg.id = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.id));
        liMConversationMsg.channelID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.channel_id));
        liMConversationMsg.channelType = (byte) cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.channel_type));
        liMConversationMsg.clientSeq = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.client_seq));
        liMConversationMsg.lastMsgID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.last_msg_id));
        liMConversationMsg.lastMsgContent = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.last_msg_content));
        liMConversationMsg.type = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.type));
        liMConversationMsg.fromUID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.from_uid));
        liMConversationMsg.lastMsgTimestamp = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.last_msg_timestamp));
        liMConversationMsg.status = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.status));
        liMConversationMsg.unreadCount = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.unread_count));
        liMConversationMsg.isDeleted = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.is_deleted));
        liMConversationMsg.version = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.version));
        liMConversationMsg.browseTo = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.browse_to));
        liMConversationMsg.lastClientMsgNO = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.last_client_msg_no));
        String extra = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.extra));
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
            liMConversationMsg.extraMap = hashMap;
        }

        liMConversationMsg.reminders = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMCoverMessageColumns.reminders));
        return liMConversationMsg;
    }

    // 获取高光提示数据
    private List<LiMReminder> getReminders(String reminders) {
        List<LiMReminder> tempList = null;
        if (!TextUtils.isEmpty(reminders)) {
            try {
                JSONArray jsonArray = new JSONArray(reminders);
                if (jsonArray.length() > 0) {
                    tempList = new ArrayList<>();
                    for (int i = 0, size = jsonArray.length(); i < size; i++) {
                        LiMReminder mMention = new LiMReminder();
                        JSONObject jsonObject = jsonArray.optJSONObject(i);
                        if (jsonObject.has("type"))
                            mMention.type = jsonObject.optInt("type");
                        if (jsonObject.has("text"))
                            mMention.text = jsonObject.optString("text");
                        if (jsonObject.has("data"))
                            mMention.data = jsonObject.opt("data");
                        tempList.add(mMention);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return tempList;
    }

    private void refreshMsg(LiMConversationMsg conversationMsg, LiMMsg liMMsg) {
        LiMUIConversationMsg liMUIConversationMsg = new LiMUIConversationMsg();
        liMUIConversationMsg.clientMsgNo = conversationMsg.lastClientMsgNO;
        liMUIConversationMsg.mentionList = getReminders(conversationMsg.reminders);
        liMUIConversationMsg.unreadCount = conversationMsg.unreadCount;
        liMUIConversationMsg.status = conversationMsg.status;
        liMUIConversationMsg.setLiMMsg(liMMsg);
        liMUIConversationMsg.lastMsgTimestamp = conversationMsg.lastMsgTimestamp;
        liMUIConversationMsg.channelID = conversationMsg.channelID;
        liMUIConversationMsg.channelType = conversationMsg.channelType;
        LiMaoIM.getInstance().getLiMConversationManager().setOnRefreshMsg(liMUIConversationMsg, true);
    }


    private LiMUIConversationMsg getUIMsg(LiMConversationMsg conversationMsg) {
        LiMUIConversationMsg liMUIConversationMsg = new LiMUIConversationMsg();
        liMUIConversationMsg.clientMsgNo = conversationMsg.lastClientMsgNO;
        liMUIConversationMsg.mentionList = getReminders(conversationMsg.reminders);
        liMUIConversationMsg.unreadCount = conversationMsg.unreadCount;
        liMUIConversationMsg.status = conversationMsg.status;
        liMUIConversationMsg.lastMsgTimestamp = conversationMsg.lastMsgTimestamp;
        liMUIConversationMsg.channelID = conversationMsg.channelID;
        liMUIConversationMsg.channelType = conversationMsg.channelType;
        return liMUIConversationMsg;
    }
}
