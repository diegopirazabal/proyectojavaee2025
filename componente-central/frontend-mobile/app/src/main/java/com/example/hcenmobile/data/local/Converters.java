package com.example.hcenmobile.data.local;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Type converters para Room Database
 */
public class Converters {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
