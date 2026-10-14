# Rutas para Mascotas

App web para dueños de mascotas, funciona en cualquier ciudad:

- **Mapa real** (OpenStreetMap) con búsqueda de ciudad o dirección y botón "Mi ubicación".
- **Parques**: zonas caninas y parques donde se aceptan perros, más otros parques cercanos.
- **Veterinarias**: con filtro "Solo 24 h" y accesos directos a Google Maps para ver las de turno ahora.
- **Lugares pet-friendly**: cafés, restaurantes, hoteles, tiendas y peluquerías para mascotas.
- **Alertas de mascotas perdidas o encontradas** con foto, descripción, punto en el mapa y contacto por WhatsApp.
- Cualquiera puede **agregar lugares** que falten.

Es un solo archivo HTML, sin instalación ni servidor propio.

## Publicarla gratis con GitHub Pages

1. Crea el repositorio (por ejemplo `rutas-para-mascotas`), marcado como **Public**.
2. En el repositorio: **Add file → Upload files** y sube `index.html`, `config.js` y `README.md`. Pulsa **Commit changes**.
3. Ve a **Settings → Pages**. En "Branch" elige `main` y carpeta `/ (root)`. Pulsa **Save**.
4. En 1–2 minutos la app queda en `https://TU-USUARIO.github.io/rutas-para-mascotas/`.

En el celular, abre ese enlace y usa "Agregar a pantalla de inicio" para tenerla como app.

## Alertas compartidas (Firebase, gratis)

Sin este paso, las alertas solo se guardan en el teléfono de quien las publica. Para que toda la comunidad las vea:

1. Entra a <https://console.firebase.google.com> y crea un proyecto.
2. Menú **Build → Firestore Database → Create database** (modo producción, región cercana, ej. `southamerica-east1`).
3. En la pestaña **Rules** de Firestore pega estas reglas y publica:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /{col}/{id} {
      allow read: if col in ['alerts', 'places'];
      allow create: if col in ['alerts', 'places']
        && request.resource.data.keys().size() < 20
        && (!('photo' in request.resource.data) || request.resource.data.photo == null
            || request.resource.data.photo.size() < 800000)
        && request.resource.data.lat is number
        && request.resource.data.lng is number;
      allow update: if col == 'alerts'
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'resolvedAt']);
      allow delete: if false;
    }
  }
}
```

4. En **Project settings (⚙️) → General → Your apps**, agrega una app **Web** (`</>`). Copia el objeto `firebaseConfig`.
5. Pégalo en `config.js`, en `firebase: { ... }` (reemplazando `null`), y súbelo de nuevo al repositorio.

Con estas reglas cualquiera puede publicar y marcar alertas como reencontradas, pero nadie puede borrarlas desde la app. Las alertas falsas o antiguas se borran desde la consola de Firebase.

## Datos

- Lugares: © colaboradores de OpenStreetMap (consultados en vivo con la API Overpass). Si falta un parque o veterinaria, se puede agregar en la app o directamente en openstreetmap.org.
- Búsqueda de ciudades: Nominatim (OpenStreetMap).
- Los horarios de turno de veterinarias cambian a diario; la app enlaza a Google Maps para verlos en tiempo real.
