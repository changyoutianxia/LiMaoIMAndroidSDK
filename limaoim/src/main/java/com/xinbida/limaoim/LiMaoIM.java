package com.xinbida.limaoim;

import android.content.Context;
import android.text.TextUtils;

import com.xinbida.limaoim.manager.LiMCMDManager;
import com.xinbida.limaoim.manager.LiMChannelManager;
import com.xinbida.limaoim.manager.LiMChannelMembersManager;
import com.xinbida.limaoim.manager.LiMConnectionManager;
import com.xinbida.limaoim.manager.LiMConversationManager;
import com.xinbida.limaoim.manager.LiMMsgManager;
import com.xinbida.limaoim.message.LiMMessageHandler;
import com.xinbida.limaoim.utils.LiMCurve25519Utils;

/**
 * 5/20/21 5:25 PM
 */
public class LiMaoIM {
    private LiMaoIM() {

    }

    private static class LiMaoIMBinder {
        static final LiMaoIM im = new LiMaoIM();
    }

    public static LiMaoIM getInstance() {
        return LiMaoIMBinder.im;
    }

    private boolean isProcess;
    private boolean isDebug;

    public boolean isDebug() {
        return isDebug;
    }

    public void initIM(Context context, String uid, String token) {
        this.initIM(context, uid, token, false);
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    //设置消息文件目录
    public void setFileCacheDir(String fileDir) {
        LiMaoIMApplication.getInstance().setFileCacheDir(fileDir);
    }

    public boolean isProcess() {
        return isProcess;
    }

    public void initIM(Context context, String uid, String token, boolean isProcess) {
        if (context == null || TextUtils.isEmpty(uid) || TextUtils.isEmpty(token)) {
            throw new NullPointerException("参数不能为空");
        }
        this.isProcess = isProcess;
        LiMaoIMApplication.getInstance().initContext(context);
        LiMaoIMApplication.getInstance().setUid(uid);
        LiMaoIMApplication.getInstance().setToken(token);
        // 初始化加密key
        LiMCurve25519Utils.getInstance().initKey();
        // 初始化默认消息类型
        getLiMMsgManager().initNormalMsg();
        // 初始化数据库
        LiMaoIMApplication.getInstance().getDbHelper();
        // 将上次发送消息中的队列标志为失败
        LiMMessageHandler.getInstance().updateLastSendingMsgFail();
    }

    // 获取消息管理
    public LiMMsgManager getLiMMsgManager() {
        return LiMMsgManager.getInstance();
    }

    // 获取连接管理
    public LiMConnectionManager getLiMConnectionManager() {
        return LiMConnectionManager.getInstance();
    }

    // 获取频道管理
    public LiMChannelManager getLiMChannelManager() {
        return LiMChannelManager.getInstance();
    }

    // 获取最近会话管理
    public LiMConversationManager getLiMConversationManager() {
        return LiMConversationManager.getInstance();
    }

    // 获取频道成员管理
    public LiMChannelMembersManager getLiMChannelMembersManager() {
        return LiMChannelMembersManager.getInstance();
    }

    // 获取cmd管理
    public LiMCMDManager getLiMCMDManager() {
        return LiMCMDManager.getInstance();
    }
}
