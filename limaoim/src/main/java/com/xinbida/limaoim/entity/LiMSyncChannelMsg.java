package com.xinbida.limaoim.entity;

import java.util.List;

/**
 * 2020-10-10 15:13
 * 同步频道消息
 */
public class LiMSyncChannelMsg {
    public long min_message_seq;
    public long max_message_seq;
    public int more;
    public List<LiMSyncRecent> messages;
}
