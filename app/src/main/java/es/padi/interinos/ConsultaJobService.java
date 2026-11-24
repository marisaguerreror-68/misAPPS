package es.padi.interinos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

/** Ejecuta la consulta, guarda los datos, avisa con una notificación y programa la siguiente. */
public class ConsultaJobService extends JobService {
    static final String CANAL = "padi_consultas";

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            JSONObject r = Consulta.ejecutar(getApplicationContext());
            if (r.optBoolean("sinFechaNueva") || !r.optBoolean("ok")) {
                Scheduler.programarReintentoOSiguiente(getApplicationContext());
                if (!r.optBoolean("ok")) notificar(getApplicationContext(), "PADI: no se pudo consultar", r.optString("mensaje"));
            } else {
                Scheduler.programarDiaria(getApplicationContext());
                notificar(getApplicationContext(), "PADI · puestos ofertados", r.optString("mensaje"));
            }
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; // reintentar si el sistema lo interrumpe
    }

    static void crearCanal(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CANAL, "Consultas PADI", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Aviso diario con los puestos de interinidad ofertados");
        nm.createNotificationChannel(ch);
    }

    static void notificar(Context c, String titulo, String texto) {
        crearCanal(c);
        Intent abrir = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, abrir, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CANAL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new Notification.BigTextStyle().bigText(texto))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        try {
            c.getSystemService(NotificationManager.class).notify(1, n);
        } catch (SecurityException ignored) {
            // Sin permiso de notificaciones: los datos quedan guardados igualmente.
        }
    }
}
