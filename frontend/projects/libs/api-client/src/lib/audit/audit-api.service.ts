import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '@tmpmgmt/core';

import { AuditEventResponse, AuditQuery } from '../models/audit';

@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private get base(): string {
    return `${this.config.apiBase}/audit/events`;
  }

  list(query: AuditQuery = {}): Observable<AuditEventResponse[]> {
    let params = new HttpParams();
    if (query.aggregateType) params = params.set('aggregateType', query.aggregateType);
    if (query.aggregateId) params = params.set('aggregateId', query.aggregateId);
    if (query.actorUserId) params = params.set('actorUserId', query.actorUserId);
    if (query.limit !== undefined) params = params.set('limit', query.limit);
    return this.http.get<AuditEventResponse[]>(this.base, { params });
  }
}
