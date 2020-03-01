package com.example.trackmytime;

import android.provider.BaseColumns;

final class DatabaseContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DatabaseContract() {}

    /* Inner class that defines the table contents */
    static class DatabaseEntry implements BaseColumns {
        static final String TABLE_NAME = "timeTracking";
        static final String COLUMN_NAME_DATE = "date";
        static final String COLUMN_NAME_TIMEDIFF = "timeDifference";
    }


}
