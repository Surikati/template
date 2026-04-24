import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '@tmpmgmt/core';

import {
  CreateQuestionnaireRequest,
  EvaluateRulesRequest,
  EvaluateRulesResponse,
  QuestionnaireResponse,
  ReplaceStructureRequest,
  SessionResponse,
  StartSessionRequest,
  SubmitAnswersRequest,
} from '../models/questionnaire';

@Injectable({ providedIn: 'root' })
export class QuestionnaireApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private get base(): string {
    return `${this.config.apiBase}/questionnaires`;
  }

  private get sessionBase(): string {
    return `${this.config.apiBase}/questionnaire-sessions`;
  }

  get(id: string): Observable<QuestionnaireResponse> {
    return this.http.get<QuestionnaireResponse>(`${this.base}/${id}`);
  }

  /** Returns 404-as-undefined upstream — caller should handle the empty case. */
  findByTemplateVersion(
    templateId: string,
    versionNumber: number,
  ): Observable<QuestionnaireResponse> {
    const params = new HttpParams()
      .set('templateId', templateId)
      .set('versionNumber', versionNumber);
    return this.http.get<QuestionnaireResponse>(`${this.base}/by-template-version`, { params });
  }

  create(req: CreateQuestionnaireRequest): Observable<QuestionnaireResponse> {
    return this.http.post<QuestionnaireResponse>(this.base, req);
  }

  replaceStructure(
    id: string,
    req: ReplaceStructureRequest,
  ): Observable<QuestionnaireResponse> {
    return this.http.put<QuestionnaireResponse>(`${this.base}/${id}/structure`, req);
  }

  getSession(id: string): Observable<SessionResponse> {
    return this.http.get<SessionResponse>(`${this.sessionBase}/${id}`);
  }

  startSession(req: StartSessionRequest): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(this.sessionBase, req);
  }

  submitAnswers(id: string, req: SubmitAnswersRequest): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(`${this.sessionBase}/${id}/answers`, req);
  }

  completeSession(id: string): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(`${this.sessionBase}/${id}/complete`, {});
  }

  evaluateRules(req: EvaluateRulesRequest): Observable<EvaluateRulesResponse> {
    return this.http.post<EvaluateRulesResponse>(`${this.base}/evaluate-rules`, req);
  }
}
