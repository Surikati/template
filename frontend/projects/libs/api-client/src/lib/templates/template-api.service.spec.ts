import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '@tmpmgmt/core';
import { TemplateApiService } from './template-api.service';

describe('TemplateApiService', () => {
  let service: TemplateApiService;
  let httpMock: HttpTestingController;
  const apiBase = '/api/v1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: { apiBase } },
      ],
    });
    service = TestBed.inject(TemplateApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list() issues GET /templates', () => {
    service.list().subscribe();
    const req = httpMock.expectOne(`${apiBase}/templates`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('get(id) issues GET /templates/:id', () => {
    service.get('abc').subscribe();
    const req = httpMock.expectOne(`${apiBase}/templates/abc`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('create() issues POST /templates with the request body', () => {
    const body = { slug: 'nda', name: 'NDA', category: 'legal' };
    service.create(body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/templates`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('updateMetadata() issues PUT /templates/:id/metadata', () => {
    const body = { name: 'New', category: 'legal', tags: ['a'] };
    service.updateMetadata('abc', body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/templates/abc/metadata`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('getDraft() issues GET /templates/:id/draft', () => {
    service.getDraft('abc').subscribe();
    const req = httpMock.expectOne(`${apiBase}/templates/abc/draft`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('saveDraft() issues PUT /templates/:id/draft with the request body', () => {
    const body = { content: { type: 'doc', content: [] }, variablesSchema: { type: 'object' } };
    service.saveDraft('abc', body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/templates/abc/draft`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('listVersions() issues GET /templates/:id/versions', () => {
    service.listVersions('abc').subscribe();
    const req = httpMock.expectOne(`${apiBase}/templates/abc/versions`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('publishVersion() issues POST /templates/:id/versions with the request body', () => {
    const body = { changeNote: 'first cut' };
    service.publishVersion('abc', body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/templates/abc/versions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('archive() issues POST /templates/:id/archive with empty body', () => {
    service.archive('abc').subscribe();
    const req = httpMock.expectOne(`${apiBase}/templates/abc/archive`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(null);
  });
});
