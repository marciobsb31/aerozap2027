/**
 * Configuração de ambiente da aerozap-web.
 *
 * Um arquivo só, sem troca por build: o endereço da API é o do gateway local em desenvolvimento e
 * vem substituído no artefato publicado. Isso evita o padrão de `environment.prod.ts` que cedo ou
 * tarde sobe para produção com o valor de desenvolvimento dentro.
 */
export const ambiente = {
  urlDaApi: 'http://localhost:8080',
} as const;
