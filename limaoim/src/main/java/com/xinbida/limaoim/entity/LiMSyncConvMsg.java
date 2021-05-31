package com.xinbida.limaoim.entity;

import java.util.List;

/**
 * 2020-10-09 14:59
 * 最近会话
 */
public class LiMSyncConvMsg {
    public String channel_id;
    public byte channel_type;
    public String last_client_msg_no;
    public long last_msg_seq;
    public int offset_msg_seq;
    public long timestamp;
    public int unread;
    public long version;
    public List<LiMSyncRecent> recents;
}
