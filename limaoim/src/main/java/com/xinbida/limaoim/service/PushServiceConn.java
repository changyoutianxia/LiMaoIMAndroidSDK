package com.xinbida.limaoim.service;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import com.xinbida.limaoim.IPushService;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.message.ConnectStatus;


/**
 * 2019-11-18 13:57
 * 连接管理
 */
public class PushServiceConn {
    private final Context mContext;
    /**
     * service名称
     */
    public static final String PUSH_SERVICE_NAME = "com.xinbida.limaoim.service.PushService";
    private final Object sync = new Object();
    public IPushService pushService;
    public ServiceConnection mPushServiceConnection;

    private volatile static PushServiceConn instance;

    /**
     * 获取连接对象实例
     *
     * @param context
     * @return
     */
    public static PushServiceConn getInstance(Context context) {
        if (context != null) {
            if (instance == null) {
                synchronized (PushServiceConn.class) {
                    if (instance == null) {
                        instance = new PushServiceConn(context);
                    }
                }
            }
        }
        return instance;
    }

    public PushServiceConn(Context context) {
        mContext = context;
    }

    /**
     * 开始连接
     */
    public void startChatService() {
        synchronized (sync) {
            if (!isServiceRunning(PUSH_SERVICE_NAME) || pushService == null) {
                connPushService();
            }
        }
    }

    /**
     * 判断程序是否运行
     *
     * @param className
     * @return
     */
    public boolean isServiceRunning(String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : activityManager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (PUSH_SERVICE_NAME.equals(service.service.getClassName())) {
                isRunning = true;
            }
        }
        return isRunning;
    }

    /**
     * 开始连接
     */
    public void startConn() {
        if (pushService == null) {
            connPushService();
            return;
        }

        try {
            pushService.connect(ConnectStatus.connect);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开连接
     */
    public void stopChatService() {

        if (pushService == null) {
            connPushService();
            return;
        }
        try {
            pushService.connect(ConnectStatus.disConnect);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 退出登录
     */
    public void logoutChatService() {
        if (pushService == null) {
            connPushService();
            return;
        }
        try {
            pushService.connect(ConnectStatus.logOut);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void connPushService() {
        if (mContext == null) return;
        Intent intent = new Intent(mContext, PushService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(intent);
        } else {
            mContext.startService(intent);
        }
        mContext.startService(intent);
        if (mPushServiceConnection == null) {
            pushServiceConnection();
            mContext.bindService(intent, mPushServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    private void pushServiceConnection() {
        mPushServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                pushService = null;
                mContext.unbindService(mPushServiceConnection);
                mPushServiceConnection = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                pushService = IPushService.Stub.asInterface(service);
                try {
                    pushService.regRecvMsgCallback(LiMaoIMApplication.getInstance().getUid(), LiMaoIMApplication.getInstance().getToken());
                    pushService.connect(LiMaoIMApplication.getInstance().connectStatus);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
