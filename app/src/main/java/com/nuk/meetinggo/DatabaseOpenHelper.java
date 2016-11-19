package com.nuk.meetinggo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {

    public DatabaseOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Query to create a new table named contacts
        String createQuery = "CREATE TABLE documents"
                + "(_id integer primary key autoincrement,"
                + " name TEXT,"
                + " link TEXT);";

        db.execSQL(createQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String DROP_TABLE = "DROP TABLE IF EXISTS documents";
        db.execSQL(DROP_TABLE);
        onCreate(db);
    }
}
