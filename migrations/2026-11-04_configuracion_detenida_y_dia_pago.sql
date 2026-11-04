-- Horas para marcar una operación como detenida (antes fijas en 48 en el frontend) y día de pago de
-- comisiones (antes fijo en sábado). Día ISO: 1 = lunes … 7 = domingo. Se editan en
-- Administración › Configuración; aquí solo se crean con su valor inicial.
ALTER TABLE configuracion_general
 ADD COLUMN horas_operacion_detenida INT NOT NULL DEFAULT 48,
 ADD COLUMN dia_pago_comisiones INT NOT NULL DEFAULT 6;
