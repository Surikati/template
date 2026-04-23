import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '@tmpmgmt/core';

import { SearchHit, SearchHitType } from '../models/search';

@Injectable({ providedIn: 'root' })
export class SearchApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  search(query: string, type: SearchHitType | 'all' = 'all', limit = 20): Observable<SearchHit[]> {
    const params = new HttpParams()
      .set('q', query)
      .set('type', type)
      .set('limit', String(limit));
    return this.http.get<SearchHit[]>(`${this.config.apiBase}/search`, { params });
  }
}
