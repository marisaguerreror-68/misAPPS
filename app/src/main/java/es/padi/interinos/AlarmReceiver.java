package es.padi.interinos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Salta a la hora programada y delega la consulta en un trabajo con conexión de red. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Scheduler.lanzarTrabajo(context);
        // Por si el trabajo no llega a completarse, la siguiente alarma diaria queda ya programada;
        // el trabajo la sustituye por un reintento si hace falta.
        Scheduler.programarDiaria(context);
    }
}
