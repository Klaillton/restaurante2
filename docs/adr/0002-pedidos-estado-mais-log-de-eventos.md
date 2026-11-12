# ADR 0002 — Pedido com estado mais log de eventos imutável

Estado: aceite com revisão (12/11/2026, Challenge: rever, com quatro condições)

## Contexto
Requisito futuro: um servidor local que funciona 48h sem internet e sincroniza com a cloud. O event sourcing puro é pesado para o prazo do PoC.

## Decisão
Uma tabela `pedido` (estado atual) e uma tabela `pedido_evento` (log), com tipos `CRIADO`, `ALTERADO`, `NA_COZINHA`, `PRONTO`, `ENTREGUE` e `ENVIADO_FATURACAO`.
1. **Mesma transação:** a linha em `pedido` e o evento gravam-se juntos. Um teste força uma falha a meio e confirma que nenhum dos dois fica gravado.
2. **Idempotente:** o `evento_id` é um UUID gerado no cliente, com restrição `UNIQUE` e `INSERT ... ON CONFLICT DO NOTHING`. O `pedido_id` também é um UUID gerado no cliente.
3. **Duas horas:** `ocorrido_em` vem do cliente e `recebido_em` do servidor. A ordem é dada por `recebido_em` e por uma `sequencia` dentro de cada pedido, nunca pelo relógio do cliente.
4. **Só inserts, imposto pela BD:** dois utilizadores da BD. `migracao` é o dono das tabelas e só o Flyway o usa. `app` é com quem o Spring Boot se liga e não é dono de nada: tem `SELECT, INSERT` em `pedido_evento` e nunca `UPDATE` nem `DELETE`. Teste de integração obrigatório: ligado como `app`, um `UPDATE` e um `DELETE` em `pedido_evento` têm de falhar com permissão negada. Credenciais dos dois só por variáveis de ambiente, nunca no Git.
- O evento `ALTERADO` guarda o pedido inteiro, como ficou (snapshot), e não só a diferença.
- A cozinha e o painel da dona leem o `pedido` e consultas sobre os eventos. Nunca escrevem eventos diretamente: só o módulo `pedidos` o faz.

## Alternativas
- Event sourcing puro (estado sempre calculado a partir dos eventos): adiado.
- Só a tabela `pedido`: perde a auditoria e a base para a sincronização.

## Consequências
A sincronização futura envia os eventos por `evento_id`, de forma idempotente. Há mais espaço ocupado por causa dos snapshots, o que é aceitável.
