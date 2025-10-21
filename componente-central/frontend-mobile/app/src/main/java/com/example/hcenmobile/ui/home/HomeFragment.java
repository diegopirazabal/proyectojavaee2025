package com.example.hcenmobile.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.repository.NotificacionRepository;
import com.example.hcenmobile.databinding.FragmentHomeBinding;
import com.example.hcenmobile.util.Constants;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NotificacionRepository repository;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        repository = NotificacionRepository.getInstance(requireContext());

        setupUI();
        observeData();
        setupClickListeners();

        return root;
    }

    private void setupUI() {
        // Mostrar C.I. del usuario
        SharedPreferences prefs = requireContext().getSharedPreferences(
                Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String ci = prefs.getString(Constants.PREF_USER_CI, "");
        binding.textUserCi.setText("C.I.: " + ci);
    }

    private void observeData() {
        // Observar total de notificaciones
        repository.getAllNotificaciones().observe(getViewLifecycleOwner(), notificaciones -> {
            if (notificaciones != null) {
                binding.textNotifTotalCount.setText(String.valueOf(notificaciones.size()));
            }
        });

        // Observar notificaciones no leídas
        repository.getCountNotificacionesNoLeidas().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                binding.textNotifUnreadCount.setText(String.valueOf(count));
            }
        });
    }

    private void setupClickListeners() {
        // Click en card de notificaciones -> ir a tab de notificaciones
        binding.cardNotificaciones.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_notifications));

        // Click en card de historia clínica -> ir a tab de historia
        binding.cardHistoria.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_dashboard));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}