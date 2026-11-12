# ADR 0004 — Licença com 7 dias sem rede

Estado: proposto, fora do PoC (12/11/2026)

## Contexto
Instalação local, com subscrição paga na cloud. A internet do restaurante falha de vez em quando, e o restaurante não pode parar por isso.

## Decisão
A app valida a licença online quando tem rede e guarda localmente a última validação bem-sucedida, assinada pelo servidor. Só bloqueia ao fim de 7 dias sem conseguir validar. A partir do 5.º dia mostra um aviso no painel da dona.

## Alternativas
- Validação diária obrigatória: rejeitada, porque pára o restaurante exatamente quando a internet cai.

## Consequências
Precisa de um par de chaves do servidor de licenças. Não serve um relógio monotónico, porque volta a zero quando o PC reinicia. Fica guardada a última validação, assinada pelo servidor, e a hora mais alta já vista no PC (gravada de forma persistente). Se o relógio do PC ficar abaixo dessa hora, conta como adulteração: a app avisa e não ganha tempo. A detalhar depois do PoC.
