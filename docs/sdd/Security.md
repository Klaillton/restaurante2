# Security

Restaurante 2.0 — PoC (até 18/11/2026). Estado atual: esqueleto, sem endpoints.
Presence gate **D15** checks heading + ≥1 requirement list item.

## Requirements

- Zero segredos no Git: o repositório é público até à venda. Credenciais só por variáveis de
  ambiente (`.env`, ignorado); `.env.example` só tem valores de exemplo (`trocar-me`).
- Dois utilizadores da BD (ADR 0002): `migracao` é dono do schema `restaurante` e das tabelas e
  só o Flyway o usa; `app` é com quem o Spring Boot se liga e não é dono de nada.
- `pedido_evento` é só de inserção, imposto pela BD: `app` tem `SELECT, INSERT` e nunca
  `UPDATE`, `DELETE` nem `TRUNCATE`. Provado pelo teste `PedidoEventoSoInsertsGateTest`
  (SQLSTATE `42501` exato; `42P01` ou INSERT falhado = vermelho).
- Os IDs de pedido e de evento são UUIDs gerados no cliente; o `evento_id` é chave primária,
  por isso um reenvio não duplica eventos (`ON CONFLICT DO NOTHING`, ADR 0002).
- Nunca registar em log passwords, tokens nem dados pessoais de clientes.
- Pendente (fora do esqueleto, entra com as features): autenticação dos ecrãs (comanda,
  cozinha, painel da dona) na rede local, e a licença com 7 dias sem rede (ADR 0004, fora do PoC).
