package com.nuk.meetinggo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseConnector {

    // Database name
    private static final String DATABASE_NAME = "testa.db";
    private SQLiteDatabase database;
    private DatabaseOpenHelper databaseOpenHelper;

    public DatabaseConnector(Context context) {
        databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
    }

    // Open the database connection
    public void open() throws SQLException {
        database = databaseOpenHelper.getWritableDatabase();
    }

    // Close the database connection
    public void close() {
        if(database != null)
            database.close();
    }

    // Inserts a new document in the database
    public void insertDocument(String name, String link) {
        ContentValues newDoc = new ContentValues();
        newDoc.put("name", name);
        newDoc.put("link", link);

        open();
        database.insert("documents", null, newDoc);
        close();
    }

    // Inserts a new document in the database
    public void updateDocument(long id, String name, String link) {
        ContentValues editDoc = new ContentValues();
        editDoc.put("name", name);
        editDoc.put("link", link);

        open();
        database.update("documents", editDoc, "_id=" + id, null);
        close();
    }

    // Return a cursor with all document information in the database
    public synchronized Cursor getAllDocuments() {
        open();
        //Cursor cursor = database.query("documents", new String[]{"_id", "name"}, null, null, null, null, "name");
        //close();
        //return cursor;
        return database.query("documents", new String[]{"_id", "name"}, null, null, null, null, "name");
    }

    // Get a cursor containing all information about the document specified by the given id
    public synchronized Cursor getOneDocument(long id) {
        open();
        //Cursor cursor = database.query("documents", null, "_id='" + id, null, null, null, null);
        //close();
        //return cursor;
        return database.query("documents", null, "_id=" + id, null, null, null, null);
    }

    // Delete the document specified by the given String name
    public void deleteDocument(long id) {
        open();
        database.delete("documents", "_id=" + id, null);
        close();
    }
}
