package com.example.hcenmobile.ui.notifications.detail;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.remote.ApiService;
import com.example.hcenmobile.data.remote.RetrofitClient;
import com.example.hcenmobile.data.remote.dto.ApiResponse;
import com.example.hcenmobile.data.remote.dto.AprobarSolicitudRequest;
import com.example.hcenmobile.data.remote.dto.RechazarSolicitudRequest;
import com.example.hcenmobile.data.remote.dto.SolicitudAccesoDTO;
import com.example.hcenmobile.databinding.FragmentNotificationDetailBinding;
import com.example.hcenmobile.util.Constants;
import com.example.hcenmobile.util.SessionManager;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.gson.Gson;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de detalle de una notificación.
 * Permite otorgar o rechazar permisos para solicitudes de acceso.
 */
public class NotificationDetailFragment extends Fragment {

    public static final String ARG_NOTIFICATION_SERVER_ID = "notification_server_id";
    public static final String ARG_NOTIFICATION_LOCAL_ID = "notification_local_id";
    public static final String ARG_NOTIFICATION_TYPE = "notification_type";
    public static final String ARG_NOTIFICATION_TITLE = "notification_title";
    public static final String ARG_NOTIFICATION_MESSAGE = "notification_message";
    public static final String ARG_NOTIFICATION_REMITENTE = "notification_remitente";
    public static final String ARG_NOTIFICATION_FECHA = "notification_fecha";
    public static final String ARG_NOTIFICATION_DATOS = "notification_datos";

    private static final int DEFAULT_EXPIRATION_DAYS = 15;
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private FragmentNotificationDetailBinding binding;
    private final Gson gson = new Gson();
    private final ZoneId zoneId = ZoneId.systemDefault();

    private SolicitudAccesoDTO solicitud;
    private String notificationServerId;
    private String notificationTitle;
    private String notificationMessage;
    private String notificationRemitente;
    private String notificationTipo;
    private long notificationDateMillis;

    private boolean isSolicitudAcceso;
    private boolean specialtyAvailable;
    private LocalDate selectedExpirationDate;
    private String especialidadSeleccionada;

    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationDetailBinding.inflate(inflater, container, false);
        apiService = RetrofitClient.getInstance(requireContext()).getApiService();
        readArguments();
        setupUi();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void readArguments() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        notificationServerId = args.getString(ARG_NOTIFICATION_SERVER_ID);
        notificationTitle = args.getString(ARG_NOTIFICATION_TITLE);
        notificationMessage = args.getString(ARG_NOTIFICATION_MESSAGE);
        notificationRemitente = args.getString(ARG_NOTIFICATION_REMITENTE);
        notificationTipo = args.getString(ARG_NOTIFICATION_TYPE);
        notificationDateMillis = args.getLong(ARG_NOTIFICATION_FECHA, System.currentTimeMillis());

        String datosJson = args.getString(ARG_NOTIFICATION_DATOS);
        if (!TextUtils.isEmpty(datosJson)) {
            try {
                solicitud = gson.fromJson(datosJson, SolicitudAccesoDTO.class);
            } catch (Exception ignored) {
                solicitud = null;
            }
        }

        isSolicitudAcceso = "SOLICITUD_ACCESO".equalsIgnoreCase(notificationTipo)
                || Constants.NOTIF_TYPE_ACCESS_REQUEST.equalsIgnoreCase(notificationTipo);

        specialtyAvailable = solicitud != null && !TextUtils.isEmpty(solicitud.getEspecialidad());
        selectedExpirationDate = LocalDate.now().plusDays(DEFAULT_EXPIRATION_DAYS);
        especialidadSeleccionada = solicitud != null ? solicitud.getEspecialidad() : null;
    }

    private void setupUi() {
        if (isSolicitudAcceso && solicitud != null) {
            binding.cardRequestInfo.setVisibility(View.VISIBLE);
            binding.textProfesional.setText(formatProfesionalText());
            binding.textClinica.setText(solicitud.getNombreClinica());
            binding.textDocumento.setText(getString(R.string.notification_detail_documento_format,
                    solicitud.getFechaDocumento()));
            binding.textMotivo.setText(!TextUtils.isEmpty(solicitud.getMotivoConsulta())
                    ? solicitud.getMotivoConsulta()
                    : getString(R.string.notification_detail_motivo_placeholder));
            binding.textEspecialidad.setText(specialtyAvailable
                    ? solicitud.getEspecialidad()
                    : getString(R.string.notification_detail_especialidad_placeholder));
            if (!TextUtils.isEmpty(solicitud.getDiagnostico())) {
                binding.textDiagnostico.setText(solicitud.getDiagnostico());
            } else {
                binding.textDiagnostico.setVisibility(View.GONE);
            }
        } else {
            binding.cardRequestInfo.setVisibility(View.GONE);
        }

        setupDateCard();
        setupButtons();
    }

    private void setupDateCard() {
        binding.cardExpiration.setVisibility(isSolicitudAcceso ? View.VISIBLE : View.GONE);
        binding.buttonChangeDate.setOnClickListener(v -> showDatePicker());
        updateExpirationLabel();
    }

    private void setupButtons() {
        if (!isSolicitudAcceso || solicitud == null) {
            binding.textNoActions.setVisibility(View.VISIBLE);
            binding.actionsContainer.setVisibility(View.GONE);
            return;
        }

        binding.textNoActions.setVisibility(View.GONE);
        binding.actionsContainer.setVisibility(View.VISIBLE);

        binding.btnPermitirProfesional.setOnClickListener(v ->
                enviarAprobacion("PROFESIONAL_ESPECIFICO", null));

        binding.btnPermitirEspecialidad.setOnClickListener(v ->
                manejarPermisoEspecialidad());

        binding.btnPermitirClinica.setOnClickListener(v ->
                enviarAprobacion("POR_CLINICA", null));

        binding.btnNoPermiso.setOnClickListener(v -> mostrarDialogoRechazo());
    }

    private void showDatePicker() {
        long selection = selectedExpirationDate
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli();

        long start = LocalDate.now()
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(start)
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.notification_detail_picker_title)
                .setSelection(selection)
                .setCalendarConstraints(constraints)
                .build();

        picker.addOnPositiveButtonClickListener(value -> {
            if (value == null) {
                Toast.makeText(requireContext(),
                        R.string.notification_detail_picker_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            LocalDate pickedDate = Instant.ofEpochMilli(value)
                    .atZone(zoneId)
                    .toLocalDate();
            selectedExpirationDate = pickedDate;
            updateExpirationLabel();
        });

        picker.show(getParentFragmentManager(), "expiration_picker");
    }

    private void updateExpirationLabel() {
        String label = getString(
                R.string.notification_detail_expiration_label,
                DISPLAY_DATE_FORMATTER.format(selectedExpirationDate));
        binding.textExpirationDate.setText(label);
    }

    private void manejarPermisoEspecialidad() {
        if (!TextUtils.isEmpty(obtenerEspecialidadSeleccionada())) {
            enviarAprobacion("POR_ESPECIALIDAD", obtenerEspecialidadSeleccionada());
        } else {
            mostrarDialogoEspecialidad();
        }
    }

    private void enviarAprobacion(String tipoPermiso, @Nullable String especialidadOverride) {
        if (solicitud == null || TextUtils.isEmpty(notificationServerId)) {
            Toast.makeText(requireContext(),
                    R.string.notification_detail_error_missing_data,
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(solicitud.getDocumentoId()) || TextUtils.isEmpty(solicitud.getTenantId())) {
            Toast.makeText(requireContext(),
                    R.string.notification_detail_error_missing_data,
                    Toast.LENGTH_LONG).show();
            return;
        }

        String cedula = SessionManager.getUserCI(requireContext());
        if (TextUtils.isEmpty(cedula) && solicitud.getCedulaPaciente() != null) {
            cedula = solicitud.getCedulaPaciente().trim();
        }
        if (TextUtils.isEmpty(cedula)) {
            Toast.makeText(requireContext(),
                    R.string.notification_detail_error_missing_data,
                    Toast.LENGTH_LONG).show();
            return;
        }

        if ("POR_ESPECIALIDAD".equals(tipoPermiso)) {
            String especialidad = especialidadOverride != null ? especialidadOverride : obtenerEspecialidadSeleccionada();
            if (TextUtils.isEmpty(especialidad)) {
                Toast.makeText(requireContext(),
                        R.string.notification_detail_especialidad_missing,
                        Toast.LENGTH_LONG).show();
                return;
            }
            especialidadSeleccionada = especialidad;
        }

        AprobarSolicitudRequest request = new AprobarSolicitudRequest();
        request.setNotificacionId(notificationServerId);
        request.setCedulaPaciente(cedula);
        request.setDocumentoId(solicitud.getDocumentoId());
        request.setTipoPermiso(tipoPermiso);
        request.setTenantId(solicitud.getTenantId());
        request.setFechaExpiracion(selectedExpirationDate
                .atTime(23, 59, 59)
                .format(API_DATE_FORMATTER));

        if ("PROFESIONAL_ESPECIFICO".equals(tipoPermiso)) {
            request.setProfesionalCi(solicitud.getProfesionalCi());
        } else if ("POR_ESPECIALIDAD".equals(tipoPermiso)) {
            request.setEspecialidad(especialidadSeleccionada.trim());
        }

        setLoading(true);

        apiService.aprobarSolicitud(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                   @NonNull Response<ApiResponse<Void>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(requireContext(),
                            R.string.notification_detail_success_aprobar,
                            Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(NotificationDetailFragment.this).popBackStack();
                } else {
                    Toast.makeText(requireContext(),
                            response.body() != null && response.body().getError() != null
                                    ? response.body().getError()
                                    : getString(R.string.notification_detail_error_general),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(),
                        getString(R.string.notification_detail_error_general),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarDialogoRechazo() {
        if (TextUtils.isEmpty(notificationServerId)) {
            Toast.makeText(requireContext(),
                    R.string.notification_detail_error_missing_data,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.notification_detail_deny)
                .setMessage(R.string.notification_detail_reject_confirmation)
                .setPositiveButton(R.string.notification_detail_deny, (dialog, which) -> rechazarSolicitud())
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    private void rechazarSolicitud() {
        setLoading(true);

        RechazarSolicitudRequest request = new RechazarSolicitudRequest(notificationServerId);
        apiService.rechazarSolicitud(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                   @NonNull Response<ApiResponse<Void>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(requireContext(),
                            R.string.notification_detail_success_rechazar,
                            Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(NotificationDetailFragment.this).popBackStack();
                } else {
                    Toast.makeText(requireContext(),
                            response.body() != null && response.body().getError() != null
                                    ? response.body().getError()
                                    : getString(R.string.notification_detail_error_general),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(),
                        getString(R.string.notification_detail_error_general),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (binding == null) {
            return;
        }
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonChangeDate.setEnabled(!loading);
        binding.btnPermitirProfesional.setEnabled(!loading);
        binding.btnPermitirEspecialidad.setEnabled(!loading);
        binding.btnPermitirClinica.setEnabled(!loading);
        binding.btnNoPermiso.setEnabled(!loading);
    }
    private String obtenerEspecialidadSeleccionada() {
        if (especialidadSeleccionada != null && !especialidadSeleccionada.trim().isEmpty()) {
            especialidadSeleccionada = especialidadSeleccionada.trim();
            return especialidadSeleccionada;
        }
        if (solicitud != null && !TextUtils.isEmpty(solicitud.getEspecialidad())) {
            especialidadSeleccionada = solicitud.getEspecialidad().trim();
        }
        return especialidadSeleccionada != null && !especialidadSeleccionada.isEmpty()
                ? especialidadSeleccionada
                : null;
    }

    private void mostrarDialogoEspecialidad() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint(R.string.notification_detail_especialidad_dialog_hint);
        if (!TextUtils.isEmpty(especialidadSeleccionada)) {
            input.setText(especialidadSeleccionada);
            input.setSelection(especialidadSeleccionada.length());
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.notification_detail_permission_specialty)
                .setMessage(R.string.notification_detail_especialidad_dialog_message)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText() != null ? input.getText().toString().trim() : "";
                    if (value.isEmpty()) {
                        Toast.makeText(requireContext(),
                                R.string.notification_detail_especialidad_missing,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    especialidadSeleccionada = value;
                    binding.textEspecialidad.setText(value);
                    enviarAprobacion("POR_ESPECIALIDAD", value);
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    private String formatProfesionalText() {
        if (solicitud == null) {
            return "";
        }
        String nombre = !TextUtils.isEmpty(solicitud.getProfesionalNombre())
                ? solicitud.getProfesionalNombre()
                : getString(R.string.notification_detail_default_profesional);
        String ci = solicitud.getProfesionalCi() > 0
                ? String.valueOf(solicitud.getProfesionalCi())
                : "-";
        return getString(R.string.notification_detail_profesional_format,
                nombre,
                ci);
    }
}
