package com.xinbida.limaoim.entity;

import java.util.List;

/**
 * 2020-10-09 14:49
 * 同步会话
 */
public class LiMSyncChat {
    public long cmd_version;
    public List<LiMSyncCmd> cmds;
    public List<LiMSyncConvMsg> conversations;
}
