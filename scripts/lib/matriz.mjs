#!/usr/bin/env node
// Verifica e publica a matriz de rastreabilidade (AZ-018).
//
// A matriz só vale alguma coisa se não puder mentir. Uma tabela escrita à mão envelhece em
// silêncio: o requisito entra no PRD e ninguém acrescenta a linha; o teste é renomeado e a
// evidência vira um caminho morto; alguém marca COBERTO sem teste nenhum. Este arquivo transforma
// cada uma dessas três coisas em falha de CI.
//
// Uso:
//   node matriz.mjs verificar        confere a matriz contra o PRD e o disco (sai 1 se algo falha)
//   node matriz.mjs publicar         regrava docs/architecture/matriz-de-rastreabilidade.md
//   node matriz.mjs publicar --conferir  falha se o documento publicado está desatualizado

import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const RAIZ = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const PRD = join(RAIZ, 'docs/prd/PRD-v1.md');
const MATRIZ = join(RAIZ, 'tests/rastreabilidade/matriz.json');
const DOCUMENTO = join(RAIZ, 'docs/architecture/matriz-de-rastreabilidade.md');

const STATUS_VALIDOS = ['COBERTO', 'PARCIAL', 'PENDENTE'];

function requisitosDoPrd() {
  const linhas = readFileSync(PRD, 'utf8').split('\n');
  const achados = new Map();
  for (const linha of linhas) {
    const casamento = /^\|\s*(RF-\d{3})\s*\|([^|]*)\|([^|]*)\|([^|]*)\|/.exec(linha.trim());
    if (casamento) {
      achados.set(casamento[1], {
        titulo: casamento[2].trim(),
        prioridade: casamento[3].trim(),
        marco: casamento[4].trim(),
      });
    }
  }
  return achados;
}

function verificar() {
  const problemas = [];
  const prd = requisitosDoPrd();
  const matriz = JSON.parse(readFileSync(MATRIZ, 'utf8'));
  const naMatriz = new Map(matriz.requisitos.map((r) => [r.id, r]));

  if (prd.size === 0) {
    problemas.push('Nenhum requisito encontrado no PRD — o formato da tabela mudou?');
  }

  for (const [id, doPrd] of prd) {
    const registro = naMatriz.get(id);
    if (!registro) {
      problemas.push(`${id} está no PRD e falta na matriz.`);
      continue;
    }
    if (registro.titulo !== doPrd.titulo) {
      problemas.push(`${id}: o título mudou no PRD e a matriz ficou para trás.\n`
        + `    PRD:    ${doPrd.titulo}\n    matriz: ${registro.titulo}`);
    }
    if (registro.marco !== doPrd.marco) {
      problemas.push(`${id}: marco ${registro.marco} na matriz, ${doPrd.marco} no PRD.`);
    }
  }

  for (const [id, registro] of naMatriz) {
    if (!prd.has(id)) {
      problemas.push(`${id} está na matriz e não existe no PRD.`);
    }
    if (!STATUS_VALIDOS.includes(registro.status)) {
      problemas.push(`${id}: status "${registro.status}" não existe.`);
    }
    const testes = registro.testes ?? [];
    if (registro.status !== 'PENDENTE' && testes.length === 0) {
      problemas.push(`${id} está como ${registro.status} sem nenhum teste. `
        + 'Status sem evidência é o que faz a matriz mentir.');
    }
    if (registro.status === 'PENDENTE' && testes.length > 0) {
      problemas.push(`${id} está PENDENTE mas aponta teste; promova para PARCIAL ou COBERTO.`);
    }
    for (const caminho of testes) {
      if (!existsSync(join(RAIZ, caminho))) {
        problemas.push(`${id}: o teste "${caminho}" não existe no repositório.`);
      }
    }
    if (!registro.nota || registro.nota.trim() === '') {
      problemas.push(`${id}: sem nota. A nota é o que diz ao próximo o que falta.`);
    }
  }

  if (problemas.length > 0) {
    console.error(`Matriz de rastreabilidade: ${problemas.length} problema(s).\n`);
    problemas.forEach((p) => console.error(`  - ${p}`));
    console.error('\nCorrija em tests/rastreabilidade/matriz.json e rode:');
    console.error('  bash scripts/rastreabilidade.sh');
    return false;
  }

  const total = naMatriz.size;
  const conta = (status) => [...naMatriz.values()].filter((r) => r.status === status).length;
  console.log(`Matriz íntegra: ${total} requisitos — ${conta('COBERTO')} cobertos, `
    + `${conta('PARCIAL')} parciais, ${conta('PENDENTE')} pendentes.`);
  return true;
}

function documento() {
  const matriz = JSON.parse(readFileSync(MATRIZ, 'utf8'));
  const conta = (status) => matriz.requisitos.filter((r) => r.status === status).length;
  const simbolo = { COBERTO: '●', PARCIAL: '◐', PENDENTE: '○' };

  const porMarco = new Map();
  for (const r of matriz.requisitos) {
    if (!porMarco.has(r.marco)) porMarco.set(r.marco, []);
    porMarco.get(r.marco).push(r);
  }
  // Marcos reais primeiro, em ordem; o resto (requisito bloqueado por decisão) vai para o fim.
  const ehMarco = (m) => /^M\d+$/.test(m);
  const marcos = [...porMarco.keys()].sort((a, b) => {
    if (ehMarco(a) !== ehMarco(b)) return ehMarco(a) ? -1 : 1;
    return a.localeCompare(b);
  });

  const linhas = [];
  linhas.push('# Matriz de rastreabilidade — requisito → teste → evidência');
  linhas.push('');
  linhas.push('> **Arquivo gerado.** A fonte é `tests/rastreabilidade/matriz.json`; este documento');
  linhas.push('> sai dela por `bash scripts/rastreabilidade.sh`. Editar aqui é trabalho perdido:');
  linhas.push('> a CI regera e compara.');
  linhas.push('');
  linhas.push(`Requisitos do PRD: **${matriz.requisitos.length}**. `
    + `Cobertos **${conta('COBERTO')}**, parciais **${conta('PARCIAL')}**, `
    + `pendentes **${conta('PENDENTE')}**. Atualizada em ${matriz.atualizadoEm}.`);
  linhas.push('');
  linhas.push('A contagem não é nota de desempenho. O produto está no marco M1 e a maior parte dos');
  linhas.push('requisitos ainda não tem código — um requisito pendente com nota honesta vale mais do');
  linhas.push('que um verde que não corresponde a teste nenhum. O que a matriz garante é que nenhum');
  linhas.push('requisito some do radar e que nenhum status exista sem arquivo de teste que o sustente.');
  linhas.push('');
  linhas.push('| Símbolo | Status | Significado |');
  linhas.push('| --- | --- | --- |');
  for (const [status, texto] of Object.entries(matriz.legenda)) {
    linhas.push(`| ${simbolo[status]} | ${status} | ${texto} |`);
  }
  linhas.push('');

  for (const marco of marcos) {
    const doMarco = porMarco.get(marco);
    linhas.push(`## ${ehMarco(marco) ? marco : `Sem marco — ${marco.replace(/^—\s*/, '')}`}`);
    linhas.push('');
    linhas.push('| | Requisito | Prio | Teste | Nota |');
    linhas.push('| --- | --- | --- | --- | --- |');
    for (const r of doMarco) {
      const testes = (r.testes ?? []).length === 0
        ? '—'
        : r.testes.map((t) => `\`${t.split('/').pop()}\``).join('<br>');
      linhas.push(`| ${simbolo[r.status]} | **${r.id}** ${escapar(r.titulo)} | ${r.prioridade} `
        + `| ${testes} | ${escapar(r.nota)} |`);
    }
    linhas.push('');
  }

  linhas.push('## Caminhos completos dos testes citados');
  linhas.push('');
  const todos = [...new Set(matriz.requisitos.flatMap((r) => r.testes ?? []))].sort();
  for (const caminho of todos) {
    linhas.push(`- \`${caminho}\``);
  }
  linhas.push('');
  return linhas.join('\n');
}

function escapar(texto) {
  return texto.replaceAll('|', '\\|');
}

const comando = process.argv[2];
if (comando === 'verificar') {
  process.exit(verificar() ? 0 : 1);
} else if (comando === 'publicar') {
  const novo = documento();
  if (process.argv.includes('--conferir')) {
    const atual = existsSync(DOCUMENTO) ? readFileSync(DOCUMENTO, 'utf8') : '';
    if (atual !== novo) {
      console.error('O documento publicado está desatualizado em relação à matriz.');
      console.error('Rode: bash scripts/rastreabilidade.sh');
      process.exit(1);
    }
    console.log('Documento em dia com a matriz.');
  } else {
    writeFileSync(DOCUMENTO, novo);
    console.log(`Escrito: ${DOCUMENTO.slice(RAIZ.length + 1)}`);
  }
} else {
  console.error('Uso: node matriz.mjs verificar | publicar [--conferir]');
  process.exit(2);
}
