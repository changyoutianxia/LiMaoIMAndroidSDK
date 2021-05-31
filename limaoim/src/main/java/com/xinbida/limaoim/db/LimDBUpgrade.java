package com.xinbida.limaoim.db;

import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.xinbida.limaoim.LiMaoIMApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 2020-07-31 09:36
 * 数据库升级管理
 */
public class LimDBUpgrade {
    private LimDBUpgrade() {
    }

    static class LimDBUpgradeBinder {
        final static LimDBUpgrade limDb = new LimDBUpgrade();
    }

    public static LimDBUpgrade getInstance() {
        return LimDBUpgradeBinder.limDb;
    }

    void onUpgrade(SQLiteDatabase db) {
        long maxIndex = LiMaoIMApplication.getInstance().getDBUpgradeIndex();
        List<LiMDBSql> list = getExecSQL();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).index > maxIndex && list.get(i).sqlList != null && list.get(i).sqlList.size() > 0) {
                for (String sql : list.get(i).sqlList) {
                    if (!TextUtils.isEmpty(sql)) {
                        db.execSQL(sql);
                    }
                }
                maxIndex = list.get(i).index;
            }
        }
        LiMaoIMApplication.getInstance().setDBUpgradeIndex(maxIndex);
    }

    private List<LiMDBSql> getExecSQL() {
        List<LiMDBSql> sqlList = new ArrayList<>();

        AssetManager assetManager = LiMaoIMApplication.getInstance().getContext().getAssets();
        if (assetManager != null) {
            try {
                String[] strings = assetManager.list("lim_sql");
                if (strings == null || strings.length == 0) {
                    Log.e("读取sql失败：", "--->");
                }
                assert strings != null;
                for (String str : strings) {
                    StringBuilder stringBuilder = new StringBuilder();
                    BufferedReader bf = new BufferedReader(new InputStreamReader(
                            assetManager.open("lim_sql/" + str)));
                    String line;
                    while ((line = bf.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    String temp = str.replaceAll(".sql", "");
                    List<String> list = new ArrayList<>();
                    if (stringBuilder.toString().contains(";")) {
                        list = Arrays.asList(stringBuilder.toString().split(";"));
                    } else list.add(stringBuilder.toString());
                    sqlList.add(new LiMDBSql(Long.parseLong(temp), list));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sqlList;
    }
}
