package com.xinbida.limaoim.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 2020-05-10 19:16
 * 狸猫频道搜索结果
 */
public class LiMChannelSearchResult implements Parcelable {
    //频道信息
    public LiMChannel liMChannel;
    //包含的成员名称
    public String containMemberName;

    public LiMChannelSearchResult() {
    }

    protected LiMChannelSearchResult(Parcel in) {
        liMChannel = in.readParcelable(LiMChannel.class.getClassLoader());
        containMemberName = in.readString();
    }

    public static final Creator<LiMChannelSearchResult> CREATOR = new Creator<LiMChannelSearchResult>() {
        @Override
        public LiMChannelSearchResult createFromParcel(Parcel in) {
            return new LiMChannelSearchResult(in);
        }

        @Override
        public LiMChannelSearchResult[] newArray(int size) {
            return new LiMChannelSearchResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(liMChannel, flags);
        dest.writeString(containMemberName);
    }
}
