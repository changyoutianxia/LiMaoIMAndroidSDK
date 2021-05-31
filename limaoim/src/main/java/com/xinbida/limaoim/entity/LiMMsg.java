package com.xinbida.limaoim.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.manager.LiMChannelManager;
import com.xinbida.limaoim.manager.LiMChannelMembersManager;
import com.xinbida.limaoim.message.type.LiMChannelType;
import com.xinbida.limaoim.message.type.LiMSendMsgResult;
import com.xinbida.limaoim.protocol.LiMMessageContent;
import com.xinbida.limaoim.utils.LiMDateUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * 2019-11-09 14:33
 * 消息体 对应 #DBMsgColumns 中字段
 *
 */
public class LiMMsg implements Parcelable {

    //服务器消息ID(全局唯一，无序)
    public String messageID;
    //服务器消息序号(有序递增)
    public int messageSeq;
    //客户端序号
    public long clientSeq;
    //消息时间10位时间戳
    public long timestamp;
    //消息来源发送者
    public String fromUID;
    //频道id
    public String channelID;
    //频道类型
    public byte channelType;
    //消息正文类型
    public int type;
    //消息内容Json
    public String content;
    //发送状态
    public int status;
    //语音是否已读
    public int voiceStatus;
    //是否被删除
    public int isDeleted;
    //创建时间
    public String createdAt;
    //修改时间
    public String updatedAt;
    //扩展字段
    public HashMap extraMap;
    //搜索关键字
    public String searchableWord;
    //自定义消息实体
    public LiMMessageContent baseContentMsgModel;
    //消息来源频道
    private LiMChannel from;
    //会话频道
    private LiMChannel channelInfo;
    //消息频道成员
    private LiMChannelMember memberOfFrom;
    //客户端消息ID
    public String clientMsgNO;
    //排序编号
    public long orderSeq;
    //是否被撤回
    public int revoke;
    //撤回者
    public String revoker;
    //扩展消息版本号
    public long extraVersion;
    //已读数量
    public int readedCount;
    //未读数量
    public int unreadCount;
    //本人是否已读
    public int readed;
    //消息是否回执
    public int receipt;
    //是否持久化[是否保存在数据库]
    public boolean no_persist;
    //对方是否显示红点
    public boolean red_dot = true;
    //消息是否只同步一次
    public boolean sync_once;
    //消息回应
    public List<LiMMsgReaction> reactionList;

    public LiMMsg() {
        super();
        this.timestamp = LiMDateUtils.getInstance().getCurrentSeconds();
        this.createdAt = LiMDateUtils.getInstance().time2DateStr(timestamp);
        this.updatedAt = LiMDateUtils.getInstance().time2DateStr(timestamp);
        this.messageSeq = 0;
        status = LiMSendMsgResult.send_loading;
        String msgNo = LiMaoIMApplication.getInstance().getUid() + ":"
                + channelType + ":" + channelID + ":"
                + LiMDateUtils.getInstance().getCurrentMills();
        clientMsgNO = UUID.randomUUID().toString().replaceAll("-", "");
    }

    protected LiMMsg(Parcel in) {
        revoke = in.readInt();
        orderSeq = in.readLong();
        isDeleted = in.readInt();
        clientMsgNO = in.readString();
        messageID = in.readString();
        messageSeq = in.readInt();
        clientSeq = in.readLong();
        timestamp = in.readLong();
        fromUID = in.readString();
        channelID = in.readString();
        channelType = in.readByte();
        type = in.readInt();
        content = in.readString();
        status = in.readInt();
        voiceStatus = in.readInt();
        createdAt = in.readString();
        updatedAt = in.readString();
        searchableWord = in.readString();
        extraMap = in.readHashMap(HashMap.class.getClassLoader());
        baseContentMsgModel = in.readParcelable(LiMMsg.class
                .getClassLoader());
        from = in.readParcelable(LiMChannel.class.getClassLoader());
        memberOfFrom = in.readParcelable(LiMChannelMember.class.getClassLoader());
        channelInfo = in.readParcelable(LiMChannelMember.class.getClassLoader());
        revoker = in.readString();
        extraVersion = in.readLong();
        readedCount = in.readInt();
        unreadCount = in.readInt();
        readed = in.readInt();
        receipt = in.readInt();
        no_persist = in.readByte() != 0;
        red_dot = in.readByte() != 0;
        sync_once = in.readByte() != 0;
        reactionList = in.createTypedArrayList(LiMMsgReaction.CREATOR);
    }

    public static final Creator<LiMMsg> CREATOR = new Creator<LiMMsg>() {
        @Override
        public LiMMsg createFromParcel(Parcel in) {
            return new LiMMsg(in);
        }

        @Override
        public LiMMsg[] newArray(int size) {
            return new LiMMsg[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(revoke);
        dest.writeLong(orderSeq);
        dest.writeInt(isDeleted);
        dest.writeString(clientMsgNO);
        dest.writeString(messageID);
        dest.writeInt(messageSeq);
        dest.writeLong(clientSeq);
        dest.writeLong(timestamp);
        dest.writeString(fromUID);
        dest.writeString(channelID);
        dest.writeByte(channelType);
        dest.writeInt(type);
        dest.writeString(content);
        dest.writeInt(status);
        dest.writeInt(voiceStatus);
        dest.writeString(createdAt);
        dest.writeString(updatedAt);
        dest.writeString(searchableWord);
        dest.writeMap(extraMap);
        dest.writeParcelable(baseContentMsgModel, flags);
        dest.writeParcelable(from, flags);
        dest.writeParcelable(memberOfFrom, flags);
        dest.writeParcelable(channelInfo, flags);
        dest.writeString(revoker);
        dest.writeLong(extraVersion);
        dest.writeInt(readedCount);
        dest.writeInt(unreadCount);
        dest.writeInt(readed);
        dest.writeInt(receipt);
        dest.writeByte((byte) (no_persist ? 1 : 0));
        dest.writeByte((byte) (red_dot ? 1 : 0));
        dest.writeByte((byte) (sync_once ? 1 : 0));
        dest.writeTypedList(reactionList);
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

    public LiMChannel getChannelInfo() {
        if (channelInfo == null) {
            channelInfo = LiMChannelManager.getInstance().getLiMChannel(channelID, channelType);
        }
        return channelInfo;
    }

    public void setChannelInfo(LiMChannel channelInfo) {
        this.channelInfo = channelInfo;
    }

    public LiMChannel getFrom() {
        if (from == null)
            from = LiMChannelManager.getInstance().getLiMChannel(fromUID, LiMChannelType.PERSONAL);
        return from;
    }

    public void setFrom(LiMChannel channelInfo) {
        from = channelInfo;
    }

    public LiMChannelMember getMemberOfFrom() {
        if (memberOfFrom == null)
            memberOfFrom = LiMChannelMembersManager.getInstance().getLiMChannelMember(channelID, channelType, fromUID);
        return memberOfFrom;
    }

    public void setMemberOfFrom(LiMChannelMember memberOfFrom) {
        this.memberOfFrom = memberOfFrom;
    }
}
