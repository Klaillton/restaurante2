# Observability

Restaurante 2.0 — PoC (até 18/11/2026). Decisão do Dante: a observabilidade fica para depois
do PoC. Este documento diz o que existe hoje e o que fica adiado, sem fingir mais.
Presence gate **D15** checks heading + ≥1 requirement list item.

## Requirements

- Hoje: só os logs por omissão do Spring Boot na consola (`docker compose logs app`), sem
  passwords nem dados pessoais.
- Hoje: o healthcheck do `postgres` no `docker-compose.yml` (`pg_isready`) controla o arranque
  da app.
- Adiado para depois do PoC: logs estruturados com id de correlação, endpoint de saúde da app
  (Actuator), métricas de erros 5xx e alertas do servidor local (48h sem rede, sincronização).
