package com.example.hcenmobile.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.model.Notificacion;
import com.example.hcenmobile.data.remote.dto.SolicitudAccesoDTO;
import com.example.hcenmobile.util.Constants;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para mostrar la lista de notificaciones en un RecyclerView
 * Soporta múltiples tipos de layouts (normal y solicitud de acceso)
 */
public class NotificacionesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_SOLICITUD = 1;

    private List<Notificacion> notificaciones;
    private final OnNotificacionListener listener;
    private final SimpleDateFormat dateFormat;
    private final Gson gson;

    public interface OnNotificacionListener {
        void onNotificacionClick(Notificacion notificacion);
        void onApproveClick(SolicitudAccesoDTO solicitud);
        void onRejectClick(SolicitudAccesoDTO solicitud);
    }

    public NotificacionesAdapter(OnNotificacionListener listener) {
        this.notificaciones = new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        this.gson = new Gson();
    }

    @Override
    public int getItemViewType(int position) {
        Notificacion notif = notificaciones.get(position);
        if (Constants.NOTIF_TYPE_ACCESS_REQUEST.equals(notif.getTipo())) {
            return VIEW_TYPE_SOLICITUD;
        }
        return VIEW_TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SOLICITUD) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notificacion_solicitud, parent, false);
            return new SolicitudViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notificacion, parent, false);
            return new NotificacionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Notificacion notificacion = notificaciones.get(position);
        if (holder instanceof SolicitudViewHolder) {
            ((SolicitudViewHolder) holder).bind(notificacion, listener);
        } else if (holder instanceof NotificacionViewHolder) {
            ((NotificacionViewHolder) holder).bind(notificacion, listener);
        }
    }

    @Override
    public int getItemCount() {
        return notificaciones.size();
    }

    public void setNotificaciones(List<Notificacion> notificaciones) {
        this.notificaciones = notificaciones;
        notifyDataSetChanged();
    }

    // ============ VIEW HOLDERS ============

    /**
     * ViewHolder para notificaciones normales
     */
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

        public void bind(Notificacion notificacion, OnNotificacionListener listener) {
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
            } else {
                indicadorNoLeida.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificacionClick(notificacion);
                }
            });
        }
    }

    /**
     * ViewHolder para solicitudes de acceso a documentos
     */
    class SolicitudViewHolder extends RecyclerView.ViewHolder {

        private final View indicator;
        private final TextView profesionalNombre;
        private final TextView clinicaNombre;
        private final TextView documentoInfo;
        private final TextView motivoConsulta;
        private final TextView fechaSolicitud;
        private final TextView estadoBadge;
        private final View actionButtons;
        private final Button btnApprove;
        private final Button btnReject;

        public SolicitudViewHolder(@NonNull View itemView) {
            super(itemView);
            indicator = itemView.findViewById(R.id.indicator);
            profesionalNombre = itemView.findViewById(R.id.profesional_nombre);
            clinicaNombre = itemView.findViewById(R.id.clinica_nombre);
            documentoInfo = itemView.findViewById(R.id.documento_info);
            motivoConsulta = itemView.findViewById(R.id.motivo_consulta);
            fechaSolicitud = itemView.findViewById(R.id.fecha_solicitud);
            estadoBadge = itemView.findViewById(R.id.estado_badge);
            actionButtons = itemView.findViewById(R.id.action_buttons);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }

        public void bind(Notificacion notificacion, OnNotificacionListener listener) {
            // Parsear datos adicionales desde JSON
            SolicitudAccesoDTO solicitud = null;
            if (notificacion.getDatosAdicionales() != null && !notificacion.getDatosAdicionales().isEmpty()) {
                try {
                    solicitud = gson.fromJson(notificacion.getDatosAdicionales(), SolicitudAccesoDTO.class);
                    // Copiar ID de la notificación al DTO
                    solicitud.setId(String.valueOf(notificacion.getId()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (solicitud != null) {
                // Configurar vistas con datos de la solicitud
                profesionalNombre.setText(solicitud.getProfesionalNombre());
                clinicaNombre.setText(solicitud.getNombreClinica());
                documentoInfo.setText("📄 Documento del " + solicitud.getFechaDocumento());
                motivoConsulta.setText(solicitud.getMotivoConsulta());

                // Calcular tiempo transcurrido
                fechaSolicitud.setText(getTimeAgo(notificacion.getFechaHora()));

                // Configurar estado y botones según el estado de la notificación
                // Asumiendo que Notificacion no tiene campo estado, usamos la visibilidad de botones
                // En producción, deberías agregar el campo estado a la entidad Notificacion

                // Por ahora, si no está leída, consideramos que está pendiente
                if (!notificacion.isLeida()) {
                    // Pendiente
                    indicator.setBackgroundResource(android.R.color.holo_orange_light);
                    estadoBadge.setVisibility(View.GONE);
                    actionButtons.setVisibility(View.VISIBLE);

                    SolicitudAccesoDTO finalSolicitud = solicitud;
                    btnApprove.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onApproveClick(finalSolicitud);
                        }
                    });

                    btnReject.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onRejectClick(finalSolicitud);
                        }
                    });
                } else {
                    // Ya procesada (aprobada o rechazada)
                    actionButtons.setVisibility(View.GONE);
                    estadoBadge.setVisibility(View.VISIBLE);
                    estadoBadge.setText("✓ Procesada");
                    estadoBadge.setBackgroundResource(android.R.color.darker_gray);
                    indicator.setBackgroundResource(android.R.color.darker_gray);
                }
            } else {
                // Fallback si no hay datos
                profesionalNombre.setText("Solicitud de acceso");
                clinicaNombre.setText(notificacion.getMensaje());
                actionButtons.setVisibility(View.GONE);
            }
        }

        private String getTimeAgo(Date date) {
            long diff = new Date().getTime() - date.getTime();
            long minutes = diff / (60 * 1000);
            long hours = diff / (60 * 60 * 1000);
            long days = diff / (24 * 60 * 60 * 1000);

            if (minutes < 60) {
                return "Hace " + minutes + " minutos";
            } else if (hours < 24) {
                return "Hace " + hours + " horas";
            } else if (days == 1) {
                return "Ayer";
            } else if (days < 7) {
                return "Hace " + days + " días";
            } else {
                return dateFormat.format(date);
            }
        }
    }
}
