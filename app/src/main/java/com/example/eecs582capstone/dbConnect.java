/*
Filename: dbConnect.java
Author(s): Abdelrahman Zeidan
Created: Mar 1
Last Modified:
Overview and Purpose: Database helper class that manages SQLite database creation, upgrades, user data, and session data for the application.
Notes:
*/

/*
Class Name: dbConnect
Description of Class Purpose/Function: This class connects the app to the SQLite database and provides methods for storing, retrieving, updating, and deleting user and session information.
*/
package com.example.eecs582capstone;

/*
Filename: dbConnect.java
Author(s): Abdelrahman Zeidan, Mya Hoersdig, Jackson Yanek
Created: 03-01-2026
Last Modified: 04-12-2026
Overview and Purpose: 
Notes: 
*/

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/*
dbConnect class: 
*/

public class dbConnect extends SQLiteOpenHelper {
    //Temp: needs to be translated into a cloud database for authentication.

    private static final String DB_NAME = "appName";
    private static final int DB_VERSION = 7;

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

    // Session detail columns
    private static final String COL_LABEL = "label";
    private static final String COL_VARIANCE_SCORE = "variance_score";
    private static final String COL_QUALITY_SCORE = "quality_score";
    private static final String COL_SLEEP = "sleep_hours";
    private static final String COL_MEAL = "meal_info";
    private static final String COL_CAFFEINE = "caffeine";
    private static final String COL_MOOD = "mood";
    private static final String COL_STRESS = "stress";
    private static final String COL_LOCATION = "location";
    private static final String COL_LIGHT = "light_level";
    private static final String COL_NOISE = "noise_level";
    private static final String COL_FAMILIARITY = "familiarity";
    private static final String COL_GENRE = "music_genre";
    private static final String COL_LYRICS = "lyrics_preference";
    private static final String COL_TEMPO = "tempo_bpm";

    // Optional session notes
    private static final String COL_NOTES = "session_notes";

    // Legacy snapshot columns kept for compatibility
    private static final String COL_Q1 = "quiz_q1";
    private static final String COL_Q2 = "quiz_q2";
    private static final String COL_Q3 = "quiz_q3";
    private static final String COL_Q4 = "quiz_q4";
    private static final String COL_Q5 = "quiz_q5";
    private static final String COL_Q6 = "quiz_q6";
    private static final String COL_Q7 = "quiz_q7";
    private static final String COL_Q8 = "quiz_q8";

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
                + COL_LABEL + " TEXT, "
                + COL_VARIANCE_SCORE + " INTEGER, "
                + COL_QUALITY_SCORE + " INTEGER, "
                + COL_SLEEP + " TEXT, "
                + COL_MEAL + " TEXT, "
                + COL_CAFFEINE + " TEXT, "
                + COL_MOOD + " TEXT, "
                + COL_STRESS + " INTEGER, "
                + COL_LOCATION + " TEXT, "
                + COL_LIGHT + " INTEGER, "
                + COL_NOISE + " INTEGER, "
                + COL_FAMILIARITY + " INTEGER, "
                + COL_GENRE + " TEXT, "
                + COL_LYRICS + " TEXT, "
                + COL_TEMPO + " INTEGER, "
                + COL_Q1 + " TEXT, "
                + COL_Q2 + " TEXT, "
                + COL_Q3 + " TEXT, "
                + COL_Q4 + " TEXT, "
                + COL_Q5 + " TEXT, "
                + COL_Q6 + " TEXT, "
                + COL_Q7 + " TEXT, "
                + COL_Q8 + " TEXT, "
                + COL_NOTES + " TEXT, "
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

        // Upgrade from version 2 to 4: add all of the new session detail columns
        if (oldVersion < 4) {
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_LABEL, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_VARIANCE_SCORE, "INTEGER");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_QUALITY_SCORE, "INTEGER");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_SLEEP, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_MEAL, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_CAFFEINE, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_MOOD, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_STRESS, "INTEGER");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_LOCATION, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_LIGHT, "INTEGER");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_NOISE, "INTEGER");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_FAMILIARITY, "INTEGER");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_GENRE, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_LYRICS, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_TEMPO, "INTEGER");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_Q1, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_Q2, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_Q3, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_Q4, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_Q5, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_Q6, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_Q7, "TEXT");
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_Q8, "TEXT");
        }
        // Upgrade to version 5: add session notes column
        if (oldVersion < 5) {
            addColumnIfNeeded(db, TABLE_SESSIONS, COL_NOTES, "TEXT");
        }
        // Version 7: ensure users table exists (may be missing on devices with corrupt v6 DB)
        if (oldVersion < 7) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_FIRSTNAME + " TEXT, "
                    + COL_LASTNAME + " TEXT, "
                    + COL_EMAIL + " TEXT, "
                    + COL_PASSWORD + " TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SESSIONS + " ("
                    + COL_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_USER_ID + " INTEGER, "
                    + COL_START_TIME + " INTEGER, "
                    + COL_END_TIME + " INTEGER, "
                    + COL_LABEL + " TEXT, "
                    + COL_VARIANCE_SCORE + " INTEGER, "
                    + COL_QUALITY_SCORE + " INTEGER, "
                    + COL_SLEEP + " TEXT, "
                    + COL_MEAL + " TEXT, "
                    + COL_CAFFEINE + " TEXT, "
                    + COL_MOOD + " TEXT, "
                    + COL_STRESS + " INTEGER, "
                    + COL_LOCATION + " TEXT, "
                    + COL_LIGHT + " INTEGER, "
                    + COL_NOISE + " INTEGER, "
                    + COL_FAMILIARITY + " INTEGER, "
                    + COL_GENRE + " TEXT, "
                    + COL_LYRICS + " TEXT, "
                    + COL_TEMPO + " INTEGER, "
                    + COL_Q1 + " TEXT, "
                    + COL_Q2 + " TEXT, "
                    + COL_Q3 + " TEXT, "
                    + COL_Q4 + " TEXT, "
                    + COL_Q5 + " TEXT, "
                    + COL_Q6 + " TEXT, "
                    + COL_Q7 + " TEXT, "
                    + COL_Q8 + " TEXT, "
                    + COL_NOTES + " TEXT, "
                    + "FOREIGN KEY(" + COL_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_ID + "))");
        }
    }

    // Helper used during upgrades so the app can safely add new columns
    private void addColumnIfNeeded(SQLiteDatabase db, String tableName, String columnName, String columnType) {
        try {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
        } catch (Exception ignored) {
            // If the column already exists, SQLite throws an exception.
            // We safely ignore it so the app can continue running.
        }
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

    // Creates a local user record if one doesn't already exist for this email.
    // Called after successful Supabase auth to ensure sessions FK is satisfied.
    public void ensureLocalUser(String email, String firstName, String lastName) {
        if (getUserByEmail(email) != null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FIRSTNAME, firstName);
        values.put(COL_LASTNAME, lastName);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, "");
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

    // Old method kept in case another screen still calls it
    public long startSession(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_ID, userId);
        values.put(COL_START_TIME, System.currentTimeMillis());
        long sessionId = db.insert(TABLE_SESSIONS, null, values);
        db.close();
        return sessionId;
    }

    // New method: starts a session and saves the pre-session survey answers immediately
    public long startSession(
            int userId,
            String sleep,
            String meal,
            String caffeine,
            String mood,
            int stress,
            String location,
            String genre,
            String lyrics,
            int tempo,
            int light,
            int noise,
            int familiarity,
            String notes
    ) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_ID, userId);
        values.put(COL_START_TIME, System.currentTimeMillis());
        values.put(COL_SLEEP, sleep);
        values.put(COL_MEAL, meal);
        values.put(COL_CAFFEINE, caffeine);
        values.put(COL_MOOD, mood);
        values.put(COL_STRESS, stress);
        values.put(COL_LOCATION, location);
        values.put(COL_GENRE, genre);
        values.put(COL_LYRICS, lyrics);
        values.put(COL_TEMPO, tempo);
        values.put(COL_LIGHT, light);
        values.put(COL_NOISE, noise);
        values.put(COL_FAMILIARITY, familiarity);
        if (notes != null && !notes.trim().isEmpty()) {
            values.put(COL_NOTES, notes.trim());
        }
        long sessionId = db.insert(TABLE_SESSIONS, null, values);
        db.close();
        return sessionId;
    }

    // Update just the notes field of an existing session
    public boolean updateSessionNotes(long sessionId, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (notes == null || notes.trim().isEmpty()) {
            values.putNull(COL_NOTES);
        } else {
            values.put(COL_NOTES, notes.trim());
        }
        int rows = db.update(TABLE_SESSIONS, values, COL_SESSION_ID + " = ?", new String[]{String.valueOf(sessionId)});
        db.close();
        return rows > 0;
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

    // Finds the next completed session that has not been processed into a Reading result yet
    public long getNextCompletedUnprocessedSessionId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COL_SESSION_ID + " FROM " + TABLE_SESSIONS +
                " WHERE " + COL_USER_ID + " = ? AND " + COL_END_TIME + " IS NOT NULL AND " + COL_LABEL + " IS NULL " +
                " ORDER BY " + COL_SESSION_ID + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        long sessionId = -1;
        if (cursor.moveToFirst()) {
            sessionId = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return sessionId;
    }

    // Counts only processed reading sessions
    public int getProcessedSessionCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_SESSIONS +
                " WHERE " + COL_USER_ID + " = ? AND " + COL_LABEL + " IS NOT NULL";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    // Saves the EEG analysis results into an existing session row
    public boolean saveProcessedResultsToExistingSession(long sessionId, String label, int varianceScore, int qualityScore) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LABEL, label);
        values.put(COL_VARIANCE_SCORE, varianceScore);
        values.put(COL_QUALITY_SCORE, qualityScore);
        int rows = db.update(TABLE_SESSIONS, values, COL_SESSION_ID + " = ?", new String[]{String.valueOf(sessionId)});
        db.close();
        return rows > 0;
    }

    // Returns all saved EEG sessions for one user, newest first
    public Cursor getAllSavedSessions(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_SESSIONS +
                        " WHERE " + COL_USER_ID + " = ? AND " + COL_LABEL + " IS NOT NULL " +
                        " ORDER BY " + COL_SESSION_ID + " DESC",
                new String[]{String.valueOf(userId)}
        );
    }

    // Returns the best processed session to display as optimal focus parameters on Home
    public Cursor getOptimalSessionForUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_SESSIONS +
                        " WHERE " + COL_USER_ID + " = ? AND " + COL_LABEL + " IS NOT NULL " +
                        " ORDER BY " + COL_QUALITY_SCORE + " DESC, " + COL_VARIANCE_SCORE + " DESC, " + COL_SESSION_ID + " DESC LIMIT 1",
                new String[]{String.valueOf(userId)}
        );
    }

    // Returns a single saved EEG session by database row id
    public Cursor getSavedSessionById(long sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_SESSIONS +
                        " WHERE " + COL_SESSION_ID + " = ?",
                new String[]{String.valueOf(sessionId)}
        );
    }

    // Deletes one saved EEG session
    public boolean deleteSession(long sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_SESSIONS, COL_SESSION_ID + " = ?", new String[]{String.valueOf(sessionId)});
        db.close();
        return rows > 0;
    }

    // Deletes all saved EEG sessions for one user
    public void deleteAllSessionsForUser(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SESSIONS, COL_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        db.close();
    }

    public Cursor getSessionsByDate(int userId, long start, long end) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM sessions WHERE user_id=? AND label IS NOT NULL AND start_time BETWEEN ? AND ? ORDER BY start_time DESC",
                new String[]{
                        String.valueOf(userId),
                        String.valueOf(start),
                        String.valueOf(end)
                }
        );
    }
}
