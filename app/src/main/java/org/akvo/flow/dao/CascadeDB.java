/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */
package org.akvo.flow.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.akvo.flow.domain.Node;

import java.util.ArrayList;
import java.util.List;

public class CascadeDB {
    public static final String TABLE_PATH = "path";

    private String mDBPath;

    public interface PathColumns {
        String _ID = "_id";
        String VALUE = "value";
        String PARENT = "parent";
    }

    private DatabaseHelper mHelper;
    public SQLiteDatabase mDatabase;

    private static final int VERSION = 1;

    private final Context mContext;

    public CascadeDB(Context context, String dbPath) {
        mContext = context;
        mDBPath = dbPath;
    }

    public void open() throws SQLException {
        mHelper = new DatabaseHelper(mContext, mDBPath);
        mDatabase = mHelper.getReadableDatabase();
    }

    public void close() {
        mHelper.close();
    }

    public List<Node> getValues(long parent) {
        Cursor c = mDatabase.query(true, TABLE_PATH,
                new String[]{PathColumns._ID, PathColumns.VALUE},
                PathColumns.PARENT + "=?",
                new String[]{String.valueOf(parent)},
                null, null, null, null);

        final List<Node> result = new ArrayList<Node>();
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    result.add(new Node(c.getLong(0), c.getString(1)));
                } while (c.moveToNext());
            }
            c.close();
        }
        return result;
    }

    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context, String dbPath) {
            //super(context, context.getExternalFilesDir(null) + "/" + DATABASE_NAME, null, VERSION);
            super(context, dbPath, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

    }

}
