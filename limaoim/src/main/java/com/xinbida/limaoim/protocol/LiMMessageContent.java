package com.xinbida.limaoim.protocol;

import android.os.Parcel;
import android.os.Parcelable;


import com.xinbida.limaoim.entity.LiMMentionInfo;
import com.xinbida.limaoim.msgmodel.LiMReply;

import org.json.JSONObject;

/**
 * 2019-11-10 15:14
 * 基础内容消息实体
 */
public class LiMMessageContent implements Parcelable {
    public boolean isCheckForceSendMsg = true;
    //内容
    public String content;
    //发送者id
    public String from_uid;
    //发送者名称
    public String from_name;
    //消息内容类型
    public int type;
    //是否@所有人
    public int mention_all;
    //@成员列表
    public LiMMentionInfo mentionInfo;
    //回复对象
    public LiMReply reply;
    //搜索关键字
    public String searchableWord;
    //最近会话提示文字
    public String displayContent;
    public int isDelete;
    //消息是否回执
    public int receipt;

    public LiMMessageContent() {
    }

    protected LiMMessageContent(Parcel in) {
        isCheckForceSendMsg = in.readByte() != 0;
        content = in.readString();
        from_uid = in.readString();
        from_name = in.readString();
        type = in.readInt();

        mention_all = in.readInt();
        mentionInfo = in.readParcelable(LiMMentionInfo.class.getClassLoader());
        searchableWord = in.readString();
        displayContent = in.readString();
        reply = in.readParcelable(LiMReply.class.getClassLoader());
        isDelete = in.readInt();
        receipt = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isCheckForceSendMsg ? 1 : 0));
        dest.writeString(content);
        dest.writeString(from_uid);
        dest.writeString(from_name);
        dest.writeInt(type);
        dest.writeInt(mention_all);
        dest.writeParcelable(mentionInfo, flags);
        dest.writeString(searchableWord);
        dest.writeString(displayContent);
        dest.writeParcelable(reply, flags);
        dest.writeInt(isDelete);
        dest.writeInt(receipt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LiMMessageContent> CREATOR = new Creator<LiMMessageContent>() {
        @Override
        public LiMMessageContent createFromParcel(Parcel in) {
            return new LiMMessageContent(in);
        }

        @Override
        public LiMMessageContent[] newArray(int size) {
            return new LiMMessageContent[size];
        }
    };

    public JSONObject encodeMsg() {
        return new JSONObject();
    }

    public LiMMessageContent decodeMsg(JSONObject jsonObject) {
        return this;
    }

    public String getSearchableWord() {
        return content;
    }

    public String getDisplayContent() {
        return displayContent;
    }
}
