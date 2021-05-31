package com.xinbida.limaoim.protocol;

/**
 * 2019-11-11 10:14
 * talk service 基础消息对象
 *
 * @see com.xinbida.limaoim.message.type.LiMMsgType 对应packetType类型
 */
public class LiMBaseMsg {
    //报文类型
    public short packetType;
    //标示位（目前为固定值）
    public short flag;
    //报文剩余长度
    public int remainingLength;

    //是否持久化[是否保存在数据库]
    public boolean no_persist;
    //是否显示红点
    public boolean red_dot = true;
    //是否只显示一次
    public boolean sync_once;
}
