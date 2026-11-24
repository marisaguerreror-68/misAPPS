package es.padi.interinos;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/**
 * Programa la consulta de lunes a viernes a la hora elegida (15:00 por defecto).
 * Si a esa hora PADI aún no ha publicado la asignación del día siguiente,
 * reintenta cada 30 minutos hasta las 20:00.
 */
public final class Scheduler {
    private static final int REQ_ALARMA = 1;
    private static final int JOB_ID = 42;
    private static final int HORA_LIMITE_REINTENTOS = 20;

    private Scheduler() {}

    /** Próxima ejecución diaria (L-V a la hora configurada), en milisegundos. */
    public static long proximaDiaria(Context c) {
        int h = Store.prefs(c).getInt("hora", 15);
        int m = Store.prefs(c).getInt("minuto", 0);
        Calendar now = Calendar.getInstance();
        Calendar t = (Calendar) now.clone();
        t.set(Calendar.HOUR_OF_DAY, h);
        t.set(Calendar.MINUTE, m);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (!t.after(now)) t.add(Calendar.DAY_OF_MONTH, 1);
        while (t.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || t.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            t.add(Calendar.DAY_OF_MONTH, 1);
        }
        return t.getTimeInMillis();
    }

    public static void programarDiaria(Context c) {
        programarEn(c, proximaDiaria(c));
    }

    /** Tras un intento sin fecha nueva: reintento en 30 min si aún es pronto; si no, al día siguiente. */
    public static void programarReintentoOSiguiente(Context c) {
        Calendar t = Calendar.getInstance();
        t.add(Calendar.MINUTE, 30);
        if (t.get(Calendar.HOUR_OF_DAY) < HORA_LIMITE_REINTENTOS) programarEn(c, t.getTimeInMillis());
        else programarDiaria(c);
    }

    private static void programarEn(Context c, long when) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ_ALARMA,
                new Intent(c, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean exacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        if (exacta) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        Store.prefs(c).edit().putLong("proxima", when).putBoolean("exacta", exacta).apply();
    }

    /** Lanza la consulta en segundo plano en cuanto haya conexión. */
    public static void lanzarTrabajo(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(c, ConsultaJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(10 * 60 * 1000L)
                .build();
        js.schedule(job);
    }
}
