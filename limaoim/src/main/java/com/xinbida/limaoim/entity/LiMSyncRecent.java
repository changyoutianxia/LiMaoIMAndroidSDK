package com.xinbida.limaoim.entity;

import java.util.List;
import java.util.Map;

/**
 * 2020-10-09 15:11
 * 最近消息
 */
public class LiMSyncRecent {
    public String message_id;
    public int message_seq;
    public String client_msg_no;
    public String from_uid;
    public String channel_id;
    public byte channel_type;
    public long timestamp;
    public int voice_status;
    public int is_deleted;
    public int revoke;
    public String revoker;
    public long extra_version;
    public int unread_count;
    public int readed_count;
    public int readed;
    public int receipt;
    public int setting;
    public Map payload;
    public List<LiMSyncMsgReaction> reactions;
}
