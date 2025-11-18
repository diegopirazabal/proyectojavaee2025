package com.example.hcenmobile.ui.notifications;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hcenmobile.data.model.Notificacion;
import com.example.hcenmobile.data.repository.NotificacionRepository;

import java.util.List;

public class NotificationsViewModel extends AndroidViewModel {

    private final NotificacionRepository repository;
    private final LiveData<List<Notificacion>> notificaciones;
    private final LiveData<Integer> countNoLeidas;
    private final MutableLiveData<String> errorMessage;
    private final MutableLiveData<Boolean> isRefreshing;

    public NotificationsViewModel(@NonNull Application application) {
        super(application);
        repository = NotificacionRepository.getInstance(application);
        notificaciones = repository.getAllNotificaciones();
        countNoLeidas = repository.getCountNotificacionesNoLeidas();
        errorMessage = new MutableLiveData<>();
        isRefreshing = new MutableLiveData<>(false);
    }

    public LiveData<List<Notificacion>> getNotificaciones() {
        return notificaciones;
    }

    public LiveData<Integer> getCountNoLeidas() {
        return countNoLeidas;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }

    public void sincronizarNotificaciones(String userId) {
        isRefreshing.setValue(true);
        repository.sincronizarNotificaciones(userId, new NotificacionRepository.SyncCallback() {
            @Override
            public void onSuccess() {
                isRefreshing.postValue(false);
            }

            @Override
            public void onError(String error) {
                isRefreshing.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    public void marcarComoLeida(long id) {
        repository.marcarComoLeida(id);
    }

    public void marcarTodasComoLeidas() {
        repository.marcarTodasComoLeidas();
    }

    public void eliminarNotificacion(long id) {
        repository.eliminarNotificacion(id);
    }
}
