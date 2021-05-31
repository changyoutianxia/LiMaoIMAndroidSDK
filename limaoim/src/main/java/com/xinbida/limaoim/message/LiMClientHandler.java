package com.xinbida.limaoim.message;

import android.util.Log;

import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.utils.LiMLoggerUtils;

import org.xsocket.connection.IConnectExceptionHandler;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IConnectionTimeoutHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.IIdleTimeoutHandler;
import org.xsocket.connection.INonBlockingConnection;

import java.io.IOException;
import java.nio.BufferUnderflowException;

/**
 * 2020-12-18 10:28
 * 连接客户端
 */
class LiMClientHandler implements IDataHandler, IConnectHandler,
        IDisconnectHandler, IConnectExceptionHandler,
        IConnectionTimeoutHandler, IIdleTimeoutHandler {

    private boolean isConnectSuccess;

    LiMClientHandler() {
        isConnectSuccess = false;
    }

    @Override
    public boolean onConnectException(INonBlockingConnection iNonBlockingConnection, IOException e) {
        LiMLoggerUtils.getInstance().e("连接异常");
        LiMConnectionHandler.getInstance().reconnection();
        close(iNonBlockingConnection);
        return true;
    }

    @Override
    public boolean onConnect(INonBlockingConnection iNonBlockingConnection) throws BufferUnderflowException {
        if (LiMConnectionHandler.getInstance().connection == null) {
            Log.e("连接信息为空", "--->");
        }
        if (LiMConnectionHandler.getInstance().connection != null && iNonBlockingConnection != null) {
            if (!LiMConnectionHandler.getInstance().connection.getId().equals(iNonBlockingConnection.getId())) {
                close(iNonBlockingConnection);
                LiMConnectionHandler.getInstance().reconnection();
            } else {
                //连接成功
                isConnectSuccess = true;
                LiMLoggerUtils.getInstance().e("连接成功");
                LiMConnectionHandler.getInstance().sendConnectMsg();
            }
        } else {
            close(iNonBlockingConnection);
            LiMLoggerUtils.getInstance().e("连接成功连接对象为空");
            LiMConnectionHandler.getInstance().reconnection();
        }
        return false;
    }

    @Override
    public boolean onConnectionTimeout(INonBlockingConnection iNonBlockingConnection) {
        if (!isConnectSuccess) {
            LiMLoggerUtils.getInstance().e("连接超时");
            LiMConnectionHandler.getInstance().reconnection();
        }
        return true;
    }

    @Override
    public boolean onData(INonBlockingConnection iNonBlockingConnection) throws BufferUnderflowException {
        if (LiMConnectionHandler.getInstance().connection != null
                && !iNonBlockingConnection.getId().equals(LiMConnectionHandler.getInstance().connection.getId())) {
            LiMLoggerUtils.getInstance().e("收到的消息ID和连接的ID对应不上---");
            try {
                iNonBlockingConnection.close();
                LiMConnectionHandler.getInstance().connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            LiMConnectionHandler.getInstance().reconnection();
            return true;
        }
        int available_len;
//        byte[] available_bytes = null;
        int bufLen = 102400;
        try {

            available_len = iNonBlockingConnection.available();
            if (available_len == -1) {
                return true;
            }
            int readCount = available_len / bufLen;
            if (available_len % bufLen != 0) {
                readCount++;
            }

            for (int i = 0; i < readCount; i++) {
                int readLen = bufLen;
                if (i == readCount - 1) {
                    if (available_len % bufLen != 0) {
                        readLen = available_len % bufLen;
                    }
                }
                byte[] buffBytes = iNonBlockingConnection.readBytesByLength(readLen);
                LiMConnectionHandler.getInstance().receivedData(buffBytes.length, buffBytes);
            }

        } catch (IOException e) {
            e.printStackTrace();
            LiMLoggerUtils.getInstance().e("处理接受到到数据异常:" + e.getMessage());
        }
        return true;
    }

    @Override
    public boolean onDisconnect(INonBlockingConnection iNonBlockingConnection) {
        LiMLoggerUtils.getInstance().e("连接断开");
        if (LiMaoIMApplication.getInstance().connectStatus != ConnectStatus.disConnect)
            LiMConnectionHandler.getInstance().reconnection();
        close(iNonBlockingConnection);
        return true;
    }

    @Override
    public boolean onIdleTimeout(INonBlockingConnection iNonBlockingConnection) {
        if (!isConnectSuccess) {
            LiMLoggerUtils.getInstance().e("Idle连接超时");
            LiMConnectionHandler.getInstance().reconnection();
            close(iNonBlockingConnection);
        }
        return true;
    }

    private void close(INonBlockingConnection iNonBlockingConnection) {
        try {
            if (iNonBlockingConnection != null)
                iNonBlockingConnection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
