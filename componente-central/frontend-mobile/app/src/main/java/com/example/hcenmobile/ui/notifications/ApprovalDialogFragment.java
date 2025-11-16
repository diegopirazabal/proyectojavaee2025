package com.example.hcenmobile.ui.notifications;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.remote.ApiService;
import com.example.hcenmobile.data.remote.RetrofitClient;
import com.example.hcenmobile.data.remote.dto.ApiResponse;
import com.example.hcenmobile.data.remote.dto.AprobarSolicitudRequest;
import com.example.hcenmobile.data.remote.dto.SolicitudAccesoDTO;
import com.example.hcenmobile.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dialog para aprobar solicitudes de acceso a documentos clínicos
 * Permite seleccionar el tipo de permiso a otorgar
 */
public class ApprovalDialogFragment extends DialogFragment {

    private static final String ARG_SOLICITUD = "solicitud";

    private SolicitudAccesoDTO solicitud;
    private OnApprovalListener listener;

    public interface OnApprovalListener {
        void onApprovalSuccess();
        void onApprovalError(String message);
    }

    public static ApprovalDialogFragment newInstance(SolicitudAccesoDTO solicitud) {
        ApprovalDialogFragment fragment = new ApprovalDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SOLICITUD, (java.io.Serializable) solicitud);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnApprovalListener(OnApprovalListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Nota: En producción, deberías usar Parcelable en lugar de Serializable
            solicitud = (SolicitudAccesoDTO) getArguments().getSerializable(ARG_SOLICITUD);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_approval, null);

        // Referencias a vistas
        TextView profesionalInfo = view.findViewById(R.id.profesional_info);
        TextView clinicaInfo = view.findViewById(R.id.clinica_info);
        TextView documentoInfo = view.findViewById(R.id.documento_info);
        TextView especialidadDesc = view.findViewById(R.id.especialidad_description);
        RadioGroup permissionTypeGroup = view.findViewById(R.id.permission_type_group);
        RadioButton radioPorEspecialidad = view.findViewById(R.id.radio_por_especialidad);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnApprove = view.findViewById(R.id.btn_approve);

        // Configurar información de la solicitud
        if (solicitud != null) {
            profesionalInfo.setText(String.format("%s (CI: %d)",
                    solicitud.getProfesionalNombre(),
                    solicitud.getProfesionalCi()));
            clinicaInfo.setText(solicitud.getNombreClinica());
            documentoInfo.setText("Documento del " + solicitud.getFechaDocumento());

            // Configurar descripción de especialidad
            String especialidad = solicitud.getEspecialidad();
            if (especialidad != null && !especialidad.isEmpty()) {
                especialidadDesc.setText(String.format(
                        "Permite acceso a cualquier profesional de %s en esta clínica",
                        especialidad));
            } else {
                // Si no hay especialidad, deshabilitar esta opción
                radioPorEspecialidad.setEnabled(false);
                especialidadDesc.setText("(Especialidad no especificada)");
            }
        }

        // Listener para cancelar
        btnCancel.setOnClickListener(v -> dismiss());

        // Listener para aprobar
        btnApprove.setOnClickListener(v -> {
            int selectedId = permissionTypeGroup.getCheckedRadioButtonId();
            String tipoPermiso;

            if (selectedId == R.id.radio_profesional_especifico) {
                tipoPermiso = "PROFESIONAL_ESPECIFICO";
            } else if (selectedId == R.id.radio_por_especialidad) {
                tipoPermiso = "POR_ESPECIALIDAD";
            } else if (selectedId == R.id.radio_por_clinica) {
                tipoPermiso = "POR_CLINICA";
            } else {
                Toast.makeText(getContext(), "Seleccione un tipo de permiso", Toast.LENGTH_SHORT).show();
                return;
            }

            aprobarSolicitud(tipoPermiso);
        });

        builder.setView(view);
        return builder.create();
    }

    private void aprobarSolicitud(String tipoPermiso) {
        // Deshabilitar botón para evitar doble click
        Dialog dialog = getDialog();
        if (dialog != null) {
            Button btnApprove = dialog.findViewById(R.id.btn_approve);
            if (btnApprove != null) {
                btnApprove.setEnabled(false);
                btnApprove.setText("Procesando...");
            }
        }

        // Obtener cédula del usuario actual
        String cedula = SessionManager.getUserCI(requireContext());
        if (cedula == null || cedula.isEmpty()) {
            Toast.makeText(getContext(), "Error: No se pudo obtener la cédula del usuario", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        // Construir request
        AprobarSolicitudRequest request = new AprobarSolicitudRequest();
        request.setNotificacionId(solicitud.getId());
        request.setCedulaPaciente(cedula);
        request.setDocumentoId(solicitud.getDocumentoId());
        request.setTipoPermiso(tipoPermiso);
        request.setTenantId(solicitud.getTenantId());

        // Configurar campos opcionales según tipo de permiso
        if ("PROFESIONAL_ESPECIFICO".equals(tipoPermiso)) {
            request.setProfesionalCi(solicitud.getProfesionalCi());
        } else if ("POR_ESPECIALIDAD".equals(tipoPermiso)) {
            request.setEspecialidad(solicitud.getEspecialidad());
        }

        // Llamar al API
        ApiService apiService = RetrofitClient.getInstance(requireContext()).getApiService();
        Call<ApiResponse<Void>> call = apiService.aprobarSolicitud(request);

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                   @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "Permiso otorgado exitosamente", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onApprovalSuccess();
                    }
                    dismiss();
                } else {
                    String errorMsg = "Error al aprobar solicitud";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMsg = response.body().getError();
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    if (listener != null) {
                        listener.onApprovalError(errorMsg);
                    }
                    dismiss();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                String errorMsg = "Error de conexión: " + t.getMessage();
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                if (listener != null) {
                    listener.onApprovalError(errorMsg);
                }
                dismiss();
            }
        });
    }
}
