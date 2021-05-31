package com.xinbida.limaoim.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * 2020-10-22 13:28
 * 提醒对象
 */
public class LiMMentionInfo implements Parcelable {

    public boolean isMentionMe;
    public List<String> uids;

    public LiMMentionInfo() {
    }

    protected LiMMentionInfo(Parcel in) {
        isMentionMe = in.readByte() != 0;
        uids = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isMentionMe ? 1 : 0));
        dest.writeStringList(uids);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LiMMentionInfo> CREATOR = new Creator<LiMMentionInfo>() {
        @Override
        public LiMMentionInfo createFromParcel(Parcel in) {
            return new LiMMentionInfo(in);
        }

        @Override
        public LiMMentionInfo[] newArray(int size) {
            return new LiMMentionInfo[size];
        }
    };
}
