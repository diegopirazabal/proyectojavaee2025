package com.example.hcenmobile.ui.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.model.HistoriaClinicaItem;
import com.example.hcenmobile.databinding.FragmentDashboardBinding;
import com.example.hcenmobile.util.SessionManager;

import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private HistoriaClinicaAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        setupViewModel();
        setupRecyclerView();
        setupObservers();
        setupListeners();
        cargarHistoria();
        return binding.getRoot();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this,
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
                .get(DashboardViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new HistoriaClinicaAdapter();
        binding.recyclerHistoria.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistoria.setAdapter(adapter);
        binding.swipeRefresh.setColorSchemeResources(R.color.blue_500, R.color.blue_700);
    }

    private void setupObservers() {
        viewModel.getDocumentos().observe(getViewLifecycleOwner(), this::renderDocumentos);
        viewModel.isLoading().observe(getViewLifecycleOwner(), this::renderLoading);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::renderError);
    }

    private void setupListeners() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refrescar());
        binding.buttonRetry.setOnClickListener(v -> cargarHistoria());
    }

    private void cargarHistoria() {
        String cedula = SessionManager.getUserCI(requireContext());
        if (TextUtils.isEmpty(cedula)) {
            renderError(getString(R.string.historia_error_sin_documentos));
            return;
        }
        viewModel.cargarHistoria(cedula);
    }

    private void renderDocumentos(List<HistoriaClinicaItem> items) {
        adapter.updateItems(items);
        boolean hasItems = items != null && !items.isEmpty();
        binding.recyclerHistoria.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        binding.layoutEmpty.setVisibility(!hasItems ? View.VISIBLE : View.GONE);
    }

    private void renderLoading(Boolean isLoading) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        boolean hasData = adapter != null && adapter.getItemCount() > 0;

        if (hasData) {
            binding.swipeRefresh.setRefreshing(loading);
        } else {
            binding.swipeRefresh.setRefreshing(false);
        }

        binding.progressLoading.setVisibility(loading && !hasData ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.layoutError.setVisibility(View.GONE);
            if (!hasData) {
                binding.layoutEmpty.setVisibility(View.GONE);
            }
        }
    }

    private void renderError(@Nullable String message) {
        boolean hasError = message != null && !message.isEmpty();
        boolean hasData = adapter != null && adapter.getItemCount() > 0;
        boolean showErrorLayout = hasError && !hasData;

        binding.layoutError.setVisibility(showErrorLayout ? View.VISIBLE : View.GONE);
        binding.swipeRefresh.setVisibility(showErrorLayout ? View.GONE : View.VISIBLE);

        if (showErrorLayout) {
            binding.textErrorMessage.setText(message);
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.swipeRefresh.setRefreshing(false);
        } else if (binding.swipeRefresh.getVisibility() == View.GONE) {
            binding.swipeRefresh.setVisibility(View.VISIBLE);
        }

        if (hasError && hasData && isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
