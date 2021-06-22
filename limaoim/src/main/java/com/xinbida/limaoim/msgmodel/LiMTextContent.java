package com.xinbida.limaoim.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

import com.xinbida.limaoim.message.type.LiMMsgContentType;
import com.xinbida.limaoim.protocol.LiMMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-04-04 10:35
 * 文本消息
 */
public class LiMTextContent extends LiMMessageContent {
    public LiMTextContent(String content) {
        this.content = content;
        this.type = LiMMsgContentType.LIMAO_TEXT;
    }

    // 无参构造必须提供
    public LiMTextContent() {
        this.type = LiMMsgContentType.LIMAO_TEXT;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("content", content);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LiMMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject.has("content"))
            this.content = jsonObject.optString("content");
        return this;
    }

    @Override
    public String getSearchableWord() {
        return content;
    }

    @Override
    public String getDisplayContent() {
        return content;
    }

    protected LiMTextContent(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }


    public static final Parcelable.Creator<LiMTextContent> CREATOR = new Parcelable.Creator<LiMTextContent>() {
        @Override
        public LiMTextContent createFromParcel(Parcel in) {
            return new LiMTextContent(in);
        }

        @Override
        public LiMTextContent[] newArray(int size) {
            return new LiMTextContent[size];
        }
    };
}
