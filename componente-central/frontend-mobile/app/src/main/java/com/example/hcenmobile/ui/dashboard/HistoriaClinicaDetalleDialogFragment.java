package com.example.hcenmobile.ui.dashboard;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.hcenmobile.R;
import com.example.hcenmobile.data.model.HistoriaClinicaItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Muestra los detalles completos de un documento clínico dentro de la Historia Clínica.
 */
public class HistoriaClinicaDetalleDialogFragment extends DialogFragment {

    private static final String ARG_DOCUMENTO_ID = "arg_documento_id";
    private static final String ARG_TENANT = "arg_tenant";
    private static final String ARG_CEDULA = "arg_cedula";
    private static final String ARG_MOTIVO = "arg_motivo";
    private static final String ARG_FECHA_RESUMEN = "arg_fecha_resumen";
    private static final String ARG_FECHA_REGISTRO = "arg_fecha_registro";
    private static final String ARG_PROFESIONAL = "arg_profesional";
    private static final String ARG_CLINICA = "arg_clinica";

    public static HistoriaClinicaDetalleDialogFragment newInstance(HistoriaClinicaItem item) {
        Bundle args = new Bundle();
        args.putString(ARG_DOCUMENTO_ID, item.getDocumentoId());
        args.putString(ARG_TENANT, item.getTenantId());
        args.putString(ARG_CEDULA, item.getUsuarioCedula());
        args.putString(ARG_MOTIVO, item.getMotivoConsulta());
        args.putString(ARG_FECHA_RESUMEN, item.getFecha());
        args.putString(ARG_FECHA_REGISTRO, item.getFechaRegistro());
        args.putString(ARG_PROFESIONAL, item.getProfesional());
        args.putString(ARG_CLINICA, item.getNombreClinica());

        HistoriaClinicaDetalleDialogFragment fragment = new HistoriaClinicaDetalleDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_historia_detalle, null);
        populateView(view);

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.historia_detalle_title)
                .setView(view)
                .setPositiveButton(R.string.button_close, (dialog, which) -> dialog.dismiss())
                .create();
    }

    private void populateView(View view) {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        setText(view, R.id.textDetalleMotivo, args.getString(ARG_MOTIVO));
        setText(view, R.id.textDetalleProfesional, args.getString(ARG_PROFESIONAL));
        setText(view, R.id.textDetalleFechaPrincipal, args.getString(ARG_FECHA_RESUMEN));
        setText(view, R.id.textDetalleFechaRegistro, args.getString(ARG_FECHA_REGISTRO));
        setText(view, R.id.textDetalleDocumentoId, args.getString(ARG_MOTIVO));
        setText(view, R.id.textDetalleTenant, args.getString(ARG_CLINICA));
        setText(view, R.id.textDetalleCedula, args.getString(ARG_CEDULA));
    }

    private void setText(View view, int viewId, @Nullable String value) {
        TextView textView = view.findViewById(viewId);
        if (textView == null) {
            return;
        }
        if (TextUtils.isEmpty(value)) {
            textView.setText(R.string.historia_detalle_no_disponible);
        } else {
            textView.setText(value);
        }
    }
}
