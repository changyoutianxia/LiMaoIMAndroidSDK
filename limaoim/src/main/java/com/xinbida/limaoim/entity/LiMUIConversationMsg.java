package com.xinbida.limaoim.entity;


import com.xinbida.limaoim.db.LiMMsgDbManager;
import com.xinbida.limaoim.manager.LiMChannelManager;

import java.util.HashMap;
import java.util.List;

/**
 * 2019-12-01 17:50
 * UI层显示最近会话消息
 */
public class LiMUIConversationMsg {
    public String clientMsgNo;
    //频道ID
    public String channelID;
    //频道类型
    public byte channelType;
    //最后一条消息时间
    public long lastMsgTimestamp;
    //消息频道
    private LiMChannel liMChannel;
    //消息正文
    private LiMMsg liMMsg;
    //最后一条消息发送状态
    public int status;
    //未读消息数量
    public int unreadCount;
    //置顶
    public int top;
    //免打扰
    public int mute;
    //高亮内容[{type:1,text:'[有人@你]'}]
    public List<LiMReminder> mentionList;
    //扩展字段
    public HashMap<String, Object> extraMap;

    public LiMMsg getLiMMsg() {
        if (liMMsg == null) {
            liMMsg = LiMMsgDbManager.getInstance().getMsgWithClientMsgNo(clientMsgNo);
        }
        return liMMsg;
    }

    public void setLiMMsg(LiMMsg liMMsg) {
        this.liMMsg = liMMsg;
    }

    public LiMChannel getLiMChannel() {
        if (liMChannel == null) {
            liMChannel = LiMChannelManager.getInstance().getLiMChannel(channelID, channelType);
        }
        return liMChannel;
    }

    public void setLiMChannel(LiMChannel liMChannel) {
        this.liMChannel = liMChannel;
    }
}
