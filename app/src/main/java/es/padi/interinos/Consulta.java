package es.padi.interinos;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Hace la consulta de PADI para el primer día con asignación posterior a hoy,
 * en convocatoria ordinaria y extraordinaria, todas las DAT, cuerpo 0590 y las
 * especialidades activas. Guarda el resultado en el móvil.
 */
public final class Consulta {
    private static final String[] TIPOS = {"O", "E"};

    private Consulta() {}

    public static synchronized JSONObject ejecutar(Context ctx) {
        JSONObject res = new JSONObject();
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        SimpleDateFormat stamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
        String hoy = iso.format(new Date());
        try {
            // 1. Fechas publicadas este mes y el siguiente, para cada tipo de convocatoria.
            Calendar cal = Calendar.getInstance();
            int y1 = cal.get(Calendar.YEAR), m1 = cal.get(Calendar.MONTH) + 1;
            cal.add(Calendar.MONTH, 1);
            int y2 = cal.get(Calendar.YEAR), m2 = cal.get(Calendar.MONTH) + 1;

            String objetivo = null;
            JSONObject fechasPorTipo = new JSONObject();
            for (String t : TIPOS) {
                List<String> fs = new ArrayList<>();
                fs.addAll(fechasSeguras(t, y1, m1));
                fs.addAll(fechasSeguras(t, y2, m2));
                fechasPorTipo.put(t, new JSONArray(fs));
                for (String f : fs) {
                    if (f.compareTo(hoy) > 0 && (objetivo == null || f.compareTo(objetivo) < 0)) objetivo = f;
                }
            }

            if (objetivo == null) {
                res.put("ok", true);
                res.put("sinFechaNueva", true);
                res.put("mensaje", "PADI aún no ha publicado la asignación del próximo día lectivo.");
                registrar(ctx, res, stamp);
                return res;
            }

            // 2. Puestos de cada especialidad activa en cada convocatoria donde exista esa fecha.
            JSONArray activas = Store.ajustes(ctx).optJSONArray("esps");
            Set<String> act = new HashSet<>();
            if (activas != null) for (int i = 0; i < activas.length(); i++) act.add(activas.optString(i));

            JSONArray puestos = new JSONArray();
            JSONObject espsPorTipo = new JSONObject();
            for (String t : TIPOS) {
                JSONArray fs = fechasPorTipo.getJSONArray(t);
                boolean publicada = false;
                for (int i = 0; i < fs.length(); i++) if (objetivo.equals(fs.getString(i))) publicada = true;
                JSONArray ofertadas = new JSONArray();
                if (publicada) {
                    List<String> disponibles = PadiApi.especialidades(Store.CUERPO, objetivo, t);
                    for (String esp : disponibles) {
                        if (!act.contains(esp)) continue;
                        ofertadas.put(esp);
                        JSONArray nec = PadiApi.necesidades(objetivo, Store.CUERPO, esp, t);
                        for (int i = 0; i < nec.length(); i++) puestos.put(limpiar(nec.getJSONObject(i), t, esp));
                    }
                }
                JSONObject info = new JSONObject();
                info.put("publicada", publicada);
                info.put("especialidades", ofertadas);
                espsPorTipo.put(t, info);
            }

            // 3. Marcar puestos nuevos respecto a la última consulta guardada anterior.
            Set<String> previos = new HashSet<>();
            JSONArray guardadas = Store.fechasGuardadas(ctx);
            for (int i = 0; i < guardadas.length(); i++) {
                String f = guardadas.getString(i);
                if (f.compareTo(objetivo) < 0) {
                    JSONObject prev = Store.leer(ctx, f);
                    if (prev != null) {
                        JSONArray pp = prev.optJSONArray("puestos");
                        if (pp != null) for (int j = 0; j < pp.length(); j++)
                            previos.add(pp.getJSONObject(j).optString("id"));
                    }
                    break;
                }
            }
            int nuevos = 0;
            for (int i = 0; i < puestos.length(); i++) {
                JSONObject p = puestos.getJSONObject(i);
                boolean nuevo = !previos.contains(p.optString("id"));
                p.put("nuevo", nuevo);
                if (nuevo) nuevos++;
            }

            JSONObject consulta = new JSONObject();
            consulta.put("fecha", objetivo);
            consulta.put("consultadoEn", stamp.format(new Date()));
            consulta.put("cuerpo", Store.CUERPO);
            consulta.put("convocatorias", espsPorTipo);
            consulta.put("puestos", puestos);
            Store.guardar(ctx, objetivo, consulta);

            res.put("ok", true);
            res.put("fecha", objetivo);
            res.put("total", puestos.length());
            res.put("nuevos", nuevos);
            res.put("mensaje", "Asignación del " + corta(objetivo) + ": " + puestos.length()
                    + " puestos" + (nuevos > 0 && !previos.isEmpty() ? " (" + nuevos + " nuevos)" : ""));
        } catch (Exception e) {
            try {
                res.put("ok", false);
                res.put("mensaje", "Error al consultar PADI: " + e.getMessage());
            } catch (Exception ignored) {}
        }
        registrar(ctx, res, stamp);
        return res;
    }

    private static List<String> fechasSeguras(String tipo, int y, int m) {
        try {
            return PadiApi.fechas(tipo, y, m);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static JSONObject limpiar(JSONObject o, String tipo, String esp) throws Exception {
        JSONObject p = new JSONObject();
        p.put("convocatoria", tipo);
        p.put("esp", esp);
        p.put("espNombre", Store.nombreEsp(esp));
        p.put("id", String.valueOf(o.opt("idnsustituciones")));
        p.put("codCentro", o.optString("ccentro"));
        p.put("centro", o.optString("centro"));
        p.put("localidad", o.optString("localidad"));
        p.put("distrito", o.optString("distrito"));
        p.put("dat", o.optString("dat"));
        p.put("tipo", o.optString("tpvacante"));
        p.put("tipoPuesto", o.optString("tipopuesto"));
        p.put("parcialidad", o.optString("dtpvacante"));
        p.put("vacSust", o.optString("dsustitucion"));
        p.put("causa", o.optString("causa"));
        p.put("fechaFin", o.optString("fhasta"));
        p.put("observaciones", o.optString("observaciones"));
        p.put("centrosCompartidos", o.optString("centrosCompartidos"));
        return p;
    }

    private static String corta(String iso) {
        String[] p = iso.split("-");
        return p.length == 3 ? p[2] + "/" + p[1] : iso;
    }

    private static void registrar(Context ctx, JSONObject res, SimpleDateFormat stamp) {
        Store.prefs(ctx).edit()
                .putString("ultimoIntento", stamp.format(new Date()))
                .putString("ultimoEstado", res.optString("mensaje"))
                .apply();
    }
}
