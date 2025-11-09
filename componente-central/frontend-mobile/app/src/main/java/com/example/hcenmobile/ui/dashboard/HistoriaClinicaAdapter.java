package com.example.hcenmobile.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.model.HistoriaClinicaItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter encargado de renderizar la lista de documentos de la historia clínica.
 */
public class HistoriaClinicaAdapter extends RecyclerView.Adapter<HistoriaClinicaAdapter.HistoriaViewHolder> {

    private final List<HistoriaClinicaItem> items = new ArrayList<>();

    @NonNull
    @Override
    public HistoriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historia_documento, parent, false);
        return new HistoriaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoriaViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<HistoriaClinicaItem> nuevos) {
        items.clear();
        if (nuevos != null && !nuevos.isEmpty()) {
            items.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    static class HistoriaViewHolder extends RecyclerView.ViewHolder {

        private final TextView motivoText;
        private final TextView fechaText;
        private final TextView profesionalText;

        HistoriaViewHolder(@NonNull View itemView) {
            super(itemView);
            motivoText = itemView.findViewById(R.id.text_motivo);
            fechaText = itemView.findViewById(R.id.text_fecha);
            profesionalText = itemView.findViewById(R.id.text_profesional);
        }

        void bind(HistoriaClinicaItem item) {
            motivoText.setText(item.getMotivoConsulta());
            fechaText.setText(itemView.getContext().getString(
                    R.string.historia_fecha_label, item.getFecha()));
            profesionalText.setText(itemView.getContext().getString(
                    R.string.historia_profesional_label, item.getProfesional()));
        }
    }
}
