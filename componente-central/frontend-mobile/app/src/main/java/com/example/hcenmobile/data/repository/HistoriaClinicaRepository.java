package com.example.hcenmobile.data.repository;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.model.HistoriaClinicaItem;
import com.example.hcenmobile.data.remote.ApiService;
import com.example.hcenmobile.data.remote.RetrofitClient;
import com.example.hcenmobile.data.remote.dto.ApiResponse;
import com.example.hcenmobile.data.remote.dto.HistoriaClinicaDocumentoDTO;
import com.example.hcenmobile.util.DateFormatterUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository encargado de orquestar las llamadas al componente central y periférico
 * para obtener la historia clínica del usuario.
 */
public class HistoriaClinicaRepository {

    private static final String TAG = "HistoriaRepository";
    private static HistoriaClinicaRepository instance;

    private final ApiService apiService;
    private final Context context;

    private HistoriaClinicaRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public static synchronized HistoriaClinicaRepository getInstance(Context context) {
        if (instance == null) {
            instance = new HistoriaClinicaRepository(context);
        }
        return instance;
    }

    public void cargarHistoriaClinica(String cedula, HistoriaCallback callback) {
        if (TextUtils.isEmpty(cedula)) {
            if (callback != null) {
                callback.onError(context.getString(R.string.historia_error_sin_documentos));
            }
            return;
        }

        apiService.getHistoriaClinicaDocumentos(cedula)
                .enqueue(new Callback<ApiResponse<List<HistoriaClinicaDocumentoDTO>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<HistoriaClinicaDocumentoDTO>>> call,
                                           @NonNull Response<ApiResponse<List<HistoriaClinicaDocumentoDTO>>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            notifyError(callback,
                                    "Error " + response.code() + " al consultar la historia clínica");
                            return;
                        }

                        ApiResponse<List<HistoriaClinicaDocumentoDTO>> body = response.body();
                        if (!body.isSuccess()) {
                            notifyError(callback, body.getError() != null ? body.getError() : body.getMessage());
                            return;
                        }

                        List<HistoriaClinicaDocumentoDTO> documentos = body.getData();
                        if (documentos == null || documentos.isEmpty()) {
                            if (callback != null) {
                                callback.onSuccess(Collections.emptyList());
                            }
                            return;
                        }

                        if (callback != null) {
                            callback.onSuccess(convertirADominio(documentos));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<HistoriaClinicaDocumentoDTO>>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "Error al obtener documentos de historia clínica", t);
                        notifyError(callback, t.getMessage());
                    }
                });
    }

    private List<HistoriaClinicaItem> convertirADominio(List<HistoriaClinicaDocumentoDTO> documentos) {
        List<HistoriaClinicaItem> items = new ArrayList<>();
        for (HistoriaClinicaDocumentoDTO dto : documentos) {
            if (dto.getDocumentoId() == null) {
                continue;
            }
            items.add(mapear(dto));
        }
        return items;
    }

    private HistoriaClinicaItem mapear(HistoriaClinicaDocumentoDTO dto) {
        String motivo = dto.getMotivoConsulta();
        if (TextUtils.isEmpty(motivo)) {
            motivo = context.getString(R.string.historia_motivo_indefinido);
        }

        String fecha = DateFormatterUtil.formatDateTime(
                dto.getFechaDocumento(),
                dto.getFechaRegistro());

        String profesional = dto.getProfesional();
        if (TextUtils.isEmpty(profesional)) {
            profesional = context.getString(R.string.historia_profesional_indefinido);
        }

        return new HistoriaClinicaItem(
                dto.getDocumentoId(),
                dto.getTenantId(),
                motivo,
                fecha,
                profesional
        );
    }

    private void notifyError(HistoriaCallback callback, String message) {
        if (callback != null) {
            String error = message != null && !message.isEmpty()
                    ? message
                    : context.getString(R.string.historia_error_message);
            callback.onError(error);
        }
    }

    public interface HistoriaCallback {
        void onSuccess(List<HistoriaClinicaItem> items);

        void onError(String message);
    }
}
