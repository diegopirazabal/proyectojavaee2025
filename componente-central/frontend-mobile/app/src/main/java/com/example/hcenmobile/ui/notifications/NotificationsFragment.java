package com.example.hcenmobile.ui.notifications;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.model.Notificacion;
import com.example.hcenmobile.data.remote.ApiService;
import com.example.hcenmobile.data.remote.RetrofitClient;
import com.example.hcenmobile.data.remote.dto.ApiResponse;
import com.example.hcenmobile.data.remote.dto.RechazarSolicitudRequest;
import com.example.hcenmobile.data.remote.dto.SolicitudAccesoDTO;
import com.example.hcenmobile.databinding.FragmentNotificationsBinding;
import com.example.hcenmobile.ui.notifications.detail.NotificationDetailFragment;
import com.example.hcenmobile.util.Constants;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";

    private FragmentNotificationsBinding binding;
    private NotificationsViewModel viewModel;
    private NotificacionesAdapter adapter;
    private final Gson gson = new Gson();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();

        return root;
    }

    private void setupRecyclerView() {
        adapter = new NotificacionesAdapter(new NotificacionesAdapter.OnNotificacionListener() {
            @Override
            public void onNotificacionClick(Notificacion notificacion) {
                viewModel.marcarComoLeida(notificacion.getId());
                navigateToNotificationDetail(notificacion);
            }

            @Override
            public void onApproveClick(Notificacion notificacion) {
                viewModel.marcarComoLeida(notificacion.getId());
                navigateToNotificationDetail(notificacion);
            }

            @Override
            public void onRejectClick(Notificacion notificacion) {
                mostrarDialogRechazo(notificacion);
            }
        });

        binding.recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewNotifications.setAdapter(adapter);
        binding.recyclerViewNotifications.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        );
        attachSwipeToDelete(binding.recyclerViewNotifications);
    }

    private void navigateToNotificationDetail(Notificacion notificacion) {
        Bundle args = new Bundle();
        args.putString(NotificationDetailFragment.ARG_NOTIFICATION_SERVER_ID, notificacion.getNotificacionId());
        args.putLong(NotificationDetailFragment.ARG_NOTIFICATION_LOCAL_ID, notificacion.getId());
        args.putString(NotificationDetailFragment.ARG_NOTIFICATION_TYPE, notificacion.getTipo());
        args.putString(NotificationDetailFragment.ARG_NOTIFICATION_TITLE, notificacion.getTitulo());
        args.putString(NotificationDetailFragment.ARG_NOTIFICATION_MESSAGE, notificacion.getMensaje());
        args.putString(NotificationDetailFragment.ARG_NOTIFICATION_REMITENTE, notificacion.getRemitente());
        long fechaMillis = notificacion.getFechaHora() != null ? notificacion.getFechaHora().getTime() : System.currentTimeMillis();
        args.putLong(NotificationDetailFragment.ARG_NOTIFICATION_FECHA, fechaMillis);
        if (!TextUtils.isEmpty(notificacion.getDatosAdicionales())) {
            args.putString(NotificationDetailFragment.ARG_NOTIFICATION_DATOS, notificacion.getDatosAdicionales());
        }

        NavHostFragment.findNavController(this)
                .navigate(R.id.action_navigation_notifications_to_notificationDetailFragment, args);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            sincronizarNotificaciones();
        });
    }

    private void observeViewModel() {
        // Observar lista de notificaciones
        viewModel.getNotificaciones().observe(getViewLifecycleOwner(), notificaciones -> {
            if (notificaciones != null && !notificaciones.isEmpty()) {
                adapter.setNotificaciones(notificaciones);
                binding.textEmpty.setVisibility(View.GONE);
                binding.recyclerViewNotifications.setVisibility(View.VISIBLE);
            } else {
                binding.textEmpty.setVisibility(View.VISIBLE);
                binding.recyclerViewNotifications.setVisibility(View.GONE);
            }
        });

        // Observar contador de no leídas
        viewModel.getCountNoLeidas().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                requireActivity().setTitle("Notificaciones (" + count + ")");
            } else {
                requireActivity().setTitle("Notificaciones");
            }
        });

        // Observar estado de refresh
        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
            binding.swipeRefreshLayout.setRefreshing(isRefreshing != null && isRefreshing);
        });

        // Observar mensajes de error
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sincronizarNotificaciones() {
        SharedPreferences prefs = requireContext().getSharedPreferences(
                Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.PREF_USER_ID, "demo_user");
        viewModel.sincronizarNotificaciones(userId);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_notifications, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_mark_all_read) {
            viewModel.marcarTodasComoLeidas();
            Toast.makeText(requireContext(), "Todas las notificaciones marcadas como leídas",
                    Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            sincronizarNotificaciones();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Muestra un dialog de confirmación para rechazar una solicitud de acceso
     */
    private void mostrarDialogRechazo(Notificacion notificacion) {
        SolicitudAccesoDTO solicitud = parseSolicitud(notificacion);
        String profesional = solicitud != null && !TextUtils.isEmpty(solicitud.getProfesionalNombre())
                ? solicitud.getProfesionalNombre()
                : "el profesional";

        new AlertDialog.Builder(requireContext())
                .setTitle("Rechazar Solicitud")
                .setMessage("¿Está seguro que desea rechazar la solicitud de acceso de " +
                        profesional + "?")
                .setPositiveButton("Rechazar", (dialog, which) -> {
                    rechazarSolicitud(notificacion);
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Rechaza una solicitud de acceso llamando al API
     */
    private void rechazarSolicitud(Notificacion notificacion) {
        String serverId = notificacion.getNotificacionId();
        if (TextUtils.isEmpty(serverId)) {
            Toast.makeText(requireContext(),
                    "No se pudo identificar la solicitud para rechazarla",
                    Toast.LENGTH_LONG).show();
            return;
        }

        RechazarSolicitudRequest request = new RechazarSolicitudRequest(serverId);

        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<ApiResponse<Void>> call = apiService.rechazarSolicitud(request);

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                   @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(requireContext(),
                            "Solicitud rechazada",
                            Toast.LENGTH_SHORT).show();
                    // Recargar notificaciones
                    sincronizarNotificaciones();
                } else {
                    String errorMsg = "Error al rechazar solicitud";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMsg = response.body().getError();
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private SolicitudAccesoDTO parseSolicitud(Notificacion notificacion) {
        if (notificacion == null || TextUtils.isEmpty(notificacion.getDatosAdicionales())) {
            return null;
        }
        try {
            return gson.fromJson(notificacion.getDatosAdicionales(), SolicitudAccesoDTO.class);
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear datos adicionales de la notificación", e);
            return null;
        }
    }

    private void attachSwipeToDelete(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getBindingAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) {
                            return;
                        }
                        Notificacion notificacion = adapter.getNotificacionAt(position);
                        if (notificacion != null) {
                            adapter.removeNotificacionAt(position);
                            viewModel.eliminarNotificacion(notificacion.getId());
                            Toast.makeText(requireContext(),
                                    "Notificación eliminada",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            adapter.notifyItemChanged(position);
                        }
                    }
                };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }
}
