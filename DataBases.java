package com.example.pgm.myapplication;

/**
 * Created by DowonYoon on 2017-06-21.
 */

import android.provider.BaseColumns;

public final class DataBases {

    public static final class CreateDB implements BaseColumns{
        public static final String NAME = "name";
        public static final String PHONE = "phone";
        public static final String _TABLENAME0 = "usertable";
        public static final String _CREATE0 = "create table if not exists "+_TABLENAME0+"("
                +_ID+" integer primary key autoincrement, "
                +NAME+" text not null , "
                +PHONE+" integer not null );";
    }
}