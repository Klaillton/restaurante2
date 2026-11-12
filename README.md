# Restaurante 2.0

[![verify](https://github.com/Klaillton/restaurante2/actions/workflows/verify.yml/badge.svg)](https://github.com/Klaillton/restaurante2/actions/workflows/verify.yml)
[![policy-base](https://github.com/Klaillton/restaurante2/actions/workflows/policy-base.yml/badge.svg)](https://github.com/Klaillton/restaurante2/actions/workflows/policy-base.yml)

Plataforma genérica de restauração, sem papel: a comanda no telemóvel, a cozinha no tablet e o painel da dona. Este repositório é o PoC — um único Spring Boot modular com PostgreSQL, para demonstração no portátil, em rede local e sem internet.

## Estado

PoC em curso, com prazo **18/11/2026**. O código atual é um esqueleto: monólito modular, esquema `pedido` / `pedido_evento`, Docker Compose e gates do gauntlet. Ainda não há endpoints HTTP nem ecrãs.

## Funcionalidades do PoC

Já no repositório:

- Monólito modular com os módulos `pedidos`, `cozinha`, `admin` e `partilhado` (fronteiras impostas por ArchUnit).
- Tabelas `pedido` (estado atual) e `pedido_evento` (registo imutável, só inserções), com IDs UUID gerados no cliente e tipos `CRIADO`, `ALTERADO`, `NA_COZINHA`, `PRONTO`, `ENTREGUE` e `ENVIADO_FATURACAO`.
- Dois utilizadores da base de dados: `migracao` (só o Flyway) e `app` (a aplicação; sem `UPDATE`/`DELETE` em `pedido_evento`).
- Arranque local com Docker Compose (PostgreSQL 17 + a aplicação na porta 8080).

Planeado / ainda não está no repositório:

- Ecrãs da comanda, da cozinha e do painel da dona (mencionados na especificação e nos ADR; não há frontend neste repo).
- Endpoints HTTP e autenticação desses ecrãs na rede local.
- Porta de exportação fiscal (`ExportacaoFiscal`): no PoC deve só gravar `ENVIADO_FATURACAO` e mostrar «enviado à faturação» — a interface ainda não existe no código ([ADR 0003](docs/adr/0003-porta-exportacao-fiscal.md), estado proposto).
- Licença com 7 dias sem rede ([ADR 0004](docs/adr/0004-licenca-7-dias-offline.md), explicitamente fora do PoC).
- Observabilidade para depois do PoC: logs estruturados, Actuator e métricas ([docs/sdd/Observability.md](docs/sdd/Observability.md)).
- Specs Gherkin (`features/`) e contrato OpenAPI (`openapi/`), ainda ausentes.

## Stack

- Java 21 e Spring Boot 3.5.16 (Web, Data JDBC, Flyway).
- PostgreSQL 17.
- Maven (`pom.xml` único; artifactId `restaurante2`, versão `0.1.0-SNAPSHOT`).
- Testcontainers (PostgreSQL) e ArchUnit 1.5.1 nos testes.
- Docker Compose e um Dockerfile em dois estágios (compilação com Maven 3.9.11 + JDK 21; execução num JRE 21, sem root).
- Qualidade: kit [ai-code-gauntlet](https://github.com/Klaillton/ai-code-gauntlet) ADD-POLY S1, fixado no SHA indicado em [ADOPT-STATUS.md](ADOPT-STATUS.md).

## Arquitectura

Um só processo Spring Boot (monólito modular, [ADR 0001](docs/adr/0001-monolito-modular.md)): não há microserviços no PoC. Os módulos vivem em pacotes Java sob `io.github.klaillton.restaurante`, não em módulos Maven separados.

- Cada módulo expõe só o subpacote `api`; o resto é interno.
- Um módulo só importa o `api` de outro; não há ciclos.
- `partilhado` tem só tipos de valor (IDs, `Dinheiro`, enums): não depende dos outros módulos e não leva anotações Spring nem JPA.

O pedido guarda o estado atual e um registo de eventos na mesma transação ([ADR 0002](docs/adr/0002-pedidos-estado-mais-log-de-eventos.md)). Só o módulo `pedidos` escreve eventos; a cozinha e o painel lêem o `pedido` e consultam o registo.

ADRs neste repositório:

- [0001 — Monólito modular](docs/adr/0001-monolito-modular.md) (aceite)
- [0002 — Pedido com estado mais log de eventos imutável](docs/adr/0002-pedidos-estado-mais-log-de-eventos.md) (aceite com revisão)
- [0003 — Porta de exportação fiscal](docs/adr/0003-porta-exportacao-fiscal.md) (proposto)
- [0004 — Licença com 7 dias sem rede](docs/adr/0004-licenca-7-dias-offline.md) (proposto, fora do PoC)

## Como correr

Pré-requisitos: Docker com Compose, JDK 21 e Maven (na CI: Temurin 21 e Maven 3.9.11). O `mvn verify` precisa de Docker, porque o gate de `pedido_evento` usa Testcontainers.

Arranque da demonstração local (rede local, sem internet — ver comentários em `docker-compose.yml`):

```bash
cp .env.example .env   # trocar TODOS os valores; o .env não vai para o Git
docker compose up -d --build
```

A aplicação fica em `http://localhost:8080`. O script de roles do Postgres só corre com o volume vazio; para recomeçar: `docker compose down -v`. Logs da app: `docker compose logs app`.

Verificar o build e os testes (inclui `FronteirasDosModulosTest` e `PedidoEventoSoInsertsGateTest`):

```bash
mvn -B verify
```

Não há neste repositório um frontend para servir, nem um comando documentado para o correr à parte do Compose.

## Estrutura do repositório

```
.
├── pom.xml
├── docker-compose.yml
├── Dockerfile
├── .env.example
├── src/main/java/io/github/klaillton/restaurante/
│   ├── pedidos/        # módulo pedidos (+ api)
│   ├── cozinha/        # módulo cozinha (+ api)
│   ├── admin/          # módulo admin (+ api)
│   └── partilhado/     # tipos de valor
├── src/main/resources/db/migration/   # Flyway (V1__pedido_e_pedido_evento.sql)
├── src/test/java/                     # gates ArchUnit e pedido_evento
├── docker/postgres/init/              # criação dos roles (volume vazio)
├── docs/adr/                          # ADR 0001–0004
├── docs/sdd/                          # Security.md, Observability.md
├── .github/workflows/                 # verify.yml, policy-base.yml
├── gauntlet.config.json
├── ADOPT-STATUS.md
└── SECURITY.md
```

## Qualidade e gauntlet

A `main` está protegida. Num pull request, os checks obrigatórios são:

- `mvn verify (JDK 21)` — workflow [verify](.github/workflows/verify.yml)
- `gauntlet (kit S1)` — o mesmo workflow
- `policy-base (base-run, PR)` — workflow [policy-base](.github/workflows/policy-base.yml)
- `reviewer/aprovado`

O kit e o que o S1 cobre estão descritos em [ADOPT-STATUS.md](ADOPT-STATUS.md). Regras para agentes: [AGENTS.md](AGENTS.md). Nunca enfraquecer os GRANT/REVOKE de `pedido_evento` nem o teste que os prova; alterações a uma migração já aplicada vão numa versão Flyway nova, nunca no ficheiro já aplicado.

## Segurança

Zero segredos no Git (repositório público). Credenciais só por variáveis de ambiente: copiar `.env.example` para `.env` (o `.env` está no `.gitignore`) e trocar todos os valores.

Para reportar uma vulnerabilidade, usa um [aviso privado de segurança no GitHub](https://github.com/Klaillton/restaurante2/security/advisories/new). Não abras uma issue pública com detalhes exploráveis. Ver [SECURITY.md](SECURITY.md) e [docs/sdd/Security.md](docs/sdd/Security.md).

## Contribuição

Não são aceites contribuições externas.

O fluxo de pull request abaixo aplica-se apenas aos responsáveis e agentes do projeto. Qualquer contribuição externa futura exigiria um CLA (acordo de licença de contribuinte).

Numa alteração interna, trabalha numa branch e abre um pull request contra a `main`. A `main` é protegida: o merge só depois dos checks obrigatórios acima. Não faças push directo para a `main`.

## Licença

Copyright (c) 2026 Klaillton Santos. All rights reserved.

O código está público para consulta. Além do previsto nos Termos de Serviço do GitHub, não é concedida nenhuma licença para usar, copiar, modificar ou distribuir este código sem autorização escrita.
