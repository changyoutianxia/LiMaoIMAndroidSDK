package com.xinbida.limaoim.message;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.message.type.LiMConnectStatus;
import com.xinbida.limaoim.protocol.LiMPingMsg;
import com.xinbida.limaoim.utils.LiMLoggerUtils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 5/21/21 11:19 AM
 */
class LiMConnectionTimerHandler {
    private LiMConnectionTimerHandler() {
    }

    private static class LiMConnectionTimerHandlerBinder {
        static final LiMConnectionTimerHandler timeHandle = new LiMConnectionTimerHandler();
    }

    public static LiMConnectionTimerHandler getInstance() {
        return LiMConnectionTimerHandlerBinder.timeHandle;
    }


    // 发送心跳定时器
    private Timer heartBeatTimer;
    // 检查心跳定时器
    private Timer checkHeartTimer;
    // 检查网络状态定时器
    private Timer checkNetWorkTimer;

    //关闭所有定时器
    void stopAll() {
        stopHeartBeatTimer();
        stopCheckHeartTimer();
        stopCheckNetWorkTimer();
    }

    //开启所有定时器
    void startAll() {
        startHeartBeatTimer();
        startCheckHeartTimer();
        startCheckNetWorkTimer();
    }

    //检测网络
    private void stopCheckNetWorkTimer() {
        if (checkNetWorkTimer != null) {
            checkNetWorkTimer.cancel();
            checkNetWorkTimer.purge();
            checkNetWorkTimer = null;
        }
    }

    //检测心跳
    private void stopCheckHeartTimer() {
        if (checkHeartTimer != null) {
            checkHeartTimer.cancel();
            checkHeartTimer.purge();
            checkHeartTimer = null;
        }
    }

    //停止心跳Timer
    private void stopHeartBeatTimer() {
        if (heartBeatTimer != null) {
            heartBeatTimer.cancel();
            heartBeatTimer.purge();
            heartBeatTimer = null;
        }
    }

    //开始心跳
    private void startHeartBeatTimer() {
        stopHeartBeatTimer();
        heartBeatTimer = new Timer();
        // 心跳时间
        int heart_time = 60 * 2;
        heartBeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //发送心跳
                LiMConnectionHandler.getInstance().sendMessage(new LiMPingMsg());
            }
        }, 0, heart_time * 1000);
    }

    //开始检查心跳Timer
    private void startCheckHeartTimer() {
        stopCheckHeartTimer();
        checkHeartTimer = new Timer();
        checkHeartTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (LiMConnectionHandler.getInstance().connection == null || heartBeatTimer == null) {
                    LiMConnectionHandler.getInstance().reconnection();
                }
                LiMConnectionHandler.getInstance().checkHeartIsTimeOut();
            }
        }, 1000 * 7, 1000 * 7);
    }


    //开启检测网络定时器
    void startCheckNetWorkTimer() {
        stopCheckNetWorkTimer();
        checkNetWorkTimer = new Timer();
        checkNetWorkTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean is_have_network = LiMaoIMApplication.getInstance().isNetworkConnected();
                if (!is_have_network) {
                    LiMaoIM.getInstance().getLiMConnectionManager().setConnectionStatus(LiMConnectStatus.noNetwork);
                    LiMLoggerUtils.getInstance().e("无网络连接...");
                } else {
                    //有网络
                    if (LiMConnectionHandler.getInstance().connectionIsNull())
                        LiMConnectionHandler.getInstance().reconnection();
                }
                if (LiMConnectionHandler.getInstance().connection == null || !LiMConnectionHandler.getInstance().connection.isOpen()) {
                    LiMConnectionHandler.getInstance().reconnection();
                }
                LiMConnectionHandler.getInstance().checkSendingMsg();
            }
        }, 0, 1000);
    }
}
