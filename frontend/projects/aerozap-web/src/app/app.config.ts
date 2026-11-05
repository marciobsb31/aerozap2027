import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import {
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';
import { interceptadorDaApi, provideAerozapApi } from 'api-client';

import { ambiente } from '../ambientes/ambiente';
import { rotas } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      rotas,
      // Ao navegar, o foco e a rolagem voltam ao topo; manter a rolagem antiga faz a página nova
      // parecer vazia para quem usa leitor de tela.
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' }),
      // `?q=` chega na página como entrada do componente, sem assinar o ActivatedRoute à mão.
      withComponentInputBinding(),
    ),
    provideHttpClient(withInterceptors([interceptadorDaApi])),
    provideAerozapApi({ urlBase: ambiente.urlDaApi }),
  ],
};
