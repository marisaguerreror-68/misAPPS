package es.padi.interinos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Todo se guarda en el almacenamiento interno de la app, en el propio móvil. */
public final class Store {
    public static final String[][] ESPECIALIDADES = {
            {"061", "ECONOMIA"},
            {"101", "ADMINISTRACION DE EMPRESAS"},
            {"105", "FORMACION Y ORIENTACION LABORAL"},
            {"110", "ORGANIZACION Y GESTION COMERCIAL"},
            {"221", "PROCESOS COMERCIALES"},
            {"222", "PROCESOS DE GESTION ADMINISTRATIVA"},
    };
    public static final String CUERPO = "0590";

    private Store() {}

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "consultas");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("ajustes", Context.MODE_PRIVATE);
    }

    /** Ajustes: especialidades activas, hora y minuto de la consulta. */
    public static JSONObject ajustes(Context c) {
        SharedPreferences p = prefs(c);
        JSONObject o = new JSONObject();
        try {
            JSONArray esps = new JSONArray(p.getString("esps", defaultEsps().toString()));
            o.put("esps", esps);
            o.put("hora", p.getInt("hora", 15));
            o.put("minuto", p.getInt("minuto", 0));
            o.put("catalogo", catalogo());
        } catch (Exception ignored) {}
        return o;
    }

    public static void guardarAjustes(Context c, JSONObject o) {
        SharedPreferences.Editor e = prefs(c).edit();
        JSONArray esps = o.optJSONArray("esps");
        if (esps != null) e.putString("esps", esps.toString());
        if (o.has("hora")) e.putInt("hora", Math.max(0, Math.min(23, o.optInt("hora", 15))));
        if (o.has("minuto")) e.putInt("minuto", Math.max(0, Math.min(59, o.optInt("minuto", 0))));
        e.apply();
    }

    private static JSONArray defaultEsps() {
        JSONArray a = new JSONArray();
        for (String[] e : ESPECIALIDADES) a.put(e[0]);
        return a;
    }

    private static JSONArray catalogo() throws Exception {
        JSONArray a = new JSONArray();
        for (String[] e : ESPECIALIDADES) {
            JSONObject o = new JSONObject();
            o.put("cod", e[0]);
            o.put("nombre", e[1]);
            a.put(o);
        }
        return a;
    }

    public static String nombreEsp(String cod) {
        for (String[] e : ESPECIALIDADES) if (e[0].equals(cod)) return e[1];
        return cod;
    }

    public static synchronized void guardar(Context c, String fecha, JSONObject consulta) throws Exception {
        File tmp = new File(dir(c), fecha + ".json.tmp");
        try (FileOutputStream fo = new FileOutputStream(tmp)) {
            fo.write(consulta.toString().getBytes(StandardCharsets.UTF_8));
        }
        File dst = new File(dir(c), fecha + ".json");
        if (!tmp.renameTo(dst)) throw new Exception("No se pudo guardar la consulta");
    }

    public static synchronized JSONObject leer(Context c, String fecha) {
        File f = new File(dir(c), fecha + ".json");
        if (!f.exists()) return null;
        try (FileInputStream fi = new FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int off = 0, n;
            while (off < b.length && (n = fi.read(b, off, b.length - off)) > 0) off += n;
            return new JSONObject(new String(b, 0, off, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    /** Fechas guardadas, de la más reciente a la más antigua. */
    public static synchronized JSONArray fechasGuardadas(Context c) {
        String[] names = dir(c).list((d, n) -> n.endsWith(".json"));
        if (names == null) names = new String[0];
        Arrays.sort(names);
        JSONArray a = new JSONArray();
        for (int i = names.length - 1; i >= 0; i--) a.put(names[i].replace(".json", ""));
        return a;
    }

    public static synchronized void borrar(Context c, String fecha) {
        new File(dir(c), fecha + ".json").delete();
    }
}
