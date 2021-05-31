package com.xinbida.limaoim.manager;

import android.os.Handler;
import android.os.Looper;

/**
 * 2020-09-21 13:48
 * 管理者
 */
public class LiMBaseManager {

    //判断是否在主线程
    private boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private Handler mainHandler;

    // 回掉给UI是在主线程
    synchronized void runOnMainThread(ICheckThreadBack iCheckThreadBack) {
        if (!isMainThread()) {
            if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(iCheckThreadBack::onMainThread);
        } else iCheckThreadBack.onMainThread();
    }

    protected interface ICheckThreadBack {
        void onMainThread();
    }
}
