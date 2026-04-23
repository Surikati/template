import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '@tmpmgmt/core';

import { AssembleRequest, AssembleResponse } from '../models/assembly';

@Injectable({ providedIn: 'root' })
export class AssemblyApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private get base(): string {
    return `${this.config.apiBase}/assemblies`;
  }

  assemble(req: AssembleRequest): Observable<AssembleResponse> {
    return this.http.post<AssembleResponse>(this.base, req);
  }

  get(jobId: string): Observable<AssembleResponse> {
    return this.http.get<AssembleResponse>(`${this.base}/${jobId}`);
  }

  /**
   * Turns a relative path from document-service (e.g. `/api/v1/documents/.../files/DOCX`)
   * into an absolute URL pointing at the same gateway origin as {@code apiBase}.
   */
  resolveDownloadUrl(relativePath: string): string {
    const origin = new URL(this.config.apiBase).origin;
    return origin + relativePath;
  }
}
