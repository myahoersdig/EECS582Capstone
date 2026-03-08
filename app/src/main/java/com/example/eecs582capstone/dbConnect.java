package com.example.eecs582capstone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class dbConnect extends SQLiteOpenHelper {

    private static final String DB_NAME = "appName";
    private static final int DB_VERSION = 2;   // Incremented to trigger onUpgrade

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COL_ID = "id";
    private static final String COL_FIRSTNAME = "firstname";
    private static final String COL_LASTNAME = "lastname";
    private static final String COL_EMAIL = "emailAddress";
    private static final String COL_PASSWORD = "password";

    // Sessions table
    private static final String TABLE_SESSIONS = "sessions";
    private static final String COL_SESSION_ID = "session_id";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_START_TIME = "start_time";
    private static final String COL_END_TIME = "end_time";

    public dbConnect(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON;");

        // Users table
        String createUserTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_FIRSTNAME + " TEXT, "
                + COL_LASTNAME + " TEXT, "
                + COL_EMAIL + " TEXT, "
                + COL_PASSWORD + " TEXT)";
        db.execSQL(createUserTable);

        // Sessions table
        String createSessionTable = "CREATE TABLE " + TABLE_SESSIONS + " ("
                + COL_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_ID + " INTEGER, "
                + COL_START_TIME + " INTEGER, "
                + COL_END_TIME + " INTEGER, "
                + "FOREIGN KEY(" + COL_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_ID + "))";
        db.execSQL(createSessionTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade from version 1 to 2: add sessions table
        if (oldVersion < 2) {
            String createSessionTable = "CREATE TABLE IF NOT EXISTS " + TABLE_SESSIONS + " ("
                    + COL_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_USER_ID + " INTEGER, "
                    + COL_START_TIME + " INTEGER, "
                    + COL_END_TIME + " INTEGER, "
                    + "FOREIGN KEY(" + COL_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_ID + "))";
            db.execSQL(createSessionTable);
        }
        // Future upgrades can be chained here
    }

    // ---------- Users methods ----------
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

    // ---------- Sessions methods ----------
    public long startSession(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_ID, userId);
        values.put(COL_START_TIME, System.currentTimeMillis());
        long sessionId = db.insert(TABLE_SESSIONS, null, values);
        db.close();
        return sessionId;
    }

    public void endSession(long sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_END_TIME, System.currentTimeMillis());
        db.update(TABLE_SESSIONS, values, COL_SESSION_ID + " = ?", new String[]{String.valueOf(sessionId)});
        db.close();
    }

    public boolean hasActiveSession(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SESSIONS +
                " WHERE " + COL_USER_ID + " = ? AND " + COL_END_TIME + " IS NULL";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public long getActiveSessionId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COL_SESSION_ID + " FROM " + TABLE_SESSIONS +
                " WHERE " + COL_USER_ID + " = ? AND " + COL_END_TIME + " IS NULL";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        long sessionId = -1;
        if (cursor.moveToFirst()) {
            sessionId = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return sessionId;
    }
}