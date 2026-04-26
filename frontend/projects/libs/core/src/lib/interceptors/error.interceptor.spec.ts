import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { errorInterceptor } from './error.interceptor';
import { ProblemDetail } from '../models/problem-detail';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('passes successful responses through unchanged', (done) => {
    http.get<{ ok: boolean }>('/health').subscribe({
      next: (body) => {
        expect(body.ok).toBeTrue();
        done();
      },
    });
    httpMock.expectOne('/health').flush({ ok: true });
  });

  it('forwards a server-supplied ProblemDetail body verbatim', (done) => {
    const serverProblem: ProblemDetail = {
      type: 'about:blank',
      title: 'Conflict',
      status: 409,
      detail: 'Slug already in use',
      code: 'template.slug_taken',
    };
    http.get('/templates').subscribe({
      error: (err: ProblemDetail) => {
        expect(err).toEqual(serverProblem);
        done();
      },
    });
    httpMock
      .expectOne('/templates')
      .flush(serverProblem, { status: 409, statusText: 'Conflict' });
  });

  it('synthesises a ProblemDetail when the body has no code field', (done) => {
    http.get('/oops').subscribe({
      error: (err: ProblemDetail) => {
        expect(err.code).toBe('http.500');
        expect(err.status).toBe(500);
        expect(err.title).toBe('Server Error');
        done();
      },
    });
    httpMock
      .expectOne('/oops')
      .flush('opaque body', { status: 500, statusText: 'Server Error' });
  });

  it('synthesises a ProblemDetail when the response body is null', (done) => {
    http.get('/gone').subscribe({
      error: (err: ProblemDetail) => {
        expect(err.code).toBe('http.404');
        expect(err.status).toBe(404);
        done();
      },
    });
    httpMock.expectOne('/gone').flush(null, { status: 404, statusText: 'Not Found' });
  });
});
