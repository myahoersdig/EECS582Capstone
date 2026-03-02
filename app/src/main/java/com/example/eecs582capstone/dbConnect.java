package com.example.eecs582capstone;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class dbConnect extends SQLiteOpenHelper {

    private static final String DB_NAME = "appName";
    private static final String TABLE_USERS = "users";
    private static final int DB_VERSION = 1;

    private static final String COL_ID = "id";
    private static final String COL_FIRSTNAME = "firstname";
    private static final String COL_LASTNAME = "lastname";
    private static final String COL_EMAIL = "emailAddress";
    private static final String COL_PASSWORD = "password";

    public dbConnect(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_FIRSTNAME + " TEXT, "
                + COL_LASTNAME + " TEXT, "
                + COL_EMAIL + " TEXT, "
                + COL_PASSWORD + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public void addUser(Users user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FIRSTNAME, user.getFirstname());
        values.put(COL_LASTNAME, user.getLastname());
        values.put(COL_EMAIL, user.getEmailAddress());
        values.put(COL_PASSWORD, user.getPassword());
        db.insert(TABLE_USERS, null, values);
        db.close();
    }
}