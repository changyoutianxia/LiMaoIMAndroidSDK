package com.xinbida.limaoim.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

import com.xinbida.limaoim.message.type.LiMMsgContentType;
import com.xinbida.limaoim.protocol.LiMMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-04-29 23:01
 * 视频消息
 */
public class LiMVideoContent extends LiMMediaMessageContent {
    public String cover;
    public String coverLocalPath;
    public long size;
    public int width;
    public int height;
    public long second;

    public LiMVideoContent() {
        type = LiMMsgContentType.LIMAO_VIDEO;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("cover", cover);
            jsonObject.put("coverLocalPath", coverLocalPath);
            jsonObject.put("size", size);
            jsonObject.put("width", width);
            jsonObject.put("height", height);
            jsonObject.put("second", second);
            jsonObject.put("url", url);
            jsonObject.put("localPath", localPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LiMMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject.has("cover"))
            cover = jsonObject.optString("cover");
        if (jsonObject.has("coverLocalPath"))
            coverLocalPath = jsonObject.optString("coverLocalPath");
        if (jsonObject.has("size"))
            size = jsonObject.optInt("size");
        if (jsonObject.has("width"))
            width = jsonObject.optInt("width");
        if (jsonObject.has("height"))
            height = jsonObject.optInt("height");
        if (jsonObject.has("second"))
            second = jsonObject.optInt("second");
        if (jsonObject.has("url"))
            url = jsonObject.optString("url");
        if (jsonObject.has("localPath"))
            localPath = jsonObject.optString("localPath");
        return this;
    }


    protected LiMVideoContent(Parcel in) {
        super(in);
        cover = in.readString();
        coverLocalPath = in.readString();
        size = in.readLong();
        width = in.readInt();
        height = in.readInt();
        second = in.readLong();
        url = in.readString();
        localPath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(cover);
        dest.writeString(coverLocalPath);
        dest.writeLong(size);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeLong(second);
        dest.writeString(url);
        dest.writeString(localPath);
    }


    public static final Parcelable.Creator<LiMVideoContent> CREATOR = new Parcelable.Creator<LiMVideoContent>() {
        @Override
        public LiMVideoContent createFromParcel(Parcel in) {
            return new LiMVideoContent(in);
        }

        @Override
        public LiMVideoContent[] newArray(int size) {
            return new LiMVideoContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getDisplayContent() {
        return "[视频]";
    }

    @Override
    public String getSearchableWord() {
        return "[视频]";
    }
}
