# ADR 0003 — Porta de exportação fiscal

Estado: proposto (12/11/2026)

## Contexto
A faturação certificada (AT, SAF-T) fica fora do PoC, mas cada país e cada cliente vão querer o seu programa de faturação.

## Decisão
A interface `ExportacaoFiscal` fica em `pedidos.api`. No PoC, a implementação `ExportacaoFiscalRegisto` só grava o evento `ENVIADO_FATURACAO` e mostra "enviado à faturação". Não há dados fiscais reais.

## Alternativas
Integrar já um programa certificado: fora do prazo e prende o produto a um fornecedor.

## Consequências
A integração real entra como um novo adaptador, sem mexer no domínio.
