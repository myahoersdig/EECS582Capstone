package com.example.eecs582capstone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS +
                " WHERE " + COL_EMAIL + " = ? AND " + COL_PASSWORD + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{email, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public Users getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{email});

        Users user = null;
        if (cursor.moveToFirst()) {
            user = new Users(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_FIRSTNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LASTNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD))
            );
        }

        cursor.close();
        db.close();
        return user;
    }
}