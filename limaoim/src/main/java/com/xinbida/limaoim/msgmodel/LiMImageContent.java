package com.xinbida.limaoim.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

import com.xinbida.limaoim.message.type.LiMMsgContentType;
import com.xinbida.limaoim.protocol.LiMMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-04-04 10:42
 * 图片消息
 */
public class LiMImageContent extends LiMMediaMessageContent {
    public int width;
    public int height;

    public LiMImageContent(String localPath) {
        this.localPath = localPath;
        this.type = LiMMsgContentType.LIMAO_IMAGE;
    }

    // 无参构造必须提供
    public LiMImageContent() {
        this.type = LiMMsgContentType.LIMAO_IMAGE;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("url", url);
            jsonObject.put("width", width);
            jsonObject.put("height", height);
            jsonObject.put("localPath", localPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LiMMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject.has("url"))
            this.url = jsonObject.optString("url");
        if (jsonObject.has("localPath"))
            this.localPath = jsonObject.optString("localPath");
        if (jsonObject.has("height"))
            this.height = jsonObject.optInt("height");
        if (jsonObject.has("width"))
            this.width = jsonObject.optInt("width");
        return this;
    }


    protected LiMImageContent(Parcel in) {
        super(in);
        width = in.readInt();
        height = in.readInt();
        url = in.readString();
        localPath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeString(url);
        dest.writeString(localPath);
    }


    public static final Parcelable.Creator<LiMImageContent> CREATOR = new Parcelable.Creator<LiMImageContent>() {
        @Override
        public LiMImageContent createFromParcel(Parcel in) {
            return new LiMImageContent(in);
        }

        @Override
        public LiMImageContent[] newArray(int size) {
            return new LiMImageContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getDisplayContent() {
        return "[图片]";
    }

    @Override
    public String getSearchableWord() {
        return "[图片]";
    }
}
