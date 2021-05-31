package com.xinbida.limaoim.manager;

import com.xinbida.limaoim.db.LiMConversationDbManager;
import com.xinbida.limaoim.db.LiMDBColumns;
import com.xinbida.limaoim.db.LiMMsgDbManager;
import com.xinbida.limaoim.entity.LiMConversationMsg;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.entity.LiMUIConversationMsg;
import com.xinbida.limaoim.interfaces.IDeleteConversationMsg;
import com.xinbida.limaoim.interfaces.IRefreshConversationMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * 5/21/21 12:12 PM
 * 最近会话管理
 */
public class LiMConversationManager extends LiMBaseManager {
    private LiMConversationManager() {
    }

    private static class LiMConversationManagerBinder {
        static final LiMConversationManager manager = new LiMConversationManager();
    }

    public static LiMConversationManager getInstance() {
        return LiMConversationManagerBinder.manager;
    }

    //获取提醒管理
    public LiMReminderManager getLiMReminderManager() {
        return LiMReminderManager.getInstance();
    }

    //监听刷新最近会话
    private List<IRefreshConversationMsg> refreshMsgList;

    //移除某个会话
    private List<IDeleteConversationMsg> iDeleteMsgList;

    /**
     * 修改某个会话红点数量
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     * @param count       未读数量
     */
    public void updateMsgRedDotCount(String channelId, byte channelType, int count) {
        LiMConversationDbManager.getInstance().updateMsgCount(channelId, channelType, count);
    }

    /**
     * 修改消息浏览至某个messageSeq
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     */
    public boolean updateMsgBrowseTo(String channelID, byte channelType) {
        long messageSeq = LiMMsgDbManager.getInstance().getMaxMessageSeq(channelID, channelType);
        return LiMConversationDbManager.getInstance().updateMsgBrowseTo(channelID, channelType, messageSeq);
    }

    /**
     * 修改最后一条消息的时间
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param time        时间戳10位
     * @return boolean
     */
    public boolean updateLastMsgTime(String channelID, byte channelType, long time) {
        return LiMConversationDbManager.getInstance().updateMsg(channelID, channelType, LiMDBColumns.LiMCoverMessageColumns.last_msg_timestamp, String.valueOf(time));
    }

    /**
     * 获取某个频道浏览至的messageSeq
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return messageSeq
     */
    public long getMsgBrowseToMessageSeq(String channelID, byte channelType) {
        return LiMConversationDbManager.getInstance().getMsgBrowseTo(channelID, channelType);
    }

    /**
     * 查询会话记录消息
     *
     * @return 最近会话集合
     */
    public List<LiMUIConversationMsg> queryMsgList() {
        return LiMConversationDbManager.getInstance().getAll();
    }

    /**
     * 查询某条消息
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return LiMConversationMsg
     */
    public LiMConversationMsg getConversationMsg(String channelID, byte channelType) {
        return LiMConversationDbManager.getInstance().getConversationMsg(channelID, channelType);
    }

    public void addOrUpdateConversationMsg(LiMConversationMsg mConversationMsg) {
        LiMMsg liMMsg = LiMMsgDbManager.getInstance().getMsgMaxOrderSeqWithChannel(mConversationMsg.channelID, mConversationMsg.channelType);
        if (liMMsg != null) {
            mConversationMsg.lastClientMsgNO = liMMsg.clientMsgNO;
            mConversationMsg.lastMsgID = liMMsg.messageID;
            mConversationMsg.lastMsgContent = liMMsg.content;
            mConversationMsg.clientSeq = liMMsg.clientSeq;
        }
        LiMConversationDbManager.getInstance().updateMsg(mConversationMsg.channelID, mConversationMsg.channelType, mConversationMsg.lastClientMsgNO, mConversationMsg.unreadCount, mConversationMsg.clientSeq);
    }

    /**
     * 删除某个会话记录信息
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     */
    public boolean deleteMsg(String channelId, byte channelType) {
        return LiMConversationDbManager.getInstance().deleteMsg(channelId, channelType);
    }

    /**
     * 清除所有最近会话
     */
    public boolean clearAll() {
        return LiMConversationDbManager.getInstance().clearEmpty();
    }


    /**
     * 监听刷新最近会话
     *
     * @param listener 回调
     */
    public void addOnRefreshMsg(IRefreshConversationMsg listener) {
        if (refreshMsgList == null)
            refreshMsgList = new ArrayList<>();
        if (listener != null)
            refreshMsgList.add(listener);
    }

    /**
     * 设置刷新最近会话
     */
    public void setOnRefreshMsg(LiMUIConversationMsg liMUIConversationMsg, boolean isEnd) {
        if (refreshMsgList != null && refreshMsgList.size() > 0) {
            runOnMainThread(() -> {
                for (int i = 0, size = refreshMsgList.size(); i < size; i++) {
                    if (refreshMsgList.get(i) != null)
                        refreshMsgList.get(i).onRefreshConversationMsg(liMUIConversationMsg, isEnd);
                }
            });
        }
    }

    //监听删除最近会话监听
    public void addOnDeleteMsgListener(IDeleteConversationMsg listener) {
        if (iDeleteMsgList == null) iDeleteMsgList = new ArrayList<>();
        iDeleteMsgList.add(listener);
    }

    // 删除某个最近会话
    public void setDeleteMsg(String channelID, byte channelType) {
        if (iDeleteMsgList != null && iDeleteMsgList.size() > 0) {
            runOnMainThread(() -> {
                for (int i = 0, size = iDeleteMsgList.size(); i < size; i++) {
                    if (iDeleteMsgList.get(i) != null)
                        iDeleteMsgList.get(i).onDelete(channelID, channelType);
                }
            });
        }
    }
}
