import { Routes } from '@angular/router';

/**
 * As páginas são carregadas sob demanda: o pacote inicial fica com a casca e o resto chega quando
 * a pessoa navega. Com duas telas o ganho é pequeno; o ponto é que o padrão já esteja posto antes
 * de existirem vinte.
 */
export const rotas: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./paginas/inicio/inicio').then((m) => m.Inicio),
  },
  {
    path: 'busca',
    loadComponent: () => import('./paginas/busca/busca').then((m) => m.Busca),
  },
  {
    path: '**',
    loadComponent: () =>
      import('./paginas/nao-encontrada/nao-encontrada').then((m) => m.NaoEncontrada),
  },
];
