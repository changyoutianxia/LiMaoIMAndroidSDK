package com.xinbida.limaoim.manager;

import android.os.RemoteException;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.interfaces.IConnectionStatus;
import com.xinbida.limaoim.interfaces.IGetIpAndPort;
import com.xinbida.limaoim.interfaces.IGetSocketIpAndPortListener;
import com.xinbida.limaoim.message.ConnectStatus;
import com.xinbida.limaoim.message.LiMConnectionHandler;
import com.xinbida.limaoim.message.LiMMessageHandler;
import com.xinbida.limaoim.protocol.LiMMessageContent;
import com.xinbida.limaoim.service.PushServiceConn;

import java.util.ArrayList;
import java.util.List;

/**
 * 5/21/21 10:31 AM
 * connect manager
 */
public class LiMConnectionManager extends LiMBaseManager {
    private LiMConnectionManager() {

    }

    private static class LiMConnectionManagerBinder {
        static final LiMConnectionManager connectManager = new LiMConnectionManager();
    }

    public static LiMConnectionManager getInstance() {
        return LiMConnectionManagerBinder.connectManager;
    }


    private IGetIpAndPort iGetIpAndPort;
    private List<IConnectionStatus> iConnectionStatusList;

    // 连接
    public void connection() {
        LiMaoIMApplication.getInstance().connectStatus = ConnectStatus.connect;
        if (LiMConnectionHandler.getInstance().connectionIsNull()) {
            if (LiMaoIM.getInstance().isProcess())
                PushServiceConn.getInstance(LiMaoIMApplication.getInstance().getContext()).startConn();
            else LiMConnectionHandler.getInstance().reconnection();
        }
    }


    public void disconnect(boolean isLogout) {
        if (isLogout) {
            logoutChat();
        } else {
            stopConnect();
        }
    }

    /**
     * 断开连接
     */
    private void stopConnect() {
        LiMaoIMApplication.getInstance().connectStatus = ConnectStatus.disConnect;
        if (LiMaoIM.getInstance().isProcess())
            PushServiceConn.getInstance(LiMaoIMApplication.getInstance().getContext()).stopChatService();
        else {
            LiMConnectionHandler.getInstance().stopAll();
        }
    }

    /**
     * 退出登录
     */
    private void logoutChat() {
        LiMaoIMApplication.getInstance().connectStatus = ConnectStatus.logOut;
        LiMMessageHandler.getInstance().saveReceiveMsg();
        if (LiMaoIM.getInstance().isProcess())
            PushServiceConn.getInstance(LiMaoIMApplication.getInstance().getContext()).logoutChatService();
        else {
            LiMaoIMApplication.getInstance().setToken("");
            LiMMessageHandler.getInstance().updateLastSendingMsgFail();
            LiMConnectionHandler.getInstance().stopAll();
            LiMaoIMApplication.getInstance().closeDbHelper();
        }
    }

    public void sendMessage(LiMMessageContent liMBaseContentMsgModel, String channelID, byte channelType) {
        if (LiMaoIM.getInstance().isProcess()) {
            if (PushServiceConn.getInstance(LiMaoIMApplication.getInstance().getContext()).pushService == null) {
                PushServiceConn.getInstance(LiMaoIMApplication.getInstance().getContext()).connPushService();
                return;
            }
            try {
                PushServiceConn.getInstance(LiMaoIMApplication.getInstance().getContext()).pushService
                        .sendMessage(liMBaseContentMsgModel, channelID, channelType);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            LiMConnectionHandler.getInstance().sendMessage(liMBaseContentMsgModel, channelID, channelType);
        }

    }

    public void sendMessage(LiMMsg liMMsg) {
        if (LiMaoIM.getInstance().isProcess()) {
            if (PushServiceConn.getInstance(LiMaoIMApplication.getInstance().getContext()).pushService == null) {
                PushServiceConn.getInstance(LiMaoIMApplication.getInstance().getContext()).connPushService();
                return;
            }
            try {
                PushServiceConn.getInstance(LiMaoIMApplication.getInstance().getContext()).pushService
                        .sendMessageWithLiMMsg(liMMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            LiMConnectionHandler.getInstance().sendMessage(liMMsg);
        }
    }

    public void getIpAndPort(IGetSocketIpAndPortListener iGetIpAndPortListener) {
        if (iGetIpAndPort != null) {
            runOnMainThread(() -> iGetIpAndPort.getIP(iGetIpAndPortListener));
        }
    }

    // 监听获取IP和port
    public void addOnGetIpAndPortListener(IGetIpAndPort iGetIpAndPort) {
        this.iGetIpAndPort = iGetIpAndPort;
    }

    public void setConnectionStatus(int status) {
        if (iConnectionStatusList != null && iConnectionStatusList.size() > 0) {
            runOnMainThread(() -> {
                for (int i = 0, size = iConnectionStatusList.size(); i < size; i++) {
                    iConnectionStatusList.get(i).onStatus(status);
                }
            });
        }
    }

    // 监听连接状态
    public void addOnConnectionStatusListener(IConnectionStatus iConnectionStatus) {
        if (iConnectionStatus == null) return;
        if (iConnectionStatusList == null) iConnectionStatusList = new ArrayList<>();
        iConnectionStatusList.add(iConnectionStatus);
    }


}
