package com.xinbida.limaoim.message.type;

/**
 * 2019-11-11 09:47
 * 消息内容类型
 */
public class LiMMsgContentType {
    //文本
    public static final int LIMAO_TEXT = 1;
    //图片
    public static final int LIMAO_IMAGE = 2;
    //GIF
    public static final int LIMAO_GIF = 3;
    //语音
    public static final int LIMAO_VOICE = 4;
    //视频
    public static final int LIMAO_VIDEO = 5;
    //位置
    public static final int LIMAO_LOCATION = 6;
    //名片
    public static final int LIMAO_CARD = 7;
    //文件
    public static final int LIMAO_FILE = 8;
    //红包
    public static final int LIMAO_REDPACKET = 9;
    //转账
    public static final int LIMAO_TRANSFER = 10;
    //合并转发消息
    public static final int LIMAO_MULTIPLE_FORWARD = 11;
    //内部消息，无需存储到数据库
    public static final int LIM_INSIDE_MSG = 99;
}
