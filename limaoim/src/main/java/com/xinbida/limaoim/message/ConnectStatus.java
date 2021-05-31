package com.xinbida.limaoim.message;

/**
 * 2019-12-04 13:24
 * 连接状态
 */
public class ConnectStatus {
    //连接
    public static final byte connect = 0;
    //断开连接
    public static final byte disConnect = 1;
    //退出登录
    public static final byte logOut = 2;
    //等待[目前未用到]
    public static final byte waiting = 3;
}
