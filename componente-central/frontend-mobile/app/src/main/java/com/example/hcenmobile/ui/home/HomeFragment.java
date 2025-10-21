package com.example.hcenmobile.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.hcenmobile.databinding.FragmentHomeBinding;
import com.example.hcenmobile.util.SessionManager;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupUI();

        return root;
    }

    private void setupUI() {
        // Mostrar C.I. del usuario usando SessionManager
        String ci = SessionManager.getUserCI(requireContext());
        binding.textUserCi.setText("C.I.: " + ci);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}