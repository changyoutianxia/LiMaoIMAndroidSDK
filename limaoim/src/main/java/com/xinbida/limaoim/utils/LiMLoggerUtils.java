package com.xinbida.limaoim.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * 2019-11-10 17:22
 * 日志管理
 */
public class LiMLoggerUtils {
    /**
     * log TAG
     */
    private final String TAG = "LiMaoLogger";
    private final String ROOT = Objects.requireNonNull(LiMaoIMApplication.getInstance().getContext().getExternalFilesDir(null)).getAbsolutePath() + "/";
    //Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    private final String FILE_NAME = "liMaoLogger.log";
    //
    private final String logFile = ROOT + FILE_NAME;

    private LiMLoggerUtils() {

    }

    private static class LiMLoggerUtilsBinder {
        private final static LiMLoggerUtils liMLoggerUtils = new LiMLoggerUtils();
    }

    public static LiMLoggerUtils getInstance() {
        return LiMLoggerUtilsBinder.liMLoggerUtils;
    }

    /**
     * 获取函数名称
     */
    private String getFunctionName() {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();

        for (StackTraceElement st : sts) {
            if (st.isNativeMethod()) {
                continue;
            }

            if (st.getClassName().equals(Thread.class.getName())) {
                continue;
            }

            if (st.getClassName().equals(this.getClass().getName())) {
                continue;
            }

            return "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId()
                    + "): " + st.getFileName() + ":" + st.getLineNumber() + "]";
        }

        return null;
    }

    private String createMessage(String msg) {
        String functionName = getFunctionName();
        return (functionName == null ? msg : (functionName + " - " + msg));
    }

    /**
     * log.i
     */
    private void info(String msg) {
        String message = createMessage(msg);
        if (LiMaoIM.getInstance().isDebug()) {
            Log.i(TAG, message);
        }
        if (LiMaoIM.getInstance().isDebug()) {
            writeLog(message);
        }
    }

    public void i(String msg) {
        info(msg);
    }

    public void i(Exception e) {
        info(e != null ? e.toString() : "null");
    }

    /**
     * log.v
     */
    private void verbose(String msg) {
        String message = createMessage(msg);
        if (LiMaoIM.getInstance().isDebug()) {
            Log.v(TAG, message);
        }
        if (LiMaoIM.getInstance().isDebug()) {
            writeLog(message);
        }
    }

    public void v(String msg) {
        if (LiMaoIM.getInstance().isDebug()) {
            verbose(msg);
        }
        if (LiMaoIM.getInstance().isDebug()) {
            writeLog(msg);
        }
    }

    public void v(Exception e) {
        if (LiMaoIM.getInstance().isDebug()) {
            verbose(e != null ? e.toString() : "null");
        }
        if (LiMaoIM.getInstance().isDebug()) {
            writeLog(e.toString());
        }
    }

    /**
     * log.d
     */
    private void debug(String msg) {
        if (LiMaoIM.getInstance().isDebug()) {
            String message = createMessage(msg);
            Log.d(TAG, message);
        }
        if (LiMaoIM.getInstance().isDebug()) {
            writeLog(msg);
        }
    }

    /**
     * log.e
     */
    public void error(String msg) {
        String message = createMessage(msg);
        if (LiMaoIM.getInstance().isDebug()) {
            Log.e(TAG, message);
        }
        if (LiMaoIM.getInstance().isDebug()) {
            writeLog(message);
        }
    }

    /**
     * log.error
     */
    public void error(Exception e) {
        StringBuilder sb = new StringBuilder();
        String name = getFunctionName();
        StackTraceElement[] sts = e.getStackTrace();

        if (name != null) {
            sb.append(name).append(" - ").append(e).append("\r\n");
        } else {
            sb.append(e).append("\r\n");
        }
        if (sts.length > 0) {
            for (StackTraceElement st : sts) {
                if (st != null) {
                    sb.append("[ ").append(st.getFileName()).append(":").append(st.getLineNumber()).append(" ]\r\n");
                }
            }
        }
        if (LiMaoIM.getInstance().isDebug()) {
            Log.e(TAG, sb.toString());
        }
        if (LiMaoIM.getInstance().isDebug()) {
            writeLog(sb.toString());
        }
    }

    /**
     * log.warn
     */
    private void warn(String msg) {
        String message = createMessage(msg);
        if (LiMaoIM.getInstance().isDebug()) {
            System.out.println(message);
        } else {
            Log.w(TAG, message);
        }
        if (LiMaoIM.getInstance().isDebug()) {
            writeLog(message);
        }
    }

    public void d(String msg) {
        debug(msg);

    }

    public void d(Exception e) {
        debug(e != null ? e.toString() : "null");
    }


    public void e(String msg) {
        error("LimaoLog_" + msg);
    }

    public void e(Exception e) {
        error(e);
    }

    /**
     * log.w
     */
    public void w(String msg) {
        warn(msg);
    }

    public void w(Exception e) {
        warn(e != null ? e.toString() : "null");
    }
//
//    public  void resetLogFile() {
//        File file = new File(logFile);
//        file.delete();
//        try {
//            file.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @SuppressLint("SimpleDateFormat")
    private void writeLog(String content) {
        try {
            File file = new File(logFile);
            if (!file.exists()) {
                file.createNewFile();
            }
//			DateFormat formate = SimpleDateFormat.getDateTimeInstance();
            SimpleDateFormat formate = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            FileWriter write = new FileWriter(file, true);
            write.write(formate.format(new Date()) + "   " +
                    content + "\n");
            write.flush();
            write.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
