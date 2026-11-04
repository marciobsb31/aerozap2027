import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';

import { AEROZAP_API_URL_BASE } from './configuracao';
import { problemaDe } from './problema';
import type { RespostaDePing } from './tipos';

/**
 * Cliente da API do AeroZap.
 *
 * Os tipos vêm do contrato OpenAPI (`docs/api-contracts/openapi/aerozap-v0.yaml`) por geração, e
 * não escritos à mão: mudar o contrato sem mudar o cliente para de compilar, e a CI verifica que o
 * arquivo gerado continua igual ao contrato.
 *
 * Só há um método porque só há um endpoint. Cada novo endpoint entra primeiro no contrato, depois
 * aqui — nunca o contrário.
 */
@Injectable({ providedIn: 'root' })
export class AerozapApi {
  private readonly http = inject(HttpClient);
  private readonly urlBase = inject(AEROZAP_API_URL_BASE);

  /** Confirma que a plataforma responde atrás do gateway. */
  ping(): Observable<{ servico: string; estado: string; correlacao: string }> {
    return this.http
      .get<RespostaDePing>(`${this.urlBase}/api/plataforma/v1/system/ping`)
      .pipe(
        map((resposta) => ({
          servico: resposta.data.service,
          estado: resposta.data.status,
          correlacao: resposta.meta.correlationId,
        })),
        catchError((erro: unknown) => throwError(() => problemaDe(erro))),
      );
  }
}
