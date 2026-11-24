# PADI Interinos (Android)

App que consulta cada día lectivo (lunes a viernes, 15:00 por defecto) los puestos de interinidad
ofertados en PADI (Comunidad de Madrid) para el cuerpo 0590 y las especialidades
061, 101, 105, 110, 221 y 222, en convocatoria ordinaria y extraordinaria y en todas las DAT.

- Busca sola la asignación del siguiente día lectivo: no hace falta calendario de festivos.
- Si a esa hora aún no está publicada, reintenta cada 30 minutos hasta las 20:00.
- Guarda cada asignación en el móvil (almacenamiento interno de la app) y marca los puestos nuevos.
- Avisa con una notificación; botón "Actualizar" para consultar en cualquier momento.
- Filtros por convocatoria, especialidad, DAT, vacante/sustitución, nuevos y texto. Exporta CSV.
- Sin dependencias externas. Android 8.0 o superior.

## Compilar

**Opción A – GitHub (sin instalar nada):** sube esta carpeta a un repositorio de GitHub. La acción
`.github/workflows/build.yml` compila el APK; descárgalo en Actions → última ejecución → Artifacts.

**Opción B – Android Studio:** File → Open → esta carpeta → Build → Build App Bundle(s) / APK(s) → Build APK(s).
El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar en el móvil
Copia el APK al móvil, ábrelo y permite "instalar apps de fuentes desconocidas" cuando lo pida.
Al abrir la app por primera vez, acepta el permiso de notificaciones.
En algunos móviles (Xiaomi, Huawei, Samsung…) conviene quitar la "optimización de batería" para
PADI Interinos, para que la consulta de las 15:00 no se retrase.
