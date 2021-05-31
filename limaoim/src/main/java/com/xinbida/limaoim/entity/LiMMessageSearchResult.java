package com.xinbida.limaoim.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 2020-05-10 22:26
 * 狸猫消息搜索结果
 */
public class LiMMessageSearchResult implements Parcelable {
    //消息对应的频道信息
    public LiMChannel liMChannel;
    //包含关键字的信息
    public String searchableWord;
    //条数
    public int messageCount;

    public LiMMessageSearchResult() {
    }

    protected LiMMessageSearchResult(Parcel in) {
        liMChannel = in.readParcelable(LiMChannel.class.getClassLoader());
        searchableWord = in.readString();
        messageCount = in.readInt();
    }

    public static final Creator<LiMMessageSearchResult> CREATOR = new Creator<LiMMessageSearchResult>() {
        @Override
        public LiMMessageSearchResult createFromParcel(Parcel in) {
            return new LiMMessageSearchResult(in);
        }

        @Override
        public LiMMessageSearchResult[] newArray(int size) {
            return new LiMMessageSearchResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(liMChannel, flags);
        dest.writeString(searchableWord);
        dest.writeInt(messageCount);
    }
}
