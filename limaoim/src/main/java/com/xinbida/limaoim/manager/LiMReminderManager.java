package com.xinbida.limaoim.manager;

import android.text.TextUtils;

import com.xinbida.limaoim.db.LiMConversationDbManager;
import com.xinbida.limaoim.db.LiMDBColumns;
import com.xinbida.limaoim.entity.LiMConversationMsg;
import com.xinbida.limaoim.entity.LiMReminder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 5/21/21 12:13 PM
 * 提醒管理
 */
public class LiMReminderManager {
    private LiMReminderManager() {
    }

    private static class LiMRemindManagerBinder {
        final static LiMReminderManager manager = new LiMReminderManager();
    }

    public static LiMReminderManager getInstance() {
        return LiMRemindManagerBinder.manager;
    }

    /**
     * 追加提醒内容
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param liMReminder 高光字段
     * @param timestamp   修改时间
     */
    public void appendReminder(String channelID, byte channelType, LiMReminder liMReminder, long timestamp) {
        List<LiMReminder> list = getReminders(channelID, channelType);

        boolean isAdd = true;
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).type == liMReminder.type) {
                isAdd = false;
                list.get(i).text = liMReminder.text;
                list.get(i).data = liMReminder.data;
                break;
            }
        }
        if (isAdd) {
            list.add(liMReminder);
        }
        JSONArray jsonArray = new JSONArray();
        for (int i = 0, size = list.size(); i < size; i++) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("type", list.get(i).type);
                jsonObject.put("text", list.get(i).text);
                jsonObject.put("data", list.get(i).data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(jsonObject);
        }
        if (timestamp == 0) {
            LiMConversationDbManager.getInstance().updateMsg(channelID, channelType, LiMDBColumns.LiMCoverMessageColumns.reminders, jsonArray.toString());
        } else {
            List<String> keys = new ArrayList<>();
            keys.add(LiMDBColumns.LiMCoverMessageColumns.reminders);
            keys.add(LiMDBColumns.LiMCoverMessageColumns.last_msg_timestamp);
            List<String> values = new ArrayList<>();
            values.add(jsonArray.toString());
            values.add(timestamp + "");
            LiMConversationDbManager.getInstance().updateMsg(channelID, channelType, keys, values);
        }

    }

    /**
     * 追加提醒内容
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param liMReminder 高光字段
     */
    private void appendReminder(String channelID, byte channelType, LiMReminder liMReminder) {
        appendReminder(channelID, channelType, liMReminder, 0);
    }


    /**
     * 删除某个会话的某个类型提醒
     *
     * @param channelID       频道ID
     * @param channelType     频道类型
     * @param limReminderType 提醒类型
     */
    public void deleteReminder(String channelID, byte channelType, int limReminderType) {
        List<LiMReminder> list = getReminders(channelID, channelType);
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).type == limReminderType) {
                list.remove(i);
                break;
            }
        }

        saveReminder(channelID, channelType, list);
    }

    /**
     * 清除某个频道的所有提醒
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     */
    public void clearAllReminder(String channelID, byte channelType) {
        LiMConversationDbManager.getInstance().updateMsg(channelID, channelType, LiMDBColumns.LiMCoverMessageColumns.reminders, "");
    }

    /**
     * 获取某个类型的提醒
     *
     * @param channelID       频道ID
     * @param channelType     频道类型
     * @param limReminderType 提醒类型
     * @return LiMReminder
     */
    public LiMReminder getReminder(String channelID, byte channelType, int limReminderType) {
        List<LiMReminder> list = getReminders(channelID, channelType);
        LiMReminder liMReminder = null;
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).type == limReminderType) {
                liMReminder = list.get(i);
                break;
            }
        }
        return liMReminder;
    }

    /**
     * 查询某个会话的高光内容
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return List<LiMReminder>
     */
    public List<LiMReminder> getReminders(String channelID, byte channelType) {
        LiMConversationMsg liMConversationMsg = LiMConversationDbManager.getInstance().queryLastLiMConversationMsg(channelID, channelType);
        List<LiMReminder> list = new ArrayList<>();
        if (liMConversationMsg != null && !TextUtils.isEmpty(liMConversationMsg.reminders)) {
            String reminders = liMConversationMsg.reminders;
            try {
                JSONArray jsonArray = new JSONArray(reminders);
                if (jsonArray.length() > 0) {
                    for (int i = 0, size = jsonArray.length(); i < size; i++) {
                        LiMReminder liMReminder = new LiMReminder();
                        liMReminder.type = jsonArray.getJSONObject(i).optInt("type");
                        liMReminder.text = jsonArray.getJSONObject(i).optString("text");
                        liMReminder.data = jsonArray.getJSONObject(i).opt("data");
                        list.add(liMReminder);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * 设置某个频道的提醒
     *
     * @param channelID       频道ID
     * @param channelType     频道类型
     * @param liMReminderList 提醒
     */
    public void setReminders(String channelID, byte channelType, List<LiMReminder> liMReminderList) {
        saveReminder(channelID, channelType, liMReminderList);
    }

    /**
     * 保存某个频道的提醒
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param list        提醒数据
     */
    private void saveReminder(String channelID, byte channelType, List<LiMReminder> list) {
        JSONArray jsonArray = new JSONArray();
        if (list != null && list.size() > 0) {
            for (int i = 0, size = list.size(); i < size; i++) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", list.get(i).type);
                    jsonObject.put("text", list.get(i).text);
                    jsonObject.put("data", list.get(i).data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(jsonObject);
            }
        }

        LiMConversationDbManager.getInstance().updateMsg(channelID, channelType, LiMDBColumns.LiMCoverMessageColumns.reminders, jsonArray.toString());
    }

}
