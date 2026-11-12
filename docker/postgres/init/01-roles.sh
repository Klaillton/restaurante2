#!/usr/bin/env bash
# Cria os dois utilizadores da BD do ADR 0002. Corre UMA vez, pelo entrypoint oficial do
# postgres, quando o volume de dados está vazio (/docker-entrypoint-initdb.d).
#   migracao: dono do schema e das tabelas; só o Flyway o usa.
#   app:      com quem o Spring Boot se liga; não é dono de nada (os GRANT estão na V1).
# As passwords vêm só de variáveis de ambiente (.env, nunca no Git).
set -euo pipefail

: "${POSTGRES_USER:?POSTGRES_USER em falta}"
: "${POSTGRES_DB:?POSTGRES_DB em falta}"
: "${MIGRACAO_PASSWORD:?MIGRACAO_PASSWORD em falta}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD em falta}"

psql -v ON_ERROR_STOP=1 --no-psqlrc \
  --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v db="$POSTGRES_DB" \
  -v migracao_pw="$MIGRACAO_PASSWORD" \
  -v app_pw="$APP_DB_PASSWORD" <<'SQL'
-- format(%L) cita o valor como literal SQL; a password nunca passa por concatenação em shell.
SELECT format('CREATE ROLE migracao LOGIN PASSWORD %L', :'migracao_pw') \gexec
SELECT format('CREATE ROLE app LOGIN PASSWORD %L', :'app_pw') \gexec

REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO migracao, app;

-- Ninguém cria objetos no schema public.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- O schema da aplicação pertence ao migracao; o app só o pode usar (sem CREATE).
CREATE SCHEMA restaurante AUTHORIZATION migracao;
REVOKE ALL ON SCHEMA restaurante FROM PUBLIC;
GRANT USAGE ON SCHEMA restaurante TO app;

ALTER ROLE migracao SET search_path = restaurante;
ALTER ROLE app SET search_path = restaurante;
SQL
