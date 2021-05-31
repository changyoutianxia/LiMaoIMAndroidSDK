package com.xinbida.limaoim.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;


import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.entity.LiMChannel;
import com.xinbida.limaoim.entity.LiMMsgReaction;
import com.xinbida.limaoim.message.type.LiMChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 4/16/21 1:46 PM
 * 消息回应
 */
class LiMMsgReactionDBManager {
    private LiMMsgReactionDBManager() {
    }

    private static class LiMMessageReactionDBManagerBinder {
        final static LiMMsgReactionDBManager manager = new LiMMsgReactionDBManager();
    }

    public static LiMMsgReactionDBManager getInstance() {
        return LiMMessageReactionDBManagerBinder.manager;
    }

    public void insertReaction(List<LiMMsgReaction> list) {
        if (list == null || list.size() == 0) return;
        for (int i = 0, size = list.size(); i < size; i++) {
            insertOrUpdate(list.get(i));
        }
    }

    public void update(LiMMsgReaction reaction) {
        String[] update = new String[3];
        update[0] = reaction.messageID;
        update[1] = reaction.uid;
        update[2] = reaction.emoji;
        ContentValues cv = new ContentValues();
        cv.put(LiMDBColumns.LiMMessageReaction.is_deleted, reaction.isDeleted);
        cv.put(LiMDBColumns.LiMMessageReaction.seq, reaction.seq);
        LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.chat_msg_reaction_tab, cv, LiMDBColumns.LiMMessageReaction.message_id + "=? and " + LiMDBColumns.LiMMessageReaction.uid + "=? and " + LiMDBColumns.LiMMessageReaction.emoji + "=?", update);

    }

    public synchronized void insertOrUpdate(LiMMsgReaction reaction) {
        if (isExist(reaction.uid, reaction.messageID, reaction.emoji)) {
            update(reaction);
        } else {
            insert(reaction);
        }
    }

    public void insert(LiMMsgReaction reaction) {
        LiMaoIMApplication.getInstance().getDbHelper()
                .insert(LiMDBTables.chat_msg_reaction_tab, LiMSqlContentValues.getContentValuesWithMsgReaction(reaction));
    }

    private boolean isExist(String uid, String messageID, String emoji) {
        LiMMsgReaction reaction = queryReaction(messageID, uid, emoji);
        return reaction != null;
    }

    public List<LiMMsgReaction> queryReactions(String messageID) {
        List<LiMMsgReaction> list = new ArrayList<>();
        String sql = "select * from " + LiMDBTables.chat_msg_reaction_tab + " where " + LiMDBColumns.LiMMessageReaction.message_id + "=" + "\"" + messageID + "\"" + " and " + LiMDBColumns.LiMMessageReaction.is_deleted + "=0 ORDER BY " + LiMDBColumns.LiMMessageReaction.created_at + " desc";
        try (Cursor cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMMsgReaction reaction = serializeReaction(cursor);
                LiMChannel liMChannel = LiMaoIM.getInstance().getLiMChannelManager().getLiMChannel(reaction.uid, LiMChannelType.PERSONAL);
                if (liMChannel != null) {
                    String showName = TextUtils.isEmpty(liMChannel.channelRemark) ? liMChannel.channelName : liMChannel.channelRemark;
                    if (!TextUtils.isEmpty(showName))
                        reaction.name = showName;
                }
                list.add(reaction);
            }
        }
        return list;
    }

    public List<LiMMsgReaction> queryWithMessageIds(List<String> messageIds) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0, size = messageIds.size(); i < size; i++) {
            if (!TextUtils.isEmpty(stringBuffer)) {
                stringBuffer.append(",");
            }
            stringBuffer.append(messageIds.get(i));
        }
        String sql = "select * from " + LiMDBTables.chat_msg_reaction_tab + " where " + LiMDBColumns.LiMMessageReaction.message_id + " in (" + stringBuffer + ") and " + LiMDBColumns.LiMMessageReaction.is_deleted + "=0 ORDER BY " + LiMDBColumns.LiMMessageReaction.created_at + " desc";
        List<LiMMsgReaction> list = new ArrayList<>();
        List<String> channelIds = new ArrayList<>();
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMMsgReaction liMMsgReaction = serializeReaction(cursor);
                channelIds.add(liMMsgReaction.uid);
                list.add(liMMsgReaction);
            }
        } catch (Exception ignored) {
        }
        //查询用户备注
        List<LiMChannel> channelList = LiMChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(channelIds, LiMChannelType.PERSONAL);
        for (int i = 0, size = list.size(); i < size; i++) {
            for (int j = 0, len = channelList.size(); j < len; j++) {
                if (channelList.get(j).channelID.equals(list.get(i).uid)) {
                    list.get(i).name = TextUtils.isEmpty(channelList.get(j).channelRemark) ? channelList.get(j).channelName : channelList.get(j).channelRemark;
                }
            }
        }
        return list;
    }

    public LiMMsgReaction queryReaction(String messageID, String uid, String emoji) {
        LiMMsgReaction reaction = null;
        String sql = "select * from " + LiMDBTables.chat_msg_reaction_tab
                + " where " + LiMDBColumns.LiMMessageReaction.message_id + "=" + "\"" + messageID + "\""
                + " and " + LiMDBColumns.LiMMessageReaction.uid + "=" + "\"" + uid + "\" and "
                + LiMDBColumns.LiMMessageReaction.emoji + "=" + "\"" + emoji + "\"";
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                reaction = serializeReaction(cursor);
            }
        }

        return reaction;
    }

    public long getMaxSeqWithChannel(String channelID, byte channelType) {
        int maxSeq = 0;
        String sql = "select max(seq) seq from " + LiMDBTables.chat_msg_reaction_tab
                + " where " + LiMDBColumns.LiMMessageReaction.channel_id + "=" + "\"" + channelID + "\"" + " and "
                + LiMDBColumns.LiMMessageReaction.channel_type + "=" + channelType + " limit 0, 1";
        try {
            if (LiMaoIMApplication.getInstance().getDbHelper() != null) {
                Cursor cursor = LiMaoIMApplication
                        .getInstance()
                        .getDbHelper()
                        .rawQuery(sql);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        maxSeq = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.seq));
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return maxSeq;
    }

    private LiMMsgReaction serializeReaction(Cursor cursor) {
        LiMMsgReaction reaction = new LiMMsgReaction();
        reaction.channelID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.channel_id));
        reaction.channelType = (byte) cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.channel_type));
        reaction.isDeleted = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.is_deleted));
        reaction.uid = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.uid));
        reaction.name = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.name));
        reaction.messageID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.message_id));
        reaction.createdAt = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.created_at));
        reaction.seq = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.seq));
        reaction.emoji = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMMessageReaction.emoji));
        return reaction;
    }
}
