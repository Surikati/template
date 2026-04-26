import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '@tmpmgmt/core';

import {
  CreateTemplateRequest,
  PublishVersionRequest,
  TemplateBundle,
  TemplateDraftResponse,
  TemplateResponse,
  TemplateVersionDiffResponse,
  TemplateVersionResponse,
  UpdateDraftRequest,
  UpdateMetadataRequest,
} from '../models/template';

@Injectable({ providedIn: 'root' })
export class TemplateApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private get base(): string {
    return `${this.config.apiBase}/templates`;
  }

  list(): Observable<TemplateResponse[]> {
    return this.http.get<TemplateResponse[]>(this.base);
  }

  get(id: string): Observable<TemplateResponse> {
    return this.http.get<TemplateResponse>(`${this.base}/${id}`);
  }

  create(req: CreateTemplateRequest): Observable<TemplateResponse> {
    return this.http.post<TemplateResponse>(this.base, req);
  }

  updateMetadata(id: string, req: UpdateMetadataRequest): Observable<TemplateResponse> {
    return this.http.put<TemplateResponse>(`${this.base}/${id}/metadata`, req);
  }

  getDraft(templateId: string): Observable<TemplateDraftResponse> {
    return this.http.get<TemplateDraftResponse>(`${this.base}/${templateId}/draft`);
  }

  saveDraft(templateId: string, req: UpdateDraftRequest): Observable<TemplateDraftResponse> {
    return this.http.put<TemplateDraftResponse>(`${this.base}/${templateId}/draft`, req);
  }

  listVersions(templateId: string): Observable<TemplateVersionResponse[]> {
    return this.http.get<TemplateVersionResponse[]>(`${this.base}/${templateId}/versions`);
  }

  publishVersion(
    templateId: string,
    req: PublishVersionRequest,
  ): Observable<TemplateVersionResponse> {
    return this.http.post<TemplateVersionResponse>(`${this.base}/${templateId}/versions`, req);
  }

  diffVersions(
    templateId: string,
    fromVersion: number,
    toVersion: number,
  ): Observable<TemplateVersionDiffResponse> {
    const params = new HttpParams()
      .set('from', String(fromVersion))
      .set('to', String(toVersion));
    return this.http.get<TemplateVersionDiffResponse>(`${this.base}/${templateId}/versions/diff`, {
      params,
    });
  }

  exportBundle(templateId: string): Observable<TemplateBundle> {
    return this.http.get<TemplateBundle>(`${this.base}/${templateId}/export`);
  }

  importBundle(bundle: TemplateBundle): Observable<TemplateResponse> {
    return this.http.post<TemplateResponse>(`${this.base}/import`, bundle);
  }

  archive(templateId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${templateId}/archive`, {});
  }
}
