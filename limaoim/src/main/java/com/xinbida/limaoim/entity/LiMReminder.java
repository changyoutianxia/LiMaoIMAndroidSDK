package com.xinbida.limaoim.entity;

/**
 * 2020-01-24 13:34
 * 提醒信息
 */
public class LiMReminder {
    public int type;
    public String text;
    public Object data;

    public LiMReminder(int type, String text, Object data) {
        this.text = text;
        this.type = type;
        this.data = data;
    }

    public LiMReminder(int type, String text) {
        this.text = text;
        this.type = type;
    }

    public LiMReminder(int type) {
        this.type = type;
    }

    public LiMReminder() {
    }
}
