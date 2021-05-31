package com.xinbida.limaoim.entity;


import com.xinbida.limaoim.utils.LiMDateUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * 2019-11-09 15:00
 * 会话列表消息
 */
public class LiMConversationMsg {
    //自增ID
    public long id;
    //频道id
    public String channelID;
    //频道类型
    public byte channelType;
    //本地自增id
    public long clientSeq = 0;
    //最后一条消息id
    public String lastMsgID;
    //最后一条消息内容
    public String lastMsgContent;
    //最后一条消息本地ID
    public String lastClientMsgNO;
    //已预览到的message_seq
    public long browseTo;
    //是否删除
    public int isDeleted;
    //服务器同步版本号
    public long version;
    //最后一条消息时间
    public long lastMsgTimestamp;
    //消息正文类型
    public int type;
    //最后一条消息发送状态
    public int status;
    //是否提到(@)
    public int mention;
    //未读消息数量
    public int unreadCount;
    //会话标题
    public String title;
    //发送者id
    public String fromUID;
    //正在编辑的内容
    public String reminders;
    //扩展字段
    public HashMap extraMap;

    public LiMConversationMsg() {
        this.lastMsgTimestamp = LiMDateUtils.getInstance().getCurrentSeconds();
    }

    public String getExtras() {
        String extras = "";
        if (extraMap != null) {
            JSONObject jsonObject = new JSONObject();
            for (Object key : extraMap.keySet()) {
                try {
                    jsonObject.put(key.toString(), extraMap.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            extras = jsonObject.toString();
        }
        return extras;
    }

    @Override
    public String toString() {
        return "LiMConversationMsg{" +
                "id=" + id +
                ", channelID='" + channelID + '\'' +
                ", channelType=" + channelType +
                ", clientSeq=" + clientSeq +
                ", lastMsgID='" + lastMsgID + '\'' +
                ", lastMsgContent='" + lastMsgContent + '\'' +
                ", lastClientMsgNO='" + lastClientMsgNO + '\'' +
                ", browseTo=" + browseTo +
                ", isDeleted=" + isDeleted +
                ", version=" + version +
                ", lastMsgTimestamp=" + lastMsgTimestamp +
                ", type=" + type +
                ", status=" + status +
                ", mention=" + mention +
                ", unreadCount=" + unreadCount +
                ", title='" + title + '\'' +
                ", fromUID='" + fromUID + '\'' +
                ", reminders='" + reminders + '\'' +
                ", extraMap=" + extraMap +
                '}';
    }
}
