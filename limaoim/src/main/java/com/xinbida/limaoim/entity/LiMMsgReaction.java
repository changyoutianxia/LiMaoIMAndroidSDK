package com.xinbida.limaoim.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 4/16/21 1:52 PM
 * 消息回应
 */
public class LiMMsgReaction implements Parcelable {
    public String messageID;
    public String channelID;
    public byte channelType;
    public String uid;
    public String name;
    public long seq;
    public String emoji;
    public int isDeleted;
    public String createdAt;

    public LiMMsgReaction() {
    }

    protected LiMMsgReaction(Parcel in) {
        messageID = in.readString();
        channelID = in.readString();
        channelType = in.readByte();
        uid = in.readString();
        name = in.readString();
        seq = in.readLong();
        emoji = in.readString();
        isDeleted = in.readInt();
        createdAt = in.readString();
    }

    public static final Creator<LiMMsgReaction> CREATOR = new Creator<LiMMsgReaction>() {
        @Override
        public LiMMsgReaction createFromParcel(Parcel in) {
            return new LiMMsgReaction(in);
        }

        @Override
        public LiMMsgReaction[] newArray(int size) {
            return new LiMMsgReaction[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(messageID);
        dest.writeString(channelID);
        dest.writeByte(channelType);
        dest.writeString(uid);
        dest.writeString(name);
        dest.writeLong(seq);
        dest.writeString(emoji);
        dest.writeInt(isDeleted);
        dest.writeString(createdAt);
    }
}
