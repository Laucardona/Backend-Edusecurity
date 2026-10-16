-- Escenario de ejemplo: phishing dirigido (spear phishing y whaling) en una empresa ficticia.
-- Empresa, personas, bancos y dominios son inventados. El dominio legítimo es constructoraandina.co.

INSERT INTO escenarios (uuid, tipo, titulo, descripcion, contexto, nivel, duracion_segundos, publicado, fecha_creacion, fecha_actualizacion)
VALUES ('6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01', 'PHISHING_DIRIGIDO',
        'Phishing dirigido: cierre de mes en Constructora Andina',
        'Analiza la bandeja de una analista de cuentas por pagar el día de cierre contable. Algunos mensajes fueron preparados con información interna de la empresa para engañarla.',
        'Eres Camila Rojas, analista de cuentas por pagar en Constructora Andina S.A.S. (dominio oficial: constructoraandina.co). Hoy es el último día hábil del mes y el área de finanzas cierra pagos a las 4:00 p. m. Tu jefe directo es Julián Pardo, director financiero, y el gerente general es Andrés Mejía. La empresa está construyendo el proyecto Torre Caobos. Revisa cada mensaje: decide si es phishing o legítimo, qué harías con él y qué señales de alerta encontraste.',
        'INTERMEDIO', 600, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 1. Whaling: suplantación del gerente general con dominio parecido
INSERT INTO mensajes_simulados (escenario_id, orden, canal, remitente_nombre, remitente_direccion, asunto, cuerpo, enlace_mostrado, enlace_real, adjunto_nombre, recibido_en, es_phishing, accion_correcta, explicacion)
SELECT id, 1, 'CORREO', 'Andrés Mejía - Gerente General', 'a.mejia@constructoraandlna.co',
       'Transferencia confidencial antes del cierre',
       'Camila, buen día. Estoy en una reunión con la junta y no puedo atender llamadas. Necesito que hoy, antes de las 3:00 p. m., gestiones un pago de $48.500.000 al nuevo proveedor de acero de Torre Caobos. Cambiaron su cuenta bancaria, te paso los datos nuevos por WhatsApp al 312 555 0199. Es un tema confidencial de la junta: no lo comentes con Julián ni con nadie del área hasta que se cierre la negociación. Confío en ti.',
       NULL, NULL, NULL, 'Hoy, 10:42 a. m.', TRUE, 'REPORTAR',
       'Es un fraude del CEO (whaling). El dominio constructoraandlna.co cambia la i por una l. El atacante usa datos reales (Torre Caobos, el nombre de tu jefe) para parecer creíble, mete prisa, pide secreto, cambia la cuenta bancaria y mueve la conversación a WhatsApp para salir de los controles de la empresa. Lo correcto es reportarlo y verificar con el gerente por un canal conocido.'
FROM escenarios WHERE uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01';

-- 2. Legítimo: aviso de mantenimiento de TI desde el dominio oficial
INSERT INTO mensajes_simulados (escenario_id, orden, canal, remitente_nombre, remitente_direccion, asunto, cuerpo, enlace_mostrado, enlace_real, adjunto_nombre, recibido_en, es_phishing, accion_correcta, explicacion)
SELECT id, 2, 'CORREO', 'Mesa de Ayuda TI', 'mesadeayuda@constructoraandina.co',
       'Mantenimiento programado del ERP este sábado',
       'Hola a todos. El sábado de 6:00 a. m. a 10:00 a. m. el ERP estará fuera de servicio por actualización. No necesitas hacer nada. Recuerda que la Mesa de Ayuda nunca te pedirá tu contraseña por correo, chat ni teléfono. Si tienes dudas, escribe por el canal de soporte de la intranet.',
       NULL, NULL, NULL, 'Hoy, 9:15 a. m.', FALSE, 'CONSERVAR',
       'Es legítimo: viene del dominio oficial, no pide datos, no tiene enlaces ni adjuntos y no presiona. Solo informa. Marcarlo como phishing sería un falso positivo.'
FROM escenarios WHERE uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01';

-- 3. Spear phishing: factura de proveedor con enlace que no coincide
INSERT INTO mensajes_simulados (escenario_id, orden, canal, remitente_nombre, remitente_direccion, asunto, cuerpo, enlace_mostrado, enlace_real, adjunto_nombre, recibido_en, es_phishing, accion_correcta, explicacion)
SELECT id, 3, 'CORREO', 'Facturación Aceros del Valle', 'facturacion@acerosdelvalle-pagos.com',
       'Factura vencida FV-2291 - Torre Caobos',
       'Señores Constructora Andina, la factura FV-2291 del suministro de varilla para Torre Caobos presenta 15 días de mora. Para evitar la suspensión de despachos, descargue el soporte y confirme el pago en nuestro portal de proveedores con su usuario corporativo.',
       'portal.acerosdelvalle.com/facturas/FV-2291', 'http://acerosdelvalle-pagos.top/login-office365',
       NULL, 'Hoy, 11:05 a. m.', TRUE, 'REPORTAR',
       'Es phishing dirigido. El texto del enlace muestra un portal del proveedor, pero al pasar el cursor apunta a otro dominio (.top) con una página que imita el inicio de sesión de Office 365. Usa el nombre del proyecto para parecer real, amenaza con suspender despachos y te pide tu usuario corporativo.'
FROM escenarios WHERE uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01';

-- 4. Legítimo: el director financiero pide revisar un archivo en la intranet
INSERT INTO mensajes_simulados (escenario_id, orden, canal, remitente_nombre, remitente_direccion, asunto, cuerpo, enlace_mostrado, enlace_real, adjunto_nombre, recibido_en, es_phishing, accion_correcta, explicacion)
SELECT id, 4, 'CORREO', 'Julián Pardo - Director Financiero', 'julian.pardo@constructoraandina.co',
       'Conciliación de octubre',
       'Camila, dejé la conciliación de octubre en la carpeta de Finanzas de la intranet. Cuando puedas, revisa las partidas de Torre Caobos y me comentas mañana en el comité. Gracias.',
       'intranet.constructoraandina.co/finanzas/conciliacion-octubre', 'https://intranet.constructoraandina.co/finanzas/conciliacion-octubre',
       NULL, 'Hoy, 11:30 a. m.', FALSE, 'CONSERVAR',
       'Es legítimo: el remitente y el enlace usan el dominio oficial, el texto del enlace coincide con su destino real, no pide credenciales ni pagos y el plazo es razonable.'
FROM escenarios WHERE uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01';

-- 5. Smishing: banco ficticio con dominio parecido
INSERT INTO mensajes_simulados (escenario_id, orden, canal, remitente_nombre, remitente_direccion, asunto, cuerpo, enlace_mostrado, enlace_real, adjunto_nombre, recibido_en, es_phishing, accion_correcta, explicacion)
SELECT id, 5, 'SMS', 'Banco Andino', '+57 300 555 0142',
       NULL,
       'BANCO ANDINO: Su token empresarial vence HOY. Para no bloquear los pagos del cierre, actualícelo en bancoandlno-empresas.co/token antes de las 2 p. m.',
       'bancoandlno-empresas.co/token', 'http://bancoandlno-empresas.co/token',
       NULL, 'Hoy, 12:10 p. m.', TRUE, 'ELIMINAR',
       'Es smishing. El dominio bancoandlno-empresas.co imita al banco cambiando una letra, llega desde un número celular común, mete prisa con el cierre de pagos y pide actualizar un token por un enlace. Los bancos no piden esto por SMS. Elimínalo y, si tienes dudas, llama al número oficial del banco.'
FROM escenarios WHERE uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01';

-- 6. Phishing: falso Recursos Humanos desde correo gratuito con datos personales
INSERT INTO mensajes_simulados (escenario_id, orden, canal, remitente_nombre, remitente_direccion, asunto, cuerpo, enlace_mostrado, enlace_real, adjunto_nombre, recibido_en, es_phishing, accion_correcta, explicacion)
SELECT id, 6, 'CORREO', 'Recursos Humanos Constructora Andina', 'rrhh.constructoraandina@gmail.com',
       'Actualización obligatoria de datos de nómina',
       'Hola Camila Rojas (C.C. terminada en 4417). Por cambio de operador de nómina, todos los empleados deben actualizar su cuenta para el pago de la quincena. Abre el formulario adjunto, habilita las macros y diligéncialo hoy. Si no lo haces, tu pago podría retrasarse.',
       NULL, NULL, 'Actualizacion_Nomina.xlsm', 'Hoy, 1:20 p. m.', TRUE, 'REPORTAR',
       'Es phishing dirigido. Recursos Humanos no escribe desde una cuenta de Gmail, el adjunto .xlsm pide habilitar macros (una forma común de instalar malware) y usa datos personales reales (tu cédula parcial) para ganar confianza, junto con la amenaza de retrasar tu pago.'
FROM escenarios WHERE uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01';

-- 7. Legítimo: comunicado del gerente general desde el dominio oficial
INSERT INTO mensajes_simulados (escenario_id, orden, canal, remitente_nombre, remitente_direccion, asunto, cuerpo, enlace_mostrado, enlace_real, adjunto_nombre, recibido_en, es_phishing, accion_correcta, explicacion)
SELECT id, 7, 'CORREO', 'Andrés Mejía - Gerente General', 'andres.mejia@constructoraandina.co',
       'Celebración de fin de obra estructural Torre Caobos',
       'Equipo, el próximo viernes a las 5:00 p. m. celebraremos el fin de la obra estructural de Torre Caobos en la sede principal. No hace falta confirmar. Gracias a todos por el esfuerzo de este año.',
       NULL, NULL, NULL, 'Hoy, 2:05 p. m.', FALSE, 'CONSERVAR',
       'Es legítimo: viene del dominio oficial (compáralo con el mensaje 1), no pide dinero, datos ni acciones y no presiona. Que mencione al gerente o a Torre Caobos no lo vuelve sospechoso por sí solo.'
FROM escenarios WHERE uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01';

-- Señales de alerta de los mensajes de phishing
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'DOMINIO_SIMILAR', 'El dominio imita al oficial cambiando la i de andina por una l.', 'constructoraandlna.co'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 1;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'URGENCIA', 'Fija un plazo corto para que actúes sin pensar ni verificar.', 'hoy, antes de las 3:00 p. m.'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 1;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'SOLICITUD_INUSUAL', 'Pide un pago a una cuenta bancaria nueva fuera del proceso normal.', 'Cambiaron su cuenta bancaria'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 1;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'SECRETISMO', 'Pide no contarle a tu jefe ni al equipo, para que nadie lo detenga.', 'no lo comentes con Julián ni con nadie'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 1;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'CAMBIO_DE_CANAL', 'Lleva la conversación a WhatsApp, fuera de los canales de la empresa.', 'te paso los datos nuevos por WhatsApp'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 1;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'SUPLANTACION_AUTORIDAD', 'Se hace pasar por el gerente general para que no cuestiones la orden.', 'Andrés Mejía - Gerente General'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 1;

INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'ENLACE_ENGANOSO', 'El texto del enlace no coincide con su destino real (.top).', 'acerosdelvalle-pagos.top/login-office365'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 3;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'PIDE_CREDENCIALES', 'Pide iniciar sesión con tu usuario corporativo en un portal externo.', 'con su usuario corporativo'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 3;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'AMENAZA', 'Amenaza con suspender los despachos para presionarte.', 'evitar la suspensión de despachos'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 3;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'DOMINIO_SIMILAR', 'El remitente usa un dominio que no es el del proveedor (acerosdelvalle-pagos.com).', 'acerosdelvalle-pagos.com'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 3;

INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'DOMINIO_SIMILAR', 'El dominio imita al banco cambiando la i por una l.', 'bancoandlno-empresas.co'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 5;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'URGENCIA', 'Dice que el token vence hoy y pone una hora límite.', 'vence HOY'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 5;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'REMITENTE_SOSPECHOSO', 'Un banco no escribe desde un número celular común.', '+57 300 555 0142'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 5;

INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'REMITENTE_SOSPECHOSO', 'Recursos Humanos no usa cuentas de correo gratuitas.', 'rrhh.constructoraandina@gmail.com'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 6;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'ADJUNTO_PELIGROSO', 'El archivo .xlsm pide habilitar macros, que pueden instalar malware.', 'Actualizacion_Nomina.xlsm'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 6;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'DATOS_PERSONALES', 'Usa datos personales reales para parecer confiable.', 'C.C. terminada en 4417'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 6;
INSERT INTO indicadores (mensaje_id, tipo, descripcion, fragmento)
SELECT m.id, 'AMENAZA', 'Amenaza con retrasar tu pago si no actúas hoy.', 'tu pago podría retrasarse'
FROM mensajes_simulados m JOIN escenarios e ON e.id = m.escenario_id WHERE e.uuid = '6f1c2b9e-3d4a-4f6b-9a1e-0c5d7e8f9a01' AND m.orden = 6;
