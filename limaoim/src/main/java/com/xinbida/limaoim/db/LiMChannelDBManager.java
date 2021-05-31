package com.xinbida.limaoim.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.entity.LiMChannel;
import com.xinbida.limaoim.entity.LiMChannelSearchResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 5/20/21 5:53 PM
 * channel DB manager
 */
public class LiMChannelDBManager {
    private LiMChannelDBManager() {
    }

    private static class LiMChannelDBManagerBinder {
        static final LiMChannelDBManager channelDBManager = new LiMChannelDBManager();
    }

    public static LiMChannelDBManager getInstance() {
        return LiMChannelDBManagerBinder.channelDBManager;
    }

    public LiMChannel getChannel(String channelId, int channelType) {
        return queryChannelByChannelId(channelId, channelType);
    }


    public List<LiMChannel> queryWithChannelIdsAndChannelType(List<String> channelIDs, byte channelType) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0, size = channelIDs.size(); i < size; i++) {
            if (stringBuffer.toString().contains(channelIDs.get(i)))
                continue;
            if (!TextUtils.isEmpty(stringBuffer)) {
                stringBuffer.append(",");
            }
            stringBuffer.append("\"").append(channelIDs.get(i)).append("\"");
        }
        String sql = "select * from " + LiMDBTables.channel_tab + " where " + LiMDBColumns.LiMChannelColumns.channel_id + " in (" + stringBuffer + ") and " + LiMDBColumns.LiMChannelColumns.channel_type + "=" + channelType;
        List<LiMChannel> list = new ArrayList<>();
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                LiMChannel liMChannel = serializableChannel(cursor);
                list.add(liMChannel);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private synchronized LiMChannel queryChannelByChannelId(String channelId, int channelType) {
        String selection = LiMDBColumns.LiMChannelColumns.channel_id + "=? and " + LiMDBColumns.LiMChannelColumns.channel_type + "=?";
        String[] selectionArgs = new String[2];
        selectionArgs[0] = channelId;
        selectionArgs[1] = String.valueOf(channelType);
        Cursor cursor = null;
        LiMChannel liMChannel = null;
        try {
            cursor = LiMaoIMApplication
                    .getInstance()
                    .getDbHelper()
                    .select(LiMDBTables.channel_tab, selection, selectionArgs,
                            null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToNext();
                    liMChannel = serializableChannel(cursor);
                }

            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return liMChannel;
    }

    public synchronized void insertOrUpdateChannel(LiMChannel liMChannel) {
        LiMChannel temp = queryChannelByChannelId(liMChannel.channelID, liMChannel.channelType);
        if (temp != null && !TextUtils.isEmpty(temp.channelID)) {
            updateChannel(liMChannel);
        } else {
            insertChannel(liMChannel);
        }
    }

    private synchronized void insertChannel(LiMChannel liMChannel) {
        ContentValues cv = new ContentValues();
        try {
            cv = LiMSqlContentValues.getContentValuesByLiMChannel(liMChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LiMaoIMApplication.getInstance().getDbHelper()
                .insert(LiMDBTables.channel_tab, cv);
    }

    public synchronized void updateChannel(LiMChannel liMChannel) {
        String[] update = new String[2];
        update[0] = liMChannel.channelID;
        update[1] = String.valueOf(liMChannel.channelType);
        ContentValues cv = new ContentValues();
        try {
            cv = LiMSqlContentValues.getContentValuesByLiMChannel(liMChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.channel_tab, cv, LiMDBColumns.LiMChannelColumns.channel_id + "=? and " + LiMDBColumns.LiMChannelColumns.channel_type + "=?", update);

    }
    /**
     * 查询频道
     *
     * @param channelType 频道类型
     * @param follow      是否关注 好友或陌生人
     * @param status      状态 正常或黑名单
     * @return List<LiMChannel>
     */
    public synchronized List<LiMChannel> queryAllByFollowAndStatus(byte channelType, int follow, int status) {
        String sql = "select * from " + LiMDBTables.channel_tab + " where " + LiMDBColumns.LiMChannelColumns.channel_type + "=" + channelType + " and " + LiMDBColumns.LiMChannelColumns.follow + "=" + follow + " and " + LiMDBColumns.LiMChannelColumns.status + "=" + status;
        List<LiMChannel> liMChannels = new ArrayList<>();
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return liMChannels;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                liMChannels.add(serializableChannel(cursor));
            }
        }
        return liMChannels;
    }
    /**
     * 查下指定频道类型和频道状态的频道
     *
     * @param channelType 频道类型
     * @param status      状态[sdk不维护状态]
     * @return List<LiMChannel>
     */
    public synchronized List<LiMChannel> queryAllByStatus(byte channelType, int status) {
        String sql = "select * from " + LiMDBTables.channel_tab + " where " + LiMDBColumns.LiMChannelColumns.channel_type + "=" + channelType + " and " + LiMDBColumns.LiMChannelColumns.status + "=" + status;
        List<LiMChannel> liMChannels = new ArrayList<>();
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return liMChannels;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                liMChannels.add(serializableChannel(cursor));
            }
        }
        return liMChannels;
    }


    public synchronized List<LiMChannelSearchResult> searchLiMChannelInfo(String searchKey) {
        List<LiMChannelSearchResult> list = new ArrayList<>();
        String sql = " select t.*,cm.member_name,cm.member_remark from (\n" +
                " select channel_tab.*,max(channel_members_tab.id) mid from " + LiMDBTables.channel_tab + "," + LiMDBTables.channel_members_tab + " " +
                "where channel_tab.channel_id=channel_members_tab.channel_id and channel_tab.channel_type=channel_members_tab.channel_type" +
                " and (channel_tab.channel_name like '%" + searchKey + "%' or channel_tab.channel_remark" +
                " like '%" + searchKey + "%' or channel_members_tab.member_name like '%" + searchKey + "%' or channel_members_tab.member_remark like '%" + searchKey + "%')\n" +
                " group by channel_tab.channel_id,channel_tab.channel_type\n" +
                " ) t,channel_members_tab cm where t.channel_id=cm.channel_id and t.channel_type=cm.channel_type and t.mid=cm.id";
        Cursor cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql);
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String member_name = cursor.getString(cursor.getColumnIndex("member_name"));
            String member_remark = cursor.getString(cursor.getColumnIndex("member_remark"));
            LiMChannel liMChannel = serializableChannel(cursor);
            LiMChannelSearchResult result = new LiMChannelSearchResult();
            result.liMChannel = liMChannel;
            if (!TextUtils.isEmpty(member_remark)) {
                //优先显示备注名称
                if (member_remark.toUpperCase().contains(searchKey.toUpperCase())) {
                    result.containMemberName = member_remark;
                }
            }
            if (TextUtils.isEmpty(result.containMemberName)) {
                if (!TextUtils.isEmpty(member_name)) {
                    if (member_name.toUpperCase().contains(searchKey.toUpperCase())) {
                        result.containMemberName = member_name;
                    }
                }
            }
            list.add(result);
        }
        cursor.close();
        return list;
    }

    public synchronized List<LiMChannel> searchLiMChannels(String searchKey, byte channelType) {
        List<LiMChannel> list = new ArrayList<>();
        String sql = "select * from " + LiMDBTables.channel_tab + " where (" + LiMDBColumns.LiMChannelColumns.channel_name + " LIKE \"%" + searchKey + "%\" or " + LiMDBColumns.LiMChannelColumns.channel_remark + " LIKE \"%" + searchKey + "%\") and " + LiMDBColumns.LiMChannelColumns.channel_type + "=" + channelType;
        try (Cursor cursor = LiMaoIMApplication.getInstance().getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                list.add(serializableChannel(cursor));
            }
        }
        return list;
    }

    public synchronized List<LiMChannel> queryAllByFollow(byte channelType, int follow) {
        String sql = "select * from " + LiMDBTables.channel_tab + " where " + LiMDBColumns.LiMChannelColumns.channel_type + "=" + channelType + " and " + LiMDBColumns.LiMChannelColumns.follow + "=" + follow;
        List<LiMChannel> liMChannels = new ArrayList<>();
        try (Cursor cursor = LiMaoIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return liMChannels;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                liMChannels.add(serializableChannel(cursor));
            }
        }
        return liMChannels;
    }

    public synchronized void updateChannel(String channelID, byte channelType, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = LiMDBColumns.LiMChannelColumns.channel_id + "=? and " + LiMDBColumns.LiMChannelColumns.channel_type + "=?";
        String[] whereValue = new String[2];
        whereValue[0] = channelID;
        whereValue[1] = channelType + "";
        LiMaoIMApplication.getInstance().getDbHelper()
                .update(LiMDBTables.channel_tab, updateKey, updateValue, where, whereValue);
    }
    public synchronized LiMChannel serializableChannel(Cursor cursor) {
        LiMChannel liMChannel = new LiMChannel();
        if (cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.id) >= 0)
            liMChannel.id = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.id));
        liMChannel.channelID = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.channel_id));
        liMChannel.channelType = (byte) cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.channel_type));
        liMChannel.channelName = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.channel_name));
        liMChannel.channelRemark = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.channel_remark));
        liMChannel.showNick = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.show_nick));
        liMChannel.top = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.top));
        liMChannel.mute = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.mute));
        liMChannel.isDeleted = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.is_deleted));
        liMChannel.forbidden = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.forbidden));
        if (cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.status) >= 0)
            liMChannel.status = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.status));
        liMChannel.follow = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.follow));
        liMChannel.invite = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.invite));
        if (cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.version) >= 0)
            liMChannel.version = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.version));
        if (cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.created_at) >= 0)
            liMChannel.createdAt = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.created_at));
        if (cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.updated_at) >= 0)
            liMChannel.updatedAt = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.updated_at));
        liMChannel.avatar = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.avatar));
        liMChannel.online = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.online));
        liMChannel.lastOffline = cursor.getLong(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.last_offline));
        liMChannel.category = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.category));
        liMChannel.receipt = cursor.getInt(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.receipt));
        String extra = "";
        if (cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.extra) >= 0)
            extra = cursor.getString(cursor.getColumnIndex(LiMDBColumns.LiMChannelColumns.extra));
        liMChannel.extraMap = getChannelExtra(extra);
        return liMChannel;
    }

    public HashMap<String, Object> getChannelExtra(String extra) {
        HashMap<String, Object> hashMap = new HashMap<>();
        if (!TextUtils.isEmpty(extra)) {

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
        }
        return hashMap;
    }

}
