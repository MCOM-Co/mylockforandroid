package i4nc4mp.customLock;

import i4nc4mp.customLock.WidgInfo.WidgTable;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class WidgIdProvider extends ContentProvider {

    private static final String TAG = "WidgIdProvider";

    private static final String DATABASE_NAME = "widgId.db";
    private static final int DATABASE_VERSION = 2;
    private static final String WIDG_TABLE_NAME = "Ids";

    private static HashMap<String, String> sNotesProjectionMap;

    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + WIDG_TABLE_NAME + " ("
                    + WidgTable._ID + " INTEGER PRIMARY KEY,"
                    + WidgTable.ID + " INTEGER,"
                    + WidgTable.ROW + " INTEGER,"
                    + WidgTable.WIDTH + " INTEGER,"
                    + WidgTable.HEIGHT + " INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case NOTES:
            qb.setTables(WIDG_TABLE_NAME);
            qb.setProjectionMap(sNotesProjectionMap);
            break;

        case NOTE_ID:
            qb.setTables(WIDG_TABLE_NAME);
            qb.setProjectionMap(sNotesProjectionMap);
            qb.appendWhere(WidgTable._ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = WidgInfo.WidgTable.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            return WidgTable.CONTENT_TYPE;

        case NOTE_ID:
            return WidgTable.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }


        // Make sure that the fields are all set
        if (values.containsKey(WidgInfo.WidgTable.HEIGHT) == false) {
            values.put(WidgInfo.WidgTable.HEIGHT, 1);
        	//Log.v("provider error","no height");
        }

        if (values.containsKey(WidgInfo.WidgTable.WIDTH) == false) {
        	//Log.v("provider error","no width");
        	values.put(WidgInfo.WidgTable.WIDTH, 1);
        }

        if (values.containsKey(WidgInfo.WidgTable.ROW) == false) {
        	//Log.v("provider error","no row");
        	//we won't store a row when it is a filler entry
        	values.put(WidgInfo.WidgTable.ROW, 0);
        }

        if (values.containsKey(WidgInfo.WidgTable.ID) == false) {
        	//Log.v("provider error","no manager ID");
        	values.put(WidgInfo.WidgTable.ID, 0);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(WIDG_TABLE_NAME, WidgTable.ID, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(WidgInfo.WidgTable.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.delete(WIDG_TABLE_NAME, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.delete(WIDG_TABLE_NAME, WidgTable._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.update(WIDG_TABLE_NAME, values, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.update(WIDG_TABLE_NAME, values, WidgTable._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(WidgInfo.AUTHORITY, "wID", NOTES);
        sUriMatcher.addURI(WidgInfo.AUTHORITY, "wID/#", NOTE_ID);

        sNotesProjectionMap = new HashMap<String, String>();
        sNotesProjectionMap.put(WidgTable._ID, WidgTable._ID);
        sNotesProjectionMap.put(WidgTable.ID, WidgTable.ID);
        sNotesProjectionMap.put(WidgTable.ROW, WidgTable.ROW);
        sNotesProjectionMap.put(WidgTable.HEIGHT, WidgTable.HEIGHT);
        sNotesProjectionMap.put(WidgTable.WIDTH, WidgTable.WIDTH);
    }
}