package com.xinbida.limaoim.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import com.xinbida.limaoim.IPushService;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.message.ConnectStatus;
import com.xinbida.limaoim.message.LiMConnectionHandler;
import com.xinbida.limaoim.message.LiMMessageHandler;
import com.xinbida.limaoim.protocol.LiMMessageContent;
import com.xinbida.limaoim.utils.LiMLoggerUtils;

/**
 * 2019-11-18 11:07
 * im service
 */
public class PushService extends Service {
    public static final String CHANNEL_ID_STRING = "service_limao";
    //    private ConnectionHandler mConnectionHandler;
    private final IPushService.Stub iPushService = new IPushService.Stub() {
        @Override
        public void regRecvMsgCallback(String uid, String imToken) {
            if (!TextUtils.isEmpty(LiMaoIMApplication.getInstance().getUid())
                    && !LiMaoIMApplication.getInstance().getUid()
                    .equals(uid)) {
                LiMaoIMApplication.getInstance().closeDbHelper();
            }
            LiMLoggerUtils.getInstance().e("LiMao_service连接 用户ID：" + uid);
            LiMLoggerUtils.getInstance().e("LiMao_service连接 用户Token：" + imToken);
            LiMaoIMApplication.getInstance().setUid(uid);
            LiMaoIMApplication.getInstance().setToken(imToken);
            // 初始化数据库
            LiMaoIMApplication.getInstance().getDbHelper();
            // 将上次发送消息中的队列标志为失败
            LiMMessageHandler.getInstance().updateLastSendingMsgFail();
        }

        @Override
        public void sendMessage(LiMMessageContent liMBaseContentModel, String channelID, byte channelType) {
            LiMConnectionHandler.getInstance().sendMessage(liMBaseContentModel,
                    channelID, channelType);
        }

        @Override
        public void sendMessageWithLiMMsg(LiMMsg liMMsg) {
            LiMConnectionHandler.getInstance().sendMessage(liMMsg);
        }

        @Override
        public void connect(int status) {
            LiMaoIMApplication.getInstance().connectStatus = status;
            if (status == ConnectStatus.connect) {
                LiMConnectionHandler.getInstance().reconnection();
                LiMLoggerUtils.getInstance().e("设置重连----->");
            } else if (status == ConnectStatus.disConnect) {
                LiMConnectionHandler.getInstance().stopAll();
                LiMLoggerUtils.getInstance().e("设置断开:---->");
            } else if (status == ConnectStatus.logOut) {
                LiMLoggerUtils.getInstance().e("设置退出登录:---->");
                LiMaoIMApplication.getInstance().setUid("");
                LiMaoIMApplication.getInstance().setToken("");
                LiMMessageHandler.getInstance().updateLastSendingMsgFail();
                LiMConnectionHandler.getInstance().stopAll();
                LiMaoIMApplication.getInstance().closeDbHelper();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return iPushService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (LiMaoIMApplication.getInstance().getContext() != null) {
            NotificationManager notificationManager = (NotificationManager) LiMaoIMApplication.getInstance().getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel mChannel;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                mChannel = new NotificationChannel(CHANNEL_ID_STRING, "limaoIm",
                        NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(mChannel);
                Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID_STRING).build();
                startForeground(1, notification);
            }
        }
//        mConnectionHandler = new ConnectionHandler();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LiMConnectionHandler.getInstance().stopAll();
//        mConnectionHandler = null;
    }


    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, START_STICKY, startId);
    }
}
