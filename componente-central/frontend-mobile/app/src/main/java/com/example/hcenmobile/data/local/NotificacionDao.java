package com.example.hcenmobile.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hcenmobile.data.model.Notificacion;

import java.util.List;

/**
 * Data Access Object para operaciones de base de datos sobre Notificaciones
 */
@Dao
public interface NotificacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Notificacion notificacion);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Notificacion> notificaciones);

    @Update
    void update(Notificacion notificacion);

    @Delete
    void delete(Notificacion notificacion);

    @Query("SELECT * FROM notificaciones ORDER BY fechaHora DESC")
    LiveData<List<Notificacion>> getAllNotificaciones();

    @Query("SELECT * FROM notificaciones WHERE leida = 0 ORDER BY fechaHora DESC")
    LiveData<List<Notificacion>> getNotificacionesNoLeidas();

    @Query("SELECT * FROM notificaciones WHERE id = :id")
    LiveData<Notificacion> getNotificacionById(long id);

    @Query("SELECT * FROM notificaciones WHERE notificacionId = :notificacionId LIMIT 1")
    Notificacion getNotificacionByServerId(String notificacionId);

    @Query("UPDATE notificaciones SET leida = 1 WHERE id = :id")
    void marcarComoLeida(long id);

    @Query("UPDATE notificaciones SET leida = 1")
    void marcarTodasComoLeidas();

    @Query("DELETE FROM notificaciones WHERE id = :id")
    void eliminarPorId(long id);

    @Query("DELETE FROM notificaciones")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM notificaciones WHERE leida = 0")
    LiveData<Integer> getCountNotificacionesNoLeidas();

    @Query("SELECT * FROM notificaciones WHERE tipo = :tipo ORDER BY fechaHora DESC")
    LiveData<List<Notificacion>> getNotificacionesPorTipo(String tipo);
}
