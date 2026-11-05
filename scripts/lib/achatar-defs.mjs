#!/usr/bin/env node
// Prepara os JSON Schemas publicados para a geração do cliente TypeScript.
//
// Por que existe: o openapi-typescript trata `$defs` como se fosse propriedade do objeto, e o
// cliente gerado passa a exigir um campo `$defs` que nenhuma resposta tem. Em vez de mudar os
// contratos publicados (que já estão validados pelos testes em Java e são a fonte de verdade dos
// eventos), resolvemos os `$ref` internos numa cópia temporária e apagamos o `$defs` dela.
//
// Uso: node achatar-defs.mjs <diretório de origem> <diretório de destino>

import { mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { basename, join } from 'node:path';

const [origem, destino] = process.argv.slice(2);
if (!origem || !destino) {
  console.error('Uso: achatar-defs.mjs <origem> <destino>');
  process.exit(2);
}

mkdirSync(destino, { recursive: true });

/** Resolve um ponteiro `#/$defs/x` dentro do próprio documento. */
function apontar(documento, ponteiro) {
  const partes = ponteiro.replace(/^#\//, '').split('/');
  let atual = documento;
  for (const parte of partes) {
    atual = atual?.[parte.replace(/~1/g, '/').replace(/~0/g, '~')];
    if (atual === undefined) {
      throw new Error(`Ponteiro não resolvido: ${ponteiro}`);
    }
  }
  return atual;
}

/**
 * Troca todo `$ref` interno pelo conteúdo apontado. A profundidade é limitada porque um esquema
 * recursivo não pode ser achatado: se aparecer um, o erro é melhor do que o laço infinito.
 */
function inlinar(no, documento, profundidade = 0) {
  if (profundidade > 20) {
    throw new Error('Esquema recursivo: não é possível achatar.');
  }
  if (Array.isArray(no)) {
    return no.map((item) => inlinar(item, documento, profundidade + 1));
  }
  if (no === null || typeof no !== 'object') {
    return no;
  }
  if (typeof no.$ref === 'string' && no.$ref.startsWith('#/$defs/')) {
    const { $ref, ...resto } = no;
    return { ...inlinar(apontar(documento, $ref), documento, profundidade + 1), ...resto };
  }
  const saida = {};
  for (const [chave, valor] of Object.entries(no)) {
    saida[chave] = inlinar(valor, documento, profundidade + 1);
  }
  return saida;
}

let tratados = 0;
for (const arquivo of readdirSync(origem).filter((nome) => nome.endsWith('.json'))) {
  const documento = JSON.parse(readFileSync(join(origem, arquivo), 'utf8'));
  const achatado = inlinar(documento, documento);
  delete achatado.$defs;
  writeFileSync(join(destino, basename(arquivo)), `${JSON.stringify(achatado, null, 2)}\n`);
  tratados += 1;
}

console.log(`Esquemas preparados: ${tratados}`);
