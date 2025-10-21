package com.example.hcenmobile.ui.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hcenmobile.R;
import com.example.hcenmobile.databinding.FragmentNotificationsBinding;
import com.example.hcenmobile.util.Constants;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private NotificationsViewModel viewModel;
    private NotificacionesAdapter adapter;

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
        adapter = new NotificacionesAdapter(notificacion -> {
            // Marcar como leída cuando se toca
            viewModel.marcarComoLeida(notificacion.getId());
            Toast.makeText(requireContext(),
                "Notificación de: " + notificacion.getRemitente(),
                Toast.LENGTH_SHORT).show();
        });

        binding.recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewNotifications.setAdapter(adapter);
        binding.recyclerViewNotifications.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        );
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}