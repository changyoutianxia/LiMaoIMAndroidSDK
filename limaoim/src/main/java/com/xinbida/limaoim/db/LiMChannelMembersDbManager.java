package com.xinbida.limaoim.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;


import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.entity.LiMChannelMember;
import com.xinbida.limaoim.manager.LiMChannelMembersManager;
import com.xinbida.limaoim.utils.LiMLoggerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 2019-11-10 14:06
 * 频道成员数据管理
 */
public class LiMChannelMembersDbManager {

    private LiMChannelMembersDbManager() {
    }

    private static class LiMChannelMembersManagerBinder {
        private final static LiMChannelMembersDbManager liMChannelMembersManager = new LiMChannelMembersDbManager();
    }

    public static LiMChannelMembersDbManager getInstance() {
        return LiMChannelMembersManagerBinder.liMChannelMembersManager;
    }

    /**
     * 查询某个频道的所有成员
     *
     * @param channelId 频道ID
     * @return List<LiMChannelMember>
     */
    public synchronized List<LiMChannelMember> query(String channelId, byte channelType) {
        String sql = "select " + LiMDBTables.channel_members_tab + ".*," + LiMDBTables.channel_tab + ".channel_remark," + LiMDBTables.channel_tab + ".channel_name," + LiMDBTables.channel_tab + ".avatar from " + LiMDBTables.channel_members_tab + " LEFT JOIN channel_tab on channel_members_tab.member_uid=channel_tab.channel_id and channel_tab.channel_type=1 where channel_members_tab.channel_id=" + "\"" + channelId + "\"" + " and channel_members_tab.channel_type=" + channelType + " and channel_members_tab.is_deleted=0 and channel_members_tab.status=1 order by channel_members_tab.role=1 desc,channel_members_tab.role=2 desc,channel_members_tab." + LiMDBColumns.LiMChannelMembersColumns.created_at + " asc";
//        String sql = "select * from " + LiMDBTables.channel_members_tab + " where channel_id=" + "\"" + channelId + "\"" + " and channel_type=" + channelType + " and is_deleted=0 and status=1 order by role=1 desc,role=2 desc," + LiMDBColumns.LiMChannelMembersColumns.created_at + " asc";
        Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql);
        List<LiMChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    /**
     * 查询单个频道成员
     *
     * @param channelId 频道ID
     * @param uid       用户ID
     * @return LiMChannelMember
     */
    public synchronized LiMChannelMember query(String channelId, byte channelType, String uid) {
        LiMChannelMember liMChannelMember = null;
        String sql = "select " + LiMDBTables.channel_members_tab + ".*," + LiMDBTables.channel_tab + ".channel_name," + LiMDBTables.channel_tab + ".channel_remark," + LiMDBTables.channel_tab + ".avatar from " + LiMDBTables.channel_members_tab + " left join " + LiMDBTables.channel_tab + " on channel_members_tab.member_uid = channel_tab.channel_id AND channel_tab.channel_type=1 where (channel_members_tab." + LiMDBColumns.LiMChannelMembersColumns.channel_id + "=" + "\"" + channelId + "\"" + " and channel_members_tab." + LiMDBColumns.LiMChannelMembersColumns.channel_type + "=" + channelType + " and channel_members_tab." + LiMDBColumns.LiMChannelMembersColumns.member_uid + "=" + "\"" + uid + "\")";
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                liMChannelMember = serializableChannelMember(cursor);
            }
        }
        return liMChannelMember;
    }

    public synchronized void insertMember(LiMChannelMember liMChannelMember) {
        if (TextUtils.isEmpty(liMChannelMember.channelID) || TextUtils.isEmpty(liMChannelMember.memberUID))
            return;
        ContentValues cv = new ContentValues();
        try {
            cv = LiMSqlContentValues.getContentValuesByLiMChannelMember(liMChannelMember);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LiMaoIMApplication.getInstance().getDbHelper()
                .insert(LiMDBTables.channel_members_tab, cv);
    }

    /**
     * 批量插入频道成员
     *
     * @param list List<LiMChannelMember>
     */
    public void insertChannelMember(List<LiMChannelMember> list) {

        try {
            LiMaoIMApplication.getInstance().getDbHelper().getDb()
                    .beginTransaction();
            if (list != null && list.size() > 0) {
                for (int i = 0, size = list.size(); i < size; i++) {
                    saveOrUpdateChannelMember(list.get(i));
                }
            }
            LiMaoIMApplication.getInstance().getDbHelper().getDb()
                    .setTransactionSuccessful();
        } catch (Exception e) {
            LiMLoggerUtils.getInstance().e("保存频道成员错误：" + e.getLocalizedMessage());
        } finally {
            if (LiMaoIMApplication.getInstance().getDbHelper().getDb().inTransaction()) {
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .endTransaction();
            }
        }
    }


    public void saveOrUpdateChannelMember(LiMChannelMember liMChannelMember) {
        if (liMChannelMember == null) return;
        LiMChannelMember temp = query(liMChannelMember.channelID, liMChannelMember.channelType, liMChannelMember.memberUID);
        if (temp != null && !TextUtils.isEmpty(temp.channelID)) {
            updateChannelMember(liMChannelMember);
        } else {
            insertMember(liMChannelMember);
        }

    }

    /**
     * 修改某个频道的某个成员信息
     *
     * @param liMChannelMember 成员
     */
    public synchronized void updateChannelMember(LiMChannelMember liMChannelMember) {
        String[] update = new String[3];
        update[0] = liMChannelMember.channelID;
        update[1] = String.valueOf(liMChannelMember.channelType);
        update[2] = liMChannelMember.memberUID;
        ContentValues cv = new ContentValues();
        try {
            cv = LiMSqlContentValues.getContentValuesByLiMChannelMember(liMChannelMember);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.channel_members_tab, cv, LiMDBColumns.LiMChannelMembersColumns.channel_id + "=? and " + LiMDBColumns.LiMChannelMembersColumns.channel_type + "=? and " + LiMDBColumns.LiMChannelMembersColumns.member_uid + "=?", update);
    }

    /**
     * 根据字段修改频道成员
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param field       字段
     * @param value       值
     */
    public synchronized boolean updateChannelMember(String channelID, byte channelType, String uid, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = LiMDBColumns.LiMChannelMembersColumns.channel_id + "=? and " + LiMDBColumns.LiMChannelMembersColumns.channel_type + "=? and " + LiMDBColumns.LiMChannelMembersColumns.member_uid + "=?";
        String[] whereValue = new String[3];
        whereValue[0] = channelID;
        whereValue[1] = channelType + "";
        whereValue[2] = uid;
        int row = LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.channel_members_tab, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            LiMChannelMember liMChannelMember = query(channelID, channelType, uid);
            if (liMChannelMember != null)
                //刷新频道成员信息
                LiMChannelMembersManager.getInstance().setRefreshChannelMember(liMChannelMember);
        }
        return row > 0;
    }

    /**
     * 批量删除频道成员
     *
     * @param list 频道成员
     */
    public synchronized void deleteChannelMembers(List<LiMChannelMember> list) {
        try {
            LiMaoIMApplication.getInstance().getDbHelper().getDb()
                    .beginTransaction();
            if (list != null && list.size() > 0) {
                for (int i = 0, size = list.size(); i < size; i++) {
                    saveOrUpdateChannelMember(list.get(i));
                }
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .setTransactionSuccessful();
            }
        } catch (Exception e) {
            LiMLoggerUtils.getInstance().e("移除频道成员错误：" + e.getLocalizedMessage());
        } finally {
            if (LiMaoIMApplication.getInstance().getDbHelper().getDb().inTransaction()) {
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .endTransaction();
            }
        }
        LiMChannelMembersManager.getInstance().setOnRemoveChannelMember(list);
    }

    /**
     * 获取最大版本的频道成员
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return LiMChannelMember
     */
    public synchronized LiMChannelMember getMaxVersionMember(String channelID, byte channelType) {
        LiMChannelMember liMChannelMember = null;
        String sql = "select * from " + LiMDBTables.channel_members_tab + " where " + LiMDBColumns.LiMChannelMembersColumns.channel_id + "=" + "\"" + channelID + "\"" + " and " + LiMDBColumns.LiMChannelMembersColumns.channel_type + "=" + channelType + " order by " + LiMDBColumns.LiMChannelMembersColumns.version + " desc limit 0,1";

        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                liMChannelMember = serializableChannelMember(cursor);
            }
        }
        return liMChannelMember;
    }

    public synchronized List<LiMChannelMember> queryLiMChannelMembersByStatus(String channelId, byte channelType, int status) {
        String sql = "select " + LiMDBTables.channel_members_tab + ".*," + LiMDBTables.channel_tab + ".channel_name," + LiMDBTables.channel_tab + ".channel_remark," + LiMDBTables.channel_tab + ".avatar from " + LiMDBTables.channel_members_tab + " left Join " + LiMDBTables.channel_tab + " where channel_members_tab.member_uid = channel_tab.channel_id AND channel_tab.channel_type=1 AND channel_members_tab.channel_id=" + "\"" + channelId + "\"" + " and channel_members_tab.channel_type=" + channelType + " and channel_members_tab.status=" + status + " order by channel_members_tab.role=1 desc,channel_members_tab.role=2 desc,channel_members_tab." + LiMDBColumns.LiMChannelMembersColumns.created_at + " asc";
        Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql);
        List<LiMChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized int getMembersCount(String channelID, byte channelType) {
        String sql = "select count(*) from " + LiMDBTables.channel_members_tab
                + " where (" + LiMDBColumns.LiMChannelMembersColumns.channel_id + "=" + "\"" + channelID + "\"" + " and "
                + LiMDBColumns.LiMChannelMembersColumns.channel_type + "=" + channelType + " and " + LiMDBColumns.LiMChannelMembersColumns.is_deleted + "=0 and " + LiMDBColumns.LiMChannelMembersColumns.status + "=1)";
        Cursor cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /**
     * 序列化频道成员
     *
     * @param cursor Cursor
     * @return LiMChannelMember
     */
    private synchronized LiMChannelMember serializableChannelMember(Cursor cursor) {
        LiMChannelMember liMChannelMember = new LiMChannelMember();
        liMChannelMember.id = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.id));
        liMChannelMember.status = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.status));
        liMChannelMember.channelID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.channel_id));
        liMChannelMember.channelType = (byte) cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.channel_type));
        liMChannelMember.memberUID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.member_uid));
        liMChannelMember.memberName = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.member_name));
        liMChannelMember.memberAvatar = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.member_avatar));
        liMChannelMember.memberRemark = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.member_remark));
        liMChannelMember.role = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.role));
        liMChannelMember.isDeleted = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.is_deleted));
        liMChannelMember.version = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.version));
        liMChannelMember.createdAt = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.created_at));
        liMChannelMember.updatedAt = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.updated_at));
        if (cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.channel_name) >= 0) {
            String channelName = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.channel_name));
            if (!TextUtils.isEmpty(channelName)) liMChannelMember.memberName = channelName;
        }
        if (cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.channel_remark) >= 0) {
            liMChannelMember.remark = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.channel_remark));
        }
        if (cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.avatar) >= 0) {
            liMChannelMember.memberAvatar = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.avatar));
        }
        String extra = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelMembersColumns.extra));
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
            liMChannelMember.extraMap = hashMap;
        }
        return liMChannelMember;
    }
}
