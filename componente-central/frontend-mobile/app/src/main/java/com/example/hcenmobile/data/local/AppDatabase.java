package com.example.hcenmobile.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.hcenmobile.data.model.Notificacion;
import com.example.hcenmobile.util.Constants;

/**
 * Base de datos local Room para la app HCEN Mobile
 */
@Database(entities = {Notificacion.class}, version = Constants.DATABASE_VERSION, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract NotificacionDao notificacionDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    Constants.DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // Para desarrollo; en producción usar migraciones
            .build();
        }
        return INSTANCE;
    }
}
