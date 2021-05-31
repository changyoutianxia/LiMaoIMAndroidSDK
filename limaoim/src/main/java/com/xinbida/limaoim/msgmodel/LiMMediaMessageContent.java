package com.xinbida.limaoim.msgmodel;

import android.os.Parcel;

import com.xinbida.limaoim.protocol.LiMMessageContent;

/**
 * 2020-04-04 10:39
 * 多媒体消息
 */
public abstract class LiMMediaMessageContent extends LiMMessageContent {
    public String localPath;//本地地址
    public String url;//网络地址

    public LiMMediaMessageContent() {
    }

    protected LiMMediaMessageContent(Parcel in) {
        super(in);
    }
}
