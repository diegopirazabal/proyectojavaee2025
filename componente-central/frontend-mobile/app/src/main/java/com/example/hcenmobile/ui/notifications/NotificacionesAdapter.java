package com.example.hcenmobile.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.model.Notificacion;
import com.example.hcenmobile.util.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para mostrar la lista de notificaciones en un RecyclerView
 */
public class NotificacionesAdapter extends RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder> {

    private List<Notificacion> notificaciones;
    private final OnNotificacionClickListener listener;
    private final SimpleDateFormat dateFormat;

    public interface OnNotificacionClickListener {
        void onNotificacionClick(Notificacion notificacion);
    }

    public NotificacionesAdapter(OnNotificacionClickListener listener) {
        this.notificaciones = new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public NotificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notificacion, parent, false);
        return new NotificacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacionViewHolder holder, int position) {
        Notificacion notificacion = notificaciones.get(position);
        holder.bind(notificacion, listener);
    }

    @Override
    public int getItemCount() {
        return notificaciones.size();
    }

    public void setNotificaciones(List<Notificacion> notificaciones) {
        this.notificaciones = notificaciones;
        notifyDataSetChanged();
    }

    class NotificacionViewHolder extends RecyclerView.ViewHolder {

        private final TextView textTitulo;
        private final TextView textMensaje;
        private final TextView textFecha;
        private final TextView textRemitente;
        private final View indicadorNoLeida;

        public NotificacionViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitulo = itemView.findViewById(R.id.text_notificacion_titulo);
            textMensaje = itemView.findViewById(R.id.text_notificacion_mensaje);
            textFecha = itemView.findViewById(R.id.text_notificacion_fecha);
            textRemitente = itemView.findViewById(R.id.text_notificacion_remitente);
            indicadorNoLeida = itemView.findViewById(R.id.view_indicador_no_leida);
        }

        public void bind(Notificacion notificacion, OnNotificacionClickListener listener) {
            textTitulo.setText(notificacion.getTitulo());
            textMensaje.setText(notificacion.getMensaje());
            textFecha.setText(dateFormat.format(notificacion.getFechaHora()));

            // Mostrar remitente si está disponible
            if (notificacion.getRemitente() != null && !notificacion.getRemitente().isEmpty()) {
                textRemitente.setText(notificacion.getRemitente());
                textRemitente.setVisibility(View.VISIBLE);
            } else {
                textRemitente.setVisibility(View.GONE);
            }

            // Indicador visual para notificaciones no leídas
            if (!notificacion.isLeida()) {
                indicadorNoLeida.setVisibility(View.VISIBLE);
                itemView.setBackgroundResource(R.color.notificacion_no_leida_bg);
            } else {
                indicadorNoLeida.setVisibility(View.GONE);
                itemView.setBackgroundResource(android.R.color.transparent);
            }

            // Icono según tipo de notificación
            int tipoIcono = getTipoIcono(notificacion.getTipo());
            // TODO: Agregar un ImageView en el layout para mostrar el icono

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificacionClick(notificacion);
                }
            });
        }

        private int getTipoIcono(String tipo) {
            if (tipo == null) return R.drawable.ic_launcher_foreground;

            switch (tipo) {
                case Constants.NOTIF_TYPE_ACCESS_REQUEST:
                    return R.drawable.ic_launcher_foreground; // TODO: Agregar icono específico
                case Constants.NOTIF_TYPE_ACCESS_GRANTED:
                    return R.drawable.ic_launcher_foreground; // TODO: Agregar icono específico
                case Constants.NOTIF_TYPE_HISTORY_ACCESSED:
                    return R.drawable.ic_launcher_foreground; // TODO: Agregar icono específico
                default:
                    return R.drawable.ic_launcher_foreground;
            }
        }
    }
}
