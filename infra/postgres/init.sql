-- Una instancia de PostgreSQL con una base por servicio. No comparten tablas:
-- ms-reservas no puede leer el padrón de ms-pacientes con un JOIN, y ese es el punto.
CREATE DATABASE mediturno_reservas OWNER mediturno;
CREATE DATABASE mediturno_pacientes OWNER mediturno;
