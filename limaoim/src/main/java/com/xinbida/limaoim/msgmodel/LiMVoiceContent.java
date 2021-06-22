package com.xinbida.limaoim.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

import com.xinbida.limaoim.message.type.LiMMsgContentType;
import com.xinbida.limaoim.protocol.LiMMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-04-04 10:45
 * 内置语音消息model
 */
public class LiMVoiceContent extends LiMMediaMessageContent {
    public int timeTrad;

    public LiMVoiceContent(String localPath, int timeTrad) {
        this.type = LiMMsgContentType.LIMAO_VOICE;
        this.timeTrad = timeTrad;
        this.localPath = localPath;
    }

    public LiMVoiceContent() {
        this.type = LiMMsgContentType.LIMAO_VOICE;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("localPath", localPath);
            jsonObject.put("timeTrad", timeTrad);
            jsonObject.put("url", url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LiMMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject.has("timeTrad"))
            timeTrad = jsonObject.optInt("timeTrad");
        if (jsonObject.has("localPath"))
            localPath = jsonObject.optString("localPath");
        if (jsonObject.has("url"))
            url = jsonObject.optString("url");
        return this;
    }


    protected LiMVoiceContent(Parcel in) {
        super(in);
        timeTrad = in.readInt();
        url = in.readString();
        localPath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(timeTrad);
        dest.writeString(url);
        dest.writeString(localPath);
    }


    public static final Parcelable.Creator<LiMVoiceContent> CREATOR = new Parcelable.Creator<LiMVoiceContent>() {
        @Override
        public LiMVoiceContent createFromParcel(Parcel in) {
            return new LiMVoiceContent(in);
        }

        @Override
        public LiMVoiceContent[] newArray(int size) {
            return new LiMVoiceContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getDisplayContent() {
        return "[语音]";
    }

    @Override
    public String getSearchableWord() {
        return "[语音]";
    }
}
