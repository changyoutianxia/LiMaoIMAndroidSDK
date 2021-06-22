package com.xinbida.limaoim;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;

import com.xinbida.limaoim.db.LiMDBHelper;
import com.xinbida.limaoim.entity.LiMSyncMsgMode;
import com.xinbida.limaoim.utils.LiMLoggerUtils;

import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * 5/20/21 5:27 PM
 */
public class LiMaoIMApplication {
    private LiMaoIMApplication() {
    }

    private static class LiMaoIMApplicationBinder {
        static final LiMaoIMApplication app = new LiMaoIMApplication();
    }

    public static LiMaoIMApplication getInstance() {
        return LiMaoIMApplicationBinder.app;
    }

    private WeakReference<Context> mContext;

    public Context getContext() {
        return mContext.get();
    }

    void initContext(Context context) {
        this.mContext = new WeakReference<>(context);
    }

    //协议版本号
    public byte protocolVersion = 4;
    private String tempUid;
    private LiMDBHelper mDbHelper;
    public int connectStatus;
    private String fileDir = "liMaoIM";
    private LiMSyncMsgMode syncMsgMode;

    public LiMSyncMsgMode getSyncMsgMode() {
        if (syncMsgMode == null) syncMsgMode = LiMSyncMsgMode.READ;
        return syncMsgMode;
    }

    // 同步消息模式
    public void setSyncMsgMode(LiMSyncMsgMode mode) {
        this.syncMsgMode = mode;
    }


    public String getUid() {
        if (mContext == null) {
            LiMLoggerUtils.getInstance().e("传入的context为空");
            return "";
        }
        if (TextUtils.isEmpty(tempUid)) {
            SharedPreferences setting = mContext.get().getSharedPreferences(
                    "account_config", Context.MODE_PRIVATE);
            tempUid = setting.getString("liMaoIM_UID", "");
        }
        return tempUid;
    }

    public void setUid(String uid) {
        if (mContext == null) return;
        tempUid = uid;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                "account_config", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        editor.putString("liMaoIM_UID", uid);
        editor.apply();
    }

    public String getToken() {
        if (mContext == null) return "";
        SharedPreferences setting = mContext.get().getSharedPreferences(
                "account_config", Context.MODE_PRIVATE);
        return setting.getString("liMaoIM_Token", "");
    }

    public void setToken(String token) {
        if (mContext == null) return;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                "account_config", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        editor.putString("liMaoIM_Token", token);
        editor.apply();
    }


    public synchronized LiMDBHelper getDbHelper() {
        if (mDbHelper == null) {
            String custId = getUid();
            if (!TextUtils.isEmpty(custId)) {
                mDbHelper = LiMDBHelper.getInstance(mContext.get(), custId);
            } else {
                LiMLoggerUtils.getInstance().e("获取DbHelper时用户ID为null");
            }
        }
        return mDbHelper;
    }

    public void closeDbHelper() {
        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
        }
    }

    public long getDBUpgradeIndex() {
        if (mContext == null) return 0;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                "account_config", Context.MODE_PRIVATE);
        return setting.getLong(getUid() + "_db_upgrade_index", 0);
    }

    public void setDBUpgradeIndex(long index) {
        if (mContext == null) return;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                "account_config", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        editor.putLong(getUid() + "_db_upgrade_index", index);
        editor.apply();
    }

    private void setDeviceId(String deviceId) {
        if (mContext == null) return;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                "account_config", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        if (TextUtils.isEmpty(deviceId))
            deviceId = UUID.randomUUID().toString().replaceAll("-", "");
        editor.putString(getUid() + "_lim_device_id", deviceId);
        editor.apply();
    }

    public String getDeviceId() {
        if (mContext == null) return "";
        SharedPreferences setting = mContext.get().getSharedPreferences(
                "account_config", Context.MODE_PRIVATE);
        String deviceId = setting.getString(getUid() + "_lim_device_id", "");
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = UUID.randomUUID().toString().replaceAll("-", "");
            setDeviceId(deviceId);
        }
        return deviceId;
    }

    public boolean isNetworkConnected() {
        if (mContext == null) {
            LiMLoggerUtils.getInstance().e("检测网络的context为空--->");
            return false;
        }
        boolean success = false;
        ConnectivityManager connectivity = (ConnectivityManager) mContext.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (connectivity != null) {
                Network networks = connectivity.getActiveNetwork();
                NetworkCapabilities networkCapabilities = connectivity.getNetworkCapabilities(networks);
                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        success = true;
                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        success = true;
                    }
                } else {
                    success = false;
                }
            }

        } else {
            NetworkInfo.State state = connectivity.getNetworkInfo(
                    ConnectivityManager.TYPE_WIFI).getState(); // 获取网络连接状态
            if (NetworkInfo.State.CONNECTED == state) {
                // 判断是否正在使用WIFI网络
                success = true;
            } else {
                state = connectivity.getNetworkInfo(
                        ConnectivityManager.TYPE_MOBILE).getState(); // 获取网络连接状态
                if (NetworkInfo.State.CONNECTED == state) { // 判断是否正在使用GPRS网络
                    success = true;
                }
            }
        }

        return success;
    }

    public void setFileCacheDir(String fileDir) {
        this.fileDir = fileDir;
    }

    public String getFileCacheDir() {
        if (TextUtils.isEmpty(fileDir))
            fileDir = "liMaoIM";

        if (!TextUtils.isEmpty(getUid())) {
            fileDir = String.format("%s/%s", fileDir, getUid());
        }
        return fileDir;
    }
}
