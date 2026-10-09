# Backend local con MySQL

El backend usa el perfil `local` cuando no hay un perfil activo. El Dockerfile de producción no incluye `application.properties` ni `application-local.properties` por `.dockerignore`; producción sigue usando `SPRING_PROFILES_ACTIVE=prod` y su archivo externo. No arranques aquí los perfiles `default`, `dev` ni `prod`.

## Preparación

Instala Docker Desktop y comprueba `docker --version` y `docker compose version`. En esta Mac, `docker --version` no estaba disponible al preparar esta guía.

`.env.local` se creó con credenciales locales aleatorias y está ignorado por Git. Si necesitas recrearlo, usa `.env.local.example` y genera valores nuevos. `JWT_SECRET` requiere Base64 de al menos 64 bytes aleatorios; `APP_CRYPTO_SECRET_KEY` requiere 16, 24 o 32 bytes ASCII. No copies secretos de producción salvo que necesites descifrar campos de un respaldo local y tengas autorización para usar ese respaldo.

Desde la raíz del backend:

```sh
docker compose --env-file .env.local -f docker-compose.local.yml up -d mysql
docker compose --env-file .env.local -f docker-compose.local.yml ps
set -a
source .env.local
set +a
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

El backend usa MySQL en `127.0.0.1:3306`, base `db_sistema_operaciones`. Si el puerto 3306 ya está ocupado, detén el otro servicio antes de iniciar este contenedor. Las pruebas usan H2 en memoria y se ejecutan con `mvn test`.

La configuración local de correo apunta a `127.0.0.1:1025`; no se incluye servidor SMTP. Los flujos que envían correo fallarán hasta que se configure un SMTP local. Ningún servicio de correo externo está configurado en este perfil.

## Importar un respaldo `.sql.gz`

La importación modifica únicamente la base local, pero puede borrar o reemplazar sus datos. Haz una copia de cualquier dato local que quieras conservar. Primero revisa las instrucciones de base de datos del archivo:

```sh
BACKUP_SQL_GZ=/ruta/al/respaldo.sql.gz
gzip -t "$BACKUP_SQL_GZ"
gzip -dc "$BACKUP_SQL_GZ" | rg -n -m 20 '^(CREATE DATABASE|USE |DROP DATABASE|DROP TABLE)'
```

Si aparece `USE` de otra base, `CREATE DATABASE` o `DROP DATABASE`, adapta el respaldo en una copia antes de importar. No importes un volcado que apunte a un servidor o base distintos sin revisarlo. Para un respaldo de las tablas de `db_sistema_operaciones`:

```sh
gzip -dc "$BACKUP_SQL_GZ" | docker compose --env-file .env.local -f docker-compose.local.yml exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -u root "$MYSQL_DATABASE"'
docker compose --env-file .env.local -f docker-compose.local.yml exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -u root -D "$MYSQL_DATABASE" -e "SHOW TABLES"'
```

El password se toma del entorno del contenedor, sin escribirlo en la línea de comandos del host. Tras importar, revisa si el respaldo contiene datos cifrados con una clave AES distinta: esos campos no se podrán descifrar con la clave local aleatoria. `spring.jpa.hibernate.ddl-auto=update` aplica solo al perfil local y puede modificar el esquema al arrancar; importa y revisa el respaldo antes de iniciar el backend.
