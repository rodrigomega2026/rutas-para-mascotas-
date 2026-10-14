// Configuración de Rutas para Mascotas.
// Sin Firebase, las alertas y lugares agregados se guardan solo en el dispositivo de cada persona.
// Para que todos vean las mismas alertas, pega aquí la configuración de tu proyecto Firebase
// (ver README.md, sección "Alertas compartidas").
window.APP_CONFIG = {
  // Punto donde abre el mapa la primera vez (luego recuerda el último lugar visto)
  startView: { lat: -33.4372, lng: -70.6345, zoom: 14 }, // Santiago, Chile

  firebase: null
  /* Ejemplo:
  firebase: {
    apiKey: "AIza...",
    authDomain: "tu-proyecto.firebaseapp.com",
    projectId: "tu-proyecto",
    storageBucket: "tu-proyecto.appspot.com",
    messagingSenderId: "123456789",
    appId: "1:123456789:web:abc123"
  }
  */
};
