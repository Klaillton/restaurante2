# Holes review — adoção do gauntlet + schema `pedido` / `pedido_evento`

Data: 12/11/2026. Âmbito: PR `chore/gauntlet-adopt` (esqueleto Maven, migração V1 do ADR 0002,
script de roles, gate de permissões). Não há features nem endpoints neste PR.

## Ambiguities

- Valores e transições de `pedido.estado`: o ADR 0002 lista os tipos de evento, não os estados.
  A V1 deixa `estado TEXT NOT NULL` sem CHECK; a feature define-os (e o CHECK entra numa V2).
- Quem atribui `sequencia` e como, com dois pedidos de alteração em simultâneo para o mesmo
  pedido: a V1 só garante `UNIQUE (pedido_id, sequencia)`; o serviço tem de tratar a colisão
  (por exemplo, `SELECT ... FOR UPDATE` no `pedido` antes de calcular a próxima).
- `pedido` não tem `DELETE` para o `app` (o ADR não o pede). Cancelar um pedido terá de ser um
  estado/evento, não um apagamento. Confirmar com o Arq quando houver a feature.
- Forma do `payload` por `tipo` (o ADR só fixa que `ALTERADO` leva o pedido inteiro).

## Contradictions

- Reenvio com o mesmo `evento_id` mas `payload` diferente: com `ON CONFLICT DO NOTHING` (ADR
  0002) é aceite em silêncio e o segundo payload perde-se. Não contradiz o ADR, mas contradiz a
  ideia de auditoria; a feature deve comparar e rejeitar (como o 422 do Gateway).
- `ON CONFLICT DO NOTHING` sem alvo também engoliria uma colisão de `(pedido_id, sequencia)`,
  que não é um reenvio. A feature deve usar `ON CONFLICT (evento_id) DO NOTHING`.

## Missing AC

- ADR 0002, ponto 1 (pedido e evento na mesma transação, com falha forçada a meio e nada
  gravado) fica para a feature do módulo `pedidos`: o teste só faz sentido contra o serviço
  real, não contra JDBC solto.
- Critérios da sincronização com a cloud e das 48h offline: fora do PoC.

## Unhappy/edge

- Tabela em falta: o UPDATE/DELETE daria SQLSTATE `42P01`; o gate exige `42501` exato e insere
  e lê uma linha antes, por isso uma migração em falta deixa-o vermelho.
- O script de roles só corre com o volume vazio: mudar uma password no `.env` depois do
  primeiro arranque não muda a BD (é preciso `ALTER ROLE` ou `docker compose down -v`).
- Passwords com aspas ou espaços: passam por variáveis do `psql` (`:'var'`), sem
  concatenação em shell.
- Relógio do cliente adiantado ou atrasado: `ocorrido_em` é só informativo; a ordem vem de
  `recebido_em` (servidor) e `sequencia`.
