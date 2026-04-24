import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '@tmpmgmt/core';

import {
  ClauseResponse,
  ClauseVersionResponse,
  CreateClauseRequest,
  PublishClauseVersionRequest,
  UpdateClauseMetadataRequest,
} from '../models/clause';

@Injectable({ providedIn: 'root' })
export class ClauseApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private get base(): string {
    return `${this.config.apiBase}/clauses`;
  }

  list(): Observable<ClauseResponse[]> {
    return this.http.get<ClauseResponse[]>(this.base);
  }

  get(id: string): Observable<ClauseResponse> {
    return this.http.get<ClauseResponse>(`${this.base}/${id}`);
  }

  create(req: CreateClauseRequest): Observable<ClauseResponse> {
    return this.http.post<ClauseResponse>(this.base, req);
  }

  updateMetadata(id: string, req: UpdateClauseMetadataRequest): Observable<ClauseResponse> {
    return this.http.put<ClauseResponse>(`${this.base}/${id}/metadata`, req);
  }

  listVersions(clauseId: string): Observable<ClauseVersionResponse[]> {
    return this.http.get<ClauseVersionResponse[]>(`${this.base}/${clauseId}/versions`);
  }

  getVersion(clauseId: string, versionNumber: number): Observable<ClauseVersionResponse> {
    return this.http.get<ClauseVersionResponse>(
      `${this.base}/${clauseId}/versions/${versionNumber}`,
    );
  }

  publishVersion(
    clauseId: string,
    req: PublishClauseVersionRequest,
  ): Observable<ClauseVersionResponse> {
    return this.http.post<ClauseVersionResponse>(`${this.base}/${clauseId}/versions`, req);
  }

  archive(clauseId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${clauseId}/archive`, {});
  }
}
