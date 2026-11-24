package es.padi.interinos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Las alarmas se borran al reiniciar el móvil o cambiar la hora: se vuelven a programar. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Scheduler.programarDiaria(context);
    }
}
