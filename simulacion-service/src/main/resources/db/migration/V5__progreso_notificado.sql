-- I7.4: marca si el resultado de una sesión cerrada ya se envió a progreso-service.
-- Las sesiones cerradas con FALSE se reenvían periódicamente (outbox ligero).
ALTER TABLE sesiones_simulacion ADD progreso_notificado BOOLEAN NOT NULL DEFAULT FALSE;
