package com.example.hcenmobile.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hcenmobile.data.model.HistoriaClinicaItem;
import com.example.hcenmobile.data.repository.HistoriaClinicaRepository;

import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private final HistoriaClinicaRepository repository;
    private final MutableLiveData<List<HistoriaClinicaItem>> documentos = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private String ultimaCedula;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = HistoriaClinicaRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<List<HistoriaClinicaItem>> getDocumentos() {
        return documentos;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void cargarHistoria(String cedula) {
        ultimaCedula = cedula;
        loading.setValue(true);
        errorMessage.setValue(null);

        repository.cargarHistoriaClinica(cedula, new HistoriaClinicaRepository.HistoriaCallback() {
            @Override
            public void onSuccess(List<HistoriaClinicaItem> items) {
                loading.postValue(false);
                errorMessage.postValue(null);
                documentos.postValue(items);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void refrescar() {
        if (ultimaCedula != null && !ultimaCedula.isEmpty()) {
            cargarHistoria(ultimaCedula);
        }
    }
}
