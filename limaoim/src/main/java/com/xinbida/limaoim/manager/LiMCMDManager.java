package com.xinbida.limaoim.manager;

import android.text.TextUtils;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.db.LiMDBColumns;
import com.xinbida.limaoim.db.LiMMsgDbManager;
import com.xinbida.limaoim.entity.LiMCMD;
import com.xinbida.limaoim.entity.LiMCMDKeys;
import com.xinbida.limaoim.entity.LiMChannel;
import com.xinbida.limaoim.interfaces.ICMDListener;
import com.xinbida.limaoim.message.type.LiMChannelType;
import com.xinbida.limaoim.utils.LiMDateUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/21/21 11:54 AM
 * cmd manager
 */
public class LiMCMDManager extends LiMBaseManager {
    private LiMCMDManager() {
    }

    private static class LiMCMDManagerBinder {
        static final LiMCMDManager cmdManager = new LiMCMDManager();
    }

    public static LiMCMDManager getInstance() {
        return LiMCMDManagerBinder.cmdManager;
    }

    private ConcurrentHashMap<String, ICMDListener> cmdListenerMap;

    public void handleCMD(JSONObject jsonObject, String channelID, byte channelType) {
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        try {
            if (!jsonObject.has("channel_id"))
                jsonObject.put("channel_id", channelID);
            if (!jsonObject.has("channel_type"))
                jsonObject.put("channel_type", channelType);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        handleCMD(jsonObject);
    }

    public void handleCMD(JSONObject json) {
        if (json == null) return;
        //内部消息
        if (json.has("cmd")) {
            String cmd = json.optString("cmd");
            JSONObject jsonObject = null;
            if (json.has("param")) {
                jsonObject = json.optJSONObject("param");
            }
            if (jsonObject == null) {
                jsonObject = new JSONObject();
            }
            try {
                if (json.has("channel_id") && !jsonObject.has("channel_id")) {
                    jsonObject.put("channel_id", json.optString("channel_id"));
                }
                if (json.has("channel_type") && !jsonObject.has("channel_type")) {
                    jsonObject.put("channel_type", json.optString("channel_type"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (cmd.equalsIgnoreCase(LiMCMDKeys.lim_memberUpdate)) {
                //更新频道成员
                if (jsonObject.has("group_no")) {
                    String group_no = jsonObject.optString("group_no");
                    LiMChannelMembersManager.getInstance().setOnSyncChannelMembers(group_no, LiMChannelType.GROUP);
                }
            } else if (cmd.equalsIgnoreCase(LiMCMDKeys.lim_groupAvatarUpdate)) {
                //更新频道头像
                if (jsonObject.has("group_no")) {
                    String group_no = jsonObject.optString("group_no");
                    LiMaoIM.getInstance().getLiMChannelManager().setOnRefreshChannelAvatar(group_no, LiMChannelType.GROUP);
                }
            } else if (cmd.equals(LiMCMDKeys.lim_userAvatarUpdate)) {
                //个人头像更新
                if (jsonObject.has("uid")) {
                    String uid = jsonObject.optString("uid");
                    LiMaoIM.getInstance().getLiMChannelManager().setOnRefreshChannelAvatar(uid, LiMChannelType.PERSONAL);
                }
            } else if (cmd.equalsIgnoreCase(LiMCMDKeys.lim_channelUpdate)) {
                //频道修改
                if (jsonObject.has("channel_id") && jsonObject.has("channel_type")) {
                    String channelID = jsonObject.optString("channel_id");
                    byte channelType = (byte) jsonObject.optInt("channel_type");
                    LiMaoIM.getInstance().getLiMChannelManager().fetchChannelInfo(channelID, channelType);
                }
            } else if (cmd.equalsIgnoreCase(LiMCMDKeys.lim_unreadClear)) {
                //清除消息红点
                if (jsonObject.has("channel_id") && jsonObject.has("channel_type")) {
                    String channelId = jsonObject.optString("channel_id");
                    int channelType = jsonObject.optInt("channel_type");
                    LiMaoIM.getInstance().getLiMConversationManager().updateMsgRedDotCount(channelId, (byte) channelType, 0);
                }
            } else if (cmd.equalsIgnoreCase(LiMCMDKeys.lim_voiceReaded)) {
                //语音已读
                if (jsonObject.has("message_id")) {
                    String messageId = jsonObject.optString("message_id");
                    LiMMsgDbManager.getInstance().updateMsgWithMessageID(messageId, LiMDBColumns.LiMMessageColumns.voice_status, 1 + "");
                }
            } else if (cmd.equalsIgnoreCase(LiMCMDKeys.lim_onlineStatus)) {
                //对方是否在线
//                int device_flag = jsonObject.optInt("device_flag");
                int online = jsonObject.optInt("online");
                String uid = jsonObject.optString("uid");
                LiMChannel liMChannel = LiMaoIM.getInstance().getLiMChannelManager().getLiMChannel(uid, LiMChannelType.PERSONAL);
                if (liMChannel != null) {
                    liMChannel.online = online;
                    if (liMChannel.online == 0) {
                        liMChannel.lastOffline = LiMDateUtils.getInstance().getCurrentMills();
                    }
                    LiMaoIM.getInstance().getLiMChannelManager().addOrUpdateChannel(liMChannel);
                }
            } else if (cmd.equals(LiMCMDKeys.lim_noticeOffline)) {
                // 通知下线
                LiMaoIMApplication.getInstance().setToken("");
            } else if (cmd.equals(LiMCMDKeys.lim_syncMessageReaction)) {
                if (jsonObject.has("channel_id") && jsonObject.has("channel_type")) {
                    String channelId = jsonObject.optString("channel_id");
                    byte channelType = (byte) jsonObject.optInt("channel_type");
                    LiMaoIM.getInstance().getLiMMsgManager().setSyncMsgReaction(channelId, channelType);
                }

            }

            LiMCMD liMCMD = new LiMCMD(cmd, jsonObject);
            pushCMDs(liMCMD);
        }

    }

    /**
     * 处理cmd
     *
     * @param cmd   cmd
     * @param param 参数
     */
    public void handleCMD(String cmd, String param) {
        if (TextUtils.isEmpty(cmd)) return;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("cmd", cmd);
            if (!TextUtils.isEmpty(param)) {
                JSONObject paramJson = new JSONObject(param);
                jsonObject.put("param", paramJson);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        handleCMD(jsonObject);
    }

    public synchronized void addCmdListener(String key, ICMDListener icmdListener) {
        if (TextUtils.isEmpty(key) || icmdListener == null) return;
        if (cmdListenerMap == null) cmdListenerMap = new ConcurrentHashMap<>();
        cmdListenerMap.put(key, icmdListener);
    }

    public void removeCmdListener(String key) {
        if (TextUtils.isEmpty(key) || cmdListenerMap == null) return;
        cmdListenerMap.remove(key);
    }

    private void pushCMDs(LiMCMD liMCMD) {
        if (cmdListenerMap != null && cmdListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, ICMDListener> entry : cmdListenerMap.entrySet()) {
                    entry.getValue().onMsg(liMCMD);
                }
            });
        }
    }
}
