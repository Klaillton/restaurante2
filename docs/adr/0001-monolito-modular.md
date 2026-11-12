# ADR 0001 — Monólito modular

Estado: aceite (12/11/2026, Challenge: aceita)

## Contexto
PoC até 18/11, código gerado pelo Grok CLI. Microserviços gastariam o prazo em infraestrutura e espalhariam erros por vários serviços.

## Decisão
Um só Spring Boot com os módulos `pedidos`, `cozinha`, `admin` e `partilhado`, sob `io.github.klaillton.restaurante`.
- Cada módulo expõe só o subpacote `api`. Tudo o resto é interno.
- O `partilhado` contém só tipos de valor (IDs, `Dinheiro`, enums). Não pode ter `@Entity`, `@Service`, `@Repository` nem `@Component`, e não depende de nenhum outro módulo.
- Regras ArchUnit no build, que o fazem falhar:
  1. Um módulo só importa o `api` de outro módulo.
  2. O `partilhado` não depende de `pedidos`, `cozinha` nem `admin`, e não tem anotações Spring nem JPA.
  3. Não há ciclos entre módulos.

## Alternativas
- Spring Modulith: mais completo, mas é mais uma dependência a aprender em 6 dias. Pode substituir o ArchUnit depois.
- Microserviços: rejeitados para o PoC.

## Consequências
Separar um módulo num serviço próprio mais tarde é barato, porque as fronteiras já existem. O ArchUnit tem de correr no `mvn verify`.
