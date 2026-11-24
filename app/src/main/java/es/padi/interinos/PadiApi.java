package es.padi.interinos;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Llamadas a la API pública que usa la web de PADI (Puestos de interinidad ofertados). */
public final class PadiApi {
    private static final String BASE =
            "https://gestiona7.madrid.org/nsus_rest_ares/v1/ActosPublicosSustituciones/";

    private PadiApi() {}

    private static JSONArray post(String metodo, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + metodo).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        c.setRequestProperty("Accept", "application/json, text/plain, */*");
        c.setRequestProperty("Origin", "https://gestiona.comunidad.madrid");
        c.setRequestProperty("Referer", "https://gestiona.comunidad.madrid/padi/");
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        if (code != 200) throw new Exception("PADI respondió " + code + " en " + metodo);
        try (InputStream is = c.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            String txt = bo.toString("UTF-8").trim();
            if (txt.isEmpty()) return new JSONArray();
            if (!txt.startsWith("[")) throw new Exception("Respuesta inesperada de PADI (¿sala de espera?)");
            return new JSONArray(txt);
        } finally {
            c.disconnect();
        }
    }

    /** Fechas de asignación (yyyy-MM-dd) publicadas para un mes. tipo: "O" ordinaria, "E" extraordinaria. */
    public static List<String> fechas(String tipo, int year, int month) throws Exception {
        JSONObject b = new JSONObject();
        b.put("year", String.valueOf(year));
        b.put("monthId", String.format("%02d", month));
        b.put("itperfilacto", tipo);
        JSONArray a = post("getFiltroFechaNec", b);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            String f = a.getJSONObject(i).optString("fcpublico", "");
            if (!f.isEmpty()) out.add(f);
        }
        return out;
    }

    /** Códigos de especialidad (p. ej. "061") con puestos en esa asignación para el cuerpo dado. */
    public static List<String> especialidades(String cuerpo, String fecha, String tipo) throws Exception {
        JSONObject b = new JSONObject();
        b.put("codcuerpocfunc", cuerpo);
        b.put("fcpublico", fecha);
        b.put("itperfilacto", tipo);
        JSONArray a = post("getFiltroEspNec", b);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            String cod = o.optString("codespcfunc", "");
            if (cod.isEmpty()) {
                String cf = o.optString("cfunc", "");
                if (cf.length() >= 7) cod = cf.substring(4, 7);
            }
            if (!cod.isEmpty()) out.add(cod.trim());
        }
        return out;
    }

    /** Puestos ofertados para cuerpo+especialidad en todas las DAT. */
    public static JSONArray necesidades(String fecha, String cuerpo, String esp, String tipo) throws Exception {
        JSONObject b = new JSONObject();
        b.put("fcpublico", fecha);
        b.put("cfunc", cuerpo + esp);
        b.put("coddat", "T");
        b.put("itperfilacto", tipo);
        return post("consultaNecesidades", b);
    }
}
