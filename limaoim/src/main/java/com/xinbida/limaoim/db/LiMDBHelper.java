package com.xinbida.limaoim.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.xinbida.limaoim.utils.LiMLoggerUtils;

/**
 * 2019-11-12 13:57
 * 数据库辅助类
 */
public class LiMDBHelper {
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    public SQLiteDatabase getDb() {
        return mDb;
    }

    private volatile static LiMDBHelper openHelper = null;
    private final static int version = 1;
    private static String myDBName;
    private static String uid;

    private LiMDBHelper(Context ctx, String uid) {
        LiMDBHelper.uid = uid;
        myDBName = "lim_" + uid + ".db";

        try {
            mDbHelper = new DatabaseHelper(ctx);
            mDb = mDbHelper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建数据库实例
     *
     * @param context 上下文
     * @param _uid    用户ID
     * @return db
     */
    public synchronized static LiMDBHelper getInstance(Context context, String _uid) {
        if (TextUtils.isEmpty(uid) || !uid.equals(_uid) || openHelper == null) {
            synchronized (LiMDBHelper.class) {
                if (openHelper != null) {
                    openHelper.close();
                    openHelper = null;
                }
                openHelper = new LiMDBHelper(context, _uid);
            }
        }
        return openHelper;
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, myDBName, null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            onUpgrade(db, 0, version);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
            LimDBUpgrade.getInstance().onUpgrade(db);
        }
    }

    /**
     * 关闭数据库
     */
    public void close() {
        try {
            uid = "";
            if (mDb != null) {
                mDb.close();
                mDb = null;
            }

            if (mDbHelper != null) {
                mDbHelper.close();
                mDbHelper = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void execSQL(String sql) {
        try {
            if (mDb == null) return;
            mDb.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void execSQL(String sql, Object[] bindArgs) {
        try {
            if (mDb == null) return;
            mDb.execSQL(sql, bindArgs);
            mDb.insertWithOnConflict("", "", new ContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    long insertSql(String tab, ContentValues cv) {
        return mDb.insertWithOnConflict(tab, "", cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    Cursor rawQuery(String sql) {
        return mDb.rawQuery(sql, null);
    }

    Cursor select(String table, String selection,
                  String[] selectionArgs,
                  String orderBy) {
        if (mDb == null) return null;
        Cursor cursor;
        try {
            cursor = mDb.query(table, null, selection, selectionArgs,
                    null, null, orderBy);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return cursor;
    }

    /**
     * 插入指定表中
     *
     * @param table           表名称
     * @param fields          字段名称
     * @param modelFieldtypes 值
     * @return 结果
     */
    public long insert(String table, String[] fields, String[] modelFieldtypes) {
        if (mDb == null) return 0;
        synchronized (this) {
            ContentValues cv = new ContentValues();
            for (int i = 0; i < fields.length; i++) {
                cv.put(fields[i], modelFieldtypes[i]);
            }
            long count = 0;
            try {
                count = mDb.insert(table, null, cv);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return count;
        }
    }

    public long insert(String table, ContentValues cv) {
        if (mDb == null) return 0;
        // synchronized (this) {
        long count = 0;
//		Cursor cursor = null;
        try {
            count = mDb.insert(table, null, cv);
//			cursor = mDb.rawQuery("select last_insert_rowid() from " + table,
//					null);
//			if (cursor.moveToFirst()) {
//				count = cursor.getInt(0);
//			}
        } catch (Exception e) {
            LiMLoggerUtils.getInstance().e("数据库插入异常:" + e.getMessage());
            e.printStackTrace();
        }
        return count;
        // }
    }

    public boolean delete(String tableName, String where, String[] whereValue) {
        if (mDb == null) return false;
        int count = mDb.delete(tableName, where, whereValue);
        return count > 0;
    }

    public int update(String table, String[] updateFields,
                      String[] updateValues, String where, String[] whereValue) {
        if (mDb == null) return 0;
        ContentValues cv = new ContentValues();
        for (int i = 0; i < updateFields.length; i++) {
            cv.put(updateFields[i], updateValues[i]);
        }
        int count = 0;
        try {
            count = mDb.update(table, cv, where, whereValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public boolean update(String tableName, ContentValues cv, String where,
                          String[] whereValue) {
        if (mDb == null) return false;
        boolean flag = false;
        try {
            flag = mDb.update(tableName, cv, where, whereValue) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public boolean update(String tableName, String whereClause,
                          ContentValues args) {
        if (mDb == null) return false;
        boolean flag = false;
        try {
            flag = mDb.update(tableName, args, whereClause, null) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    Cursor queryLimit(int pageSize, int pageNum, String tableName,
                      String orderBy, String where) {
        if (mDb == null) return null;
        return mDb.rawQuery("select * from " + tableName + "  where "
                + where + " order by " + orderBy + " desc limit " + pageNum
                + "," + pageSize, null);
    }

    public Cursor queryLimitAsc(int pageSize, int pageNum, String tableName,
                                String orderBy, String where) {
        // Log.e("pageSize1", pageSize + "/pageSize");
        // Log.e("pageNum1", pageNum + "/pageNum");
        // Log.e("orderby1", orderBy + "*****");
        if (mDb == null) return null;
        String sql = "select * from " + tableName + "  where "
                + where + " order by " + orderBy + " asc limit " + pageSize
                + " offset " + pageNum;
        return mDb.rawQuery(sql, null);
    }

    public Cursor queryOrder(String tableName, String orderBy, String where,
                             String[] whereValues) {
        if (mDb == null) return null;
        return mDb.rawQuery("select * from " + tableName + "  where "
                + where + " order by " + orderBy + " asc", whereValues);
    }

}