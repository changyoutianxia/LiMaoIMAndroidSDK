package com.xinbida.limaoim.db;

import java.util.List;

/**
 * 2020-09-08 09:58
 * 升级管理
 */
public class LiMDBSql {
    public long index;
    public List<String> sqlList;

    public LiMDBSql(long index, List<String> sqlList) {
        this.index = index;
        this.sqlList = sqlList;
    }
}
