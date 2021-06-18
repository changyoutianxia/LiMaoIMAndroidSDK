package com.xinbida.limaoim.db;

import android.content.ContentValues;

import com.xinbida.limaoim.entity.LiMChannel;
import com.xinbida.limaoim.entity.LiMChannelMember;
import com.xinbida.limaoim.entity.LiMConversationMsg;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.entity.LiMMsgReaction;

import org.json.JSONException;
import org.json.JSONObject;


class LiMSqlContentValues {

    /**
     * 获取会话数据表对应字段
     *
     * @param liMMsg 会话消息
     * @return ContentValues
     */
    static ContentValues getContentValuesWithLiMMsg(LiMMsg liMMsg) {
        ContentValues contentValues = new ContentValues();
        if (liMMsg == null) {
            return contentValues;
        }
        contentValues.put(LiMDBColumns.LiMMessageColumns.message_id, liMMsg.messageID);
        contentValues.put(LiMDBColumns.LiMMessageColumns.message_seq, liMMsg.messageSeq);
//		contentValues.put(DBMsgColumns.client_seq, liMMsg.client_seq);

        contentValues.put(LiMDBColumns.LiMMessageColumns.order_seq, liMMsg.orderSeq);
        contentValues.put(LiMDBColumns.LiMMessageColumns.timestamp, liMMsg.timestamp);
        contentValues.put(LiMDBColumns.LiMMessageColumns.from_uid, liMMsg.fromUID);
        contentValues.put(LiMDBColumns.LiMMessageColumns.channel_id, liMMsg.channelID);
        contentValues.put(LiMDBColumns.LiMMessageColumns.channel_type, liMMsg.channelType);
        contentValues.put(LiMDBColumns.LiMMessageColumns.is_deleted, liMMsg.isDeleted);
        contentValues.put(LiMDBColumns.LiMMessageColumns.type, liMMsg.type);
        contentValues.put(LiMDBColumns.LiMMessageColumns.content, liMMsg.content);
        contentValues.put(LiMDBColumns.LiMMessageColumns.status, liMMsg.status);
        contentValues.put(LiMDBColumns.LiMMessageColumns.created_at, liMMsg.createdAt);
        contentValues.put(LiMDBColumns.LiMMessageColumns.updated_at, liMMsg.updatedAt);
        contentValues.put(LiMDBColumns.LiMMessageColumns.voice_status, liMMsg.voiceStatus);
        contentValues.put(LiMDBColumns.LiMMessageColumns.client_msg_no, liMMsg.clientMsgNO);
        contentValues.put(LiMDBColumns.LiMMessageColumns.revoke, liMMsg.revoke);
        contentValues.put(LiMDBColumns.LiMMessageColumns.revoker, liMMsg.revoker);
        contentValues.put(LiMDBColumns.LiMMessageColumns.unread_count, liMMsg.unreadCount);
        contentValues.put(LiMDBColumns.LiMMessageColumns.readed_count, liMMsg.readedCount);
        contentValues.put(LiMDBColumns.LiMMessageColumns.readed, liMMsg.readed);
        contentValues.put(LiMDBColumns.LiMMessageColumns.receipt, liMMsg.receipt);
        contentValues.put(LiMDBColumns.LiMMessageColumns.extra_version, liMMsg.extraVersion);
        if (liMMsg.baseContentMsgModel != null) {
            contentValues.put(LiMDBColumns.LiMMessageColumns.searchable_word, liMMsg.baseContentMsgModel.getSearchableWord());
        }
        if (liMMsg.extraMap != null) {
            JSONObject jsonObject = new JSONObject();
            for (Object key : liMMsg.extraMap.keySet()) {
                try {
                    jsonObject.put(key.toString(), liMMsg.extraMap.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            contentValues.put(LiMDBColumns.LiMMessageColumns.extra, jsonObject.toString());
        }

        return contentValues;
    }

    /**
     * 会话记录表对应存储字段
     *
     * @param liMConversationMsg 会话消息
     * @param isUpdateStatus     是否编辑消息状态
     * @return ContentValues
     */
    static ContentValues getContentValuesWithLiMCoverMsg(LiMConversationMsg liMConversationMsg, boolean isUpdateStatus) {
        ContentValues contentValues = new ContentValues();
        if (liMConversationMsg == null) {
            return contentValues;
        }
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.channel_id, liMConversationMsg.channelID);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.channel_type, liMConversationMsg.channelType);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.last_msg_id, liMConversationMsg.lastMsgID);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.last_client_msg_no, liMConversationMsg.lastClientMsgNO);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.client_seq, liMConversationMsg.clientSeq);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.last_msg_timestamp, liMConversationMsg.lastMsgTimestamp);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.type, liMConversationMsg.type);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.version, liMConversationMsg.version);
        if (isUpdateStatus) {
            contentValues.put(LiMDBColumns.LiMCoverMessageColumns.last_msg_content, liMConversationMsg.lastMsgContent);
            contentValues.put(LiMDBColumns.LiMCoverMessageColumns.status, liMConversationMsg.status);
        }
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.reminders, liMConversationMsg.reminders);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.unread_count, liMConversationMsg.unreadCount);
        contentValues.put(LiMDBColumns.LiMCoverMessageColumns.from_uid, liMConversationMsg.fromUID);
        if (liMConversationMsg.extraMap != null) {
            JSONObject jsonObject = new JSONObject();
            for (Object key : liMConversationMsg.extraMap.keySet()) {
                try {
                    jsonObject.put(key.toString(), liMConversationMsg.extraMap.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            contentValues.put(LiMDBColumns.LiMCoverMessageColumns.extra, jsonObject.toString());
        }
        return contentValues;
    }

    /**
     * 频道表对应存储修改字段
     *
     * @param liMChannel 频道信息
     * @return ContentValues
     */
    static ContentValues getContentValuesWithLiMChannel(LiMChannel liMChannel) {
        ContentValues contentValues = new ContentValues();
        if (liMChannel == null) {
            return contentValues;
        }
        contentValues.put(LiMDBColumns.LiMChannelColumns.channel_id, liMChannel.channelID);
        contentValues.put(LiMDBColumns.LiMChannelColumns.channel_type, liMChannel.channelType);
        contentValues.put(LiMDBColumns.LiMChannelColumns.channel_name, liMChannel.channelName);
        contentValues.put(LiMDBColumns.LiMChannelColumns.channel_remark, liMChannel.channelRemark);
        contentValues.put(LiMDBColumns.LiMChannelColumns.avatar, liMChannel.avatar);
        contentValues.put(LiMDBColumns.LiMChannelColumns.top, liMChannel.top);
        contentValues.put(LiMDBColumns.LiMChannelColumns.save, liMChannel.save);
        contentValues.put(LiMDBColumns.LiMChannelColumns.mute, liMChannel.mute);
        contentValues.put(LiMDBColumns.LiMChannelColumns.forbidden, liMChannel.forbidden);
        contentValues.put(LiMDBColumns.LiMChannelColumns.invite, liMChannel.invite);
        contentValues.put(LiMDBColumns.LiMChannelColumns.status, liMChannel.status);
        contentValues.put(LiMDBColumns.LiMChannelColumns.is_deleted, liMChannel.isDeleted);
        contentValues.put(LiMDBColumns.LiMChannelColumns.follow, liMChannel.follow);
        contentValues.put(LiMDBColumns.LiMChannelColumns.version, liMChannel.version);
        contentValues.put(LiMDBColumns.LiMChannelColumns.show_nick, liMChannel.showNick);
        contentValues.put(LiMDBColumns.LiMChannelColumns.created_at, liMChannel.createdAt);
        contentValues.put(LiMDBColumns.LiMChannelColumns.updated_at, liMChannel.updatedAt);
        contentValues.put(LiMDBColumns.LiMChannelColumns.online, liMChannel.online);
        contentValues.put(LiMDBColumns.LiMChannelColumns.last_offline, liMChannel.lastOffline);
        contentValues.put(LiMDBColumns.LiMChannelColumns.receipt, liMChannel.receipt);
        contentValues.put(LiMDBColumns.LiMChannelColumns.category, liMChannel.category);

        if (liMChannel.extraMap != null) {
            JSONObject jsonObject = new JSONObject();
            for (Object key : liMChannel.extraMap.keySet()) {
                try {
                    jsonObject.put(String.valueOf(key), liMChannel.extraMap.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            contentValues.put(LiMDBColumns.LiMChannelColumns.extra, jsonObject.toString());
        }
        return contentValues;
    }

    /**
     * 频道成员
     *
     * @param liMChannelMember 频道成员
     * @return ContentValues
     */
    static ContentValues getContentValuesWithLiMChannelMember(LiMChannelMember liMChannelMember) {
        ContentValues contentValues = new ContentValues();
        if (liMChannelMember == null) {
            return contentValues;
        }
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.channel_id, liMChannelMember.channelID);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.channel_type, liMChannelMember.channelType);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.member_uid, liMChannelMember.memberUID);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.member_name, liMChannelMember.memberName);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.member_remark, liMChannelMember.memberRemark);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.member_avatar, liMChannelMember.memberAvatar);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.role, liMChannelMember.role);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.is_deleted, liMChannelMember.isDeleted);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.version, liMChannelMember.version);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.status, liMChannelMember.status);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.created_at, liMChannelMember.createdAt);
        contentValues.put(LiMDBColumns.LiMChannelMembersColumns.updated_at, liMChannelMember.updatedAt);

        if (liMChannelMember.extraMap != null) {
            JSONObject jsonObject = new JSONObject();
            for (Object key : liMChannelMember.extraMap.keySet()) {
                try {
                    jsonObject.put(String.valueOf(key), liMChannelMember.extraMap.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            contentValues.put(LiMDBColumns.LiMChannelMembersColumns.extra, jsonObject.toString());
        }

        return contentValues;
    }

    /**
     * 消息回应
     *
     * @param reaction LiMMessageReaction
     * @return ContentValues
     */
    static ContentValues getContentValuesWithMsgReaction(LiMMsgReaction reaction) {
        ContentValues contentValues = new ContentValues();
        if (reaction == null) {
            return contentValues;
        }
        contentValues.put(LiMDBColumns.LiMMessageReaction.channel_id, reaction.channelID);
        contentValues.put(LiMDBColumns.LiMMessageReaction.channel_type, reaction.channelType);
        contentValues.put(LiMDBColumns.LiMMessageReaction.message_id, reaction.messageID);
        contentValues.put(LiMDBColumns.LiMMessageReaction.uid, reaction.uid);
        contentValues.put(LiMDBColumns.LiMMessageReaction.name, reaction.name);
        contentValues.put(LiMDBColumns.LiMMessageReaction.is_deleted, reaction.isDeleted);
        contentValues.put(LiMDBColumns.LiMMessageReaction.seq, reaction.seq);
        contentValues.put(LiMDBColumns.LiMMessageReaction.emoji, reaction.emoji);
        contentValues.put(LiMDBColumns.LiMMessageReaction.created_at, reaction.createdAt);
        return contentValues;
    }
}
