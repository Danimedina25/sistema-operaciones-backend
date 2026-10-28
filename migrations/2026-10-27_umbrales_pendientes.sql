-- Semáforo de pendientes: amarillo desde horas_pendiente_atencion, rojo desde horas_pendiente_urgente.
-- Los valores se editan en Administración › Configuración; aquí solo se crean con su valor inicial.
ALTER TABLE configuracion_general
 ADD COLUMN horas_pendiente_atencion INT NOT NULL DEFAULT 4,
 ADD COLUMN horas_pendiente_urgente INT NOT NULL DEFAULT 24;
