-- Laboratorio guiado de ejemplo (HU44): respuesta a incidente por una cuenta de correo
-- corporativo comprometida. Empresa, personas, servicios y dominios son inventados; los
-- dominios usan .example y las IP son de los rangos reservados para documentación.
--
-- Camino óptimo (90 puntos): ALERTA-B, REGISTRO-C, SESIONES-A, MFA-C, REGLA-B, REPORTE-B.
-- La opción ALERTA-A lleva al paso de recuperación CREDENCIALES.

INSERT INTO escenarios (uuid, tipo, titulo, descripcion, contexto, nivel, duracion_segundos, publicado, fecha_creacion, fecha_actualizacion)
VALUES ('a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22', 'LABORATORIO',
        'Respuesta a incidente: cuenta de correo corporativo comprometida',
        'Laboratorio guiado: analiza alertas, registros de acceso y la configuración del buzón para contener paso a paso el compromiso de una cuenta de correo corporativo.',
        'Eres Daniel Vargas, coordinador de compras en Logística Altamar S.A.S. (dominio oficial: logisticaaltamar.example). Tu correo corporativo está en el servicio CorreoNube, cuyo portal oficial es cuenta.correonube.example. Hoy aparecen señales de que alguien entró a tu cuenta. En cada paso analiza el recurso simulado (alertas, registros y pantallas) y elige qué hacer. Todos los nombres, dominios y direcciones IP son ficticios.',
        'INTERMEDIO', 900, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ------------------------------------------------------------------ pasos

INSERT INTO pasos_laboratorio (escenario_id, codigo, orden, titulo, situacion, recurso_simulado, es_final)
SELECT id, 'ALERTA', 1, 'Alerta de inicio de sesión desconocido',
       'Son las 7:52 a. m. y, antes de salir para la oficina, ves en tu celular un correo con una alerta de seguridad sobre tu cuenta corporativa. Anoche no usaste el correo después de las 9:00 p. m. ¿Qué haces?',
       'De: Seguridad CorreoNube <alertas@correonube-verificacion.example.net>
Asunto: [Acción requerida] Nuevo inicio de sesión en tu cuenta

Detectamos un nuevo inicio de sesión en daniel.vargas@logisticaaltamar.example
  Fecha:         hoy, 3:14 a. m. (hora de Colombia)
  Ubicación:     Bucarest, Rumania
  Dirección IP:  203.0.113.47
  Dispositivo:   Linux - Firefox

Si no fuiste tú, protege tu cuenta en los próximos 30 minutos o será suspendida.
  [ No fui yo - asegurar mi cuenta ]  -> https://correonube-verificacion.example.net/asegurar',
       FALSE
FROM escenarios WHERE uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22';

INSERT INTO pasos_laboratorio (escenario_id, codigo, orden, titulo, situacion, recurso_simulado, es_final)
SELECT id, 'CREDENCIALES', 2, 'Escribiste la contraseña en una página falsa',
       'La página se veía igual a la de CorreoNube, pero al enviar la contraseña mostró un error. Al mirar la barra de direcciones notas el dominio. ¿Qué haces ahora?',
       'Barra de direcciones:
  https://correonube-verificacion.example.net/asegurar/login

Contenido de la página:
  Error 502 - Servicio no disponible. Intenta más tarde.',
       FALSE
FROM escenarios WHERE uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22';

INSERT INTO pasos_laboratorio (escenario_id, codigo, orden, titulo, situacion, recurso_simulado, es_final)
SELECT id, 'REGISTRO', 3, 'Revisar el registro de inicios de sesión',
       'En la app oficial de CorreoNube de tu celular abres Seguridad > Actividad reciente. Tu computador corporativo está en la oficina. En casa tienes tu portátil personal, donde anoche instalaste un complemento "VisorFacturas" que llegó adjunto a un correo. ¿Cuál es tu siguiente paso?',
       'Actividad reciente - daniel.vargas@logisticaaltamar.example

FECHA/HORA   RESULTADO  IP             UBICACIÓN          DISPOSITIVO / APP
hoy 07:55    Correcto   192.0.2.10     Bogotá, Colombia   Android - App CorreoNube
hoy 03:22    Correcto   203.0.113.47   Bucarest, Rumania  Cliente IMAP "MailSync"
hoy 03:14    Correcto   203.0.113.47   Bucarest, Rumania  Linux - Firefox
hoy 03:11    Fallido    203.0.113.47   Bucarest, Rumania  Linux - Firefox
ayer 18:40   Correcto   192.0.2.10     Bogotá, Colombia   Windows - Outlook',
       FALSE
FROM escenarios WHERE uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22';

INSERT INTO pasos_laboratorio (escenario_id, codigo, orden, titulo, situacion, recurso_simulado, es_final)
SELECT id, 'SESIONES', 4, 'Sesiones y aplicaciones con acceso',
       'Ya cambiaste la contraseña. En Seguridad > Dispositivos y sesiones ves lo que sigue conectado a tu cuenta. ¿Qué haces?',
       'Sesiones activas
  1. Windows - Outlook          Bogotá, Colombia   192.0.2.10     (este dispositivo)
  2. Linux - Firefox            Bucarest, Rumania  203.0.113.47   última actividad: hace 4 min
  3. Android - App CorreoNube   Bogotá, Colombia   192.0.2.10

Aplicaciones con acceso a tu correo
  - MailSync (IMAP)   autorizada hoy 03:22   último acceso: hace 2 min

Nota: los tokens de sesión y de aplicaciones siguen siendo válidos aunque cambies la contraseña.',
       FALSE
FROM escenarios WHERE uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22';

INSERT INTO pasos_laboratorio (escenario_id, codigo, orden, titulo, situacion, recurso_simulado, es_final)
SELECT id, 'MFA', 5, 'Verificación en dos pasos',
       'Antes de seguir revisas la configuración de seguridad de la cuenta. ¿Qué cambias?',
       'Seguridad de la cuenta
  Verificación en dos pasos ...... DESACTIVADA
  Teléfono de recuperación ....... +40 700 000 019   (agregado hoy 03:16)
  Correo de recuperación ......... daniel.vargas@correo-personal.example
  Métodos disponibles: app autenticadora, llave de seguridad, SMS',
       FALSE
FROM escenarios WHERE uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22';

INSERT INTO pasos_laboratorio (escenario_id, codigo, orden, titulo, situacion, recurso_simulado, es_final)
SELECT id, 'REGLA', 6, 'Regla de reenvío en el buzón',
       'Un proveedor te escribe por chat preguntando por qué no contestaste su correo sobre un pago. No lo ves en tu bandeja de entrada, así que revisas Configuración > Reglas. ¿Qué haces con lo que encuentras?',
       'Reglas de la bandeja de entrada

  Nombre: "Sincronización"                 Creada: hoy 03:18
  Si el asunto o el cuerpo contiene: factura, pago, transferencia, cuenta bancaria
  Entonces:
    - Reenviar a archivo.contable@buzon-respaldo.example
    - Marcar como leído
    - Mover a la carpeta "Fuentes RSS"

Carpeta "Fuentes RSS": 4 mensajes (no leídos: 0)',
       FALSE
FROM escenarios WHERE uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22';

INSERT INTO pasos_laboratorio (escenario_id, codigo, orden, titulo, situacion, recurso_simulado, es_final)
SELECT id, 'REPORTE', 7, 'Reportar y avisar',
       'La cuenta ya está asegurada. Entre los mensajes ocultos hay una conversación con el proveedor Transportes Faro sobre un pago pendiente. ¿Cómo cierras el incidente?',
       'Carpeta "Fuentes RSS" (4)
  - Transportes Faro: "Factura TF-8812 - pago pendiente"               hoy 06:40
  - Banco Andino Empresas: "Inscripción de nueva cuenta destino"        hoy 05:02
  - Contabilidad: "Programación de pagos semana 42"                     ayer 17:10
  - Transportes Faro: "Re: cambio de condiciones de pago"               ayer 16:55',
       TRUE
FROM escenarios WHERE uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22';

-- ------------------------------------------------------------------ opciones

INSERT INTO opciones_paso (paso_id, codigo, texto, puntos, es_correcta, retroalimentacion, siguiente_paso_codigo)
SELECT p.id, 'A', 'Pulsar "No fui yo - asegurar mi cuenta" y escribir tu contraseña actual en la página que se abre.', 0, FALSE,
       'Nunca actúes desde el enlace de una alerta. Esta llegó de correonube-verificacion.example.net, que no es el dominio oficial, y mete prisa con un plazo de 30 minutos. Aunque el inicio de sesión fuera real, la página podía ser falsa, y acabas de entregar tu contraseña.',
       'CREDENCIALES'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'ALERTA'
UNION ALL
SELECT p.id, 'B', 'No usar el enlace: abrir tú mismo la app oficial de CorreoNube (o escribir cuenta.correonube.example) y revisar la actividad reciente de la cuenta.', 15, TRUE,
       'Correcto. Verificar por un canal oficial que tú controlas evita caer en una alerta falsa y te permite confirmar si el inicio de sesión fue real.',
       'REGISTRO'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'ALERTA'
UNION ALL
SELECT p.id, 'C', 'Borrar el correo: seguramente es spam y no pasa nada.', 0, FALSE,
       'Ignorar la alerta sin verificarla es arriesgado: si el inicio de sesión fue real, el atacante sigue dentro mientras tanto. Lo correcto era no usar el enlace, pero sí revisar la actividad por el canal oficial. A media mañana decides hacerlo.',
       'REGISTRO'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'ALERTA';

INSERT INTO opciones_paso (paso_id, codigo, texto, puntos, es_correcta, retroalimentacion, siguiente_paso_codigo)
SELECT p.id, 'A', 'Desde el computador corporativo, entrar a cuenta.correonube.example escribiendo la dirección, cambiar la contraseña de inmediato y avisar a la mesa de ayuda.', 8, TRUE,
       'Bien recuperado. Si entregaste una contraseña en una página falsa, hay que cambiarla cuanto antes desde un equipo confiable y por la dirección oficial. Aun así, el atacante pudo usarla mientras tanto: revisa qué sigue conectado a tu cuenta.',
       'SESIONES'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'CREDENCIALES'
UNION ALL
SELECT p.id, 'B', 'Cerrar la pestaña y seguir con el día: la página ni siquiera funcionó.', 0, FALSE,
       'Que la página muestre un error no significa que no haya guardado tu contraseña: es un truco común para que no sospeches. La contraseña quedó expuesta y debe cambiarse ya.',
       'REGISTRO'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'CREDENCIALES'
UNION ALL
SELECT p.id, 'C', 'Volver a escribir la contraseña por si la primera vez quedó mal.', 0, FALSE,
       'Repetir la contraseña en la misma página falsa solo le confirma al atacante que es correcta. Hay que salir de allí y cambiarla por el canal oficial.',
       'REGISTRO'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'CREDENCIALES';

INSERT INTO opciones_paso (paso_id, codigo, texto, puntos, es_correcta, retroalimentacion, siguiente_paso_codigo)
SELECT p.id, 'A', 'Esperar al final del día para contárselo a TI y que ellos decidan.', 0, FALSE,
       'El registro confirma un acceso exitoso desde Rumania y un cliente IMAP descargando tu correo. Cada hora cuenta: el atacante puede leer, reenviar y enviar correos en tu nombre. Había que cambiar la contraseña de inmediato.',
       'SESIONES'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'REGISTRO'
UNION ALL
SELECT p.id, 'B', 'Cambiar la contraseña ya mismo desde el portátil personal, donde instalaste el complemento.', 3, FALSE,
       'Cambiar la contraseña es urgente, pero no desde un equipo que puede estar comprometido: un complemento que llegó por correo puede registrar lo que escribes, y el atacante obtendría también la contraseña nueva. Usa un dispositivo limpio.',
       'SESIONES'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'REGISTRO'
UNION ALL
SELECT p.id, 'C', 'Cambiar la contraseña desde el computador corporativo (gestionado por TI y sin el complemento), con una contraseña larga y única que no uses en ningún otro sitio.', 15, TRUE,
       'Correcto. El registro confirma un acceso real desde una IP desconocida. La contraseña nueva debe crearse desde un dispositivo limpio y ser única, para que el atacante no la capture ni la pruebe con contraseñas filtradas de otros sitios.',
       'SESIONES'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'REGISTRO';

INSERT INTO opciones_paso (paso_id, codigo, texto, puntos, es_correcta, retroalimentacion, siguiente_paso_codigo)
SELECT p.id, 'A', 'Cerrar sesión en todos los dispositivos y revocar el acceso de MailSync; luego volver a entrar solo en tus equipos.', 15, TRUE,
       'Correcto. Cambiar la contraseña no expulsa al atacante si ya tiene una sesión abierta o una aplicación autorizada. Hay que cerrar todas las sesiones y revocar los accesos de terceros.',
       'MFA'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'SESIONES'
UNION ALL
SELECT p.id, 'B', 'Cerrar solo la sesión de Firefox en Bucarest.', 5, FALSE,
       'Vas bien, pero queda incompleto: MailSync sigue autorizada y descarga tu correo por IMAP sin necesitar la contraseña. Revoca también las aplicaciones que no reconoces.',
       'MFA'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'SESIONES'
UNION ALL
SELECT p.id, 'C', 'No tocar nada: con la contraseña nueva el atacante ya no puede entrar.', 0, FALSE,
       'Es un error frecuente. Las sesiones abiertas y las aplicaciones autorizadas usan tokens que siguen funcionando después del cambio de contraseña. El atacante sigue leyendo tu correo.',
       'MFA'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'SESIONES';

INSERT INTO opciones_paso (paso_id, codigo, texto, puntos, es_correcta, retroalimentacion, siguiente_paso_codigo)
SELECT p.id, 'A', 'Activar la verificación en dos pasos por SMS con el teléfono que ya aparece registrado.', 3, FALSE,
       'Cuidado: ese número (+40, agregado hoy a las 03:16) lo puso el atacante para poder recuperar la cuenta. Activar el SMS hacia ese número le daría a él el segundo factor. Primero elimina los datos de recuperación que no reconoces.',
       'REGLA'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'MFA'
UNION ALL
SELECT p.id, 'B', 'Dejarlo para después: la verificación en dos pasos es incómoda.', 0, FALSE,
       'La verificación en dos pasos es la medida que más reduce el riesgo de que una contraseña robada sirva para entrar. Dejarla desactivada después de un incidente deja la puerta abierta al siguiente intento.',
       'REGLA'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'MFA'
UNION ALL
SELECT p.id, 'C', 'Eliminar el teléfono de recuperación desconocido, activar la verificación en dos pasos con una app autenticadora y guardar los códigos de respaldo en un lugar seguro.', 15, TRUE,
       'Correcto. Con un segundo factor que solo tú controlas, una contraseña robada ya no basta para entrar. Además quitaste el teléfono que el atacante agregó para recuperar la cuenta.',
       'REGLA'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'MFA';

INSERT INTO opciones_paso (paso_id, codigo, texto, puntos, es_correcta, retroalimentacion, siguiente_paso_codigo)
SELECT p.id, 'A', 'Dejar la regla: tal vez la creó TI para respaldar la información contable.', 0, FALSE,
       'TI no crea reglas en tu buzón a las 3:18 a. m. ni reenvía a dominios externos. Es una táctica típica del atacante: esconder los correos de pagos y quedarse con una copia para preparar un fraude.',
       'REPORTE'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'REGLA'
UNION ALL
SELECT p.id, 'B', 'Tomar captura de la regla como evidencia, eliminarla y revisar los 4 mensajes de "Fuentes RSS" para saber qué información se filtró.', 15, TRUE,
       'Correcto. Eliminar la regla corta la fuga, y la captura y los mensajes ocultos son evidencia para Seguridad: muestran qué datos tiene el atacante y a quién podría intentar engañar.',
       'REPORTE'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'REGLA'
UNION ALL
SELECT p.id, 'C', 'Eliminar la regla y borrar también la carpeta "Fuentes RSS" con sus mensajes para no dejar rastro.', 5, FALSE,
       'Cortaste el reenvío, pero borraste la evidencia: esos mensajes muestran qué información se filtró y a qué proveedores podría contactar el atacante. Conserva todo para el reporte.',
       'REPORTE'
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'REGLA';

INSERT INTO opciones_paso (paso_id, codigo, texto, puntos, es_correcta, retroalimentacion, siguiente_paso_codigo)
SELECT p.id, 'A', 'Contárselo solo a tu jefe por chat y no reportarlo a TI para no armar un problema.', 5, FALSE,
       'Avisar a tu jefe ayuda, pero sin un reporte formal TI no puede revisar si otras cuentas fueron afectadas, bloquear la IP ni alertar a contabilidad sobre posibles cambios fraudulentos de cuenta bancaria.',
       NULL
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'REPORTE'
UNION ALL
SELECT p.id, 'B', 'Reportar el incidente a TI y Seguridad con la evidencia (registro de accesos, IP, regla de reenvío) y avisar a contabilidad y a tus proveedores, por un canal conocido, que ignoren cualquier solicitud de cambio de cuenta enviada desde tu correo.', 15, TRUE,
       'Correcto. El reporte permite contener el incidente en toda la empresa, y avisar a tus contactos por un canal conocido evita que el atacante use la información robada para un fraude de pagos.',
       NULL
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'REPORTE'
UNION ALL
SELECT p.id, 'C', 'No decir nada: la cuenta ya está asegurada y el incidente terminó.', 0, FALSE,
       'El incidente no termina al asegurar la cuenta: el atacante tiene información de pagos y proveedores y puede intentar un fraude haciéndose pasar por ti. Callarlo impide que la empresa y tus contactos se protejan.',
       NULL
FROM pasos_laboratorio p JOIN escenarios e ON e.id = p.escenario_id
WHERE e.uuid = 'a3d5e7f9-2b4c-4d6e-8f10-3c5e7a9b1d22' AND p.codigo = 'REPORTE';
