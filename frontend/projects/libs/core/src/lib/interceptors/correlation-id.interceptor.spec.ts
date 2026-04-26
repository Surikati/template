import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { correlationIdInterceptor } from './correlation-id.interceptor';

describe('correlationIdInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([correlationIdInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('attaches an X-Correlation-Id header in UUID v4 shape', () => {
    http.get('/foo').subscribe();
    const req = httpMock.expectOne('/foo');
    const header = req.request.headers.get('X-Correlation-Id');
    expect(header).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
    req.flush({});
  });

  it('sends a different correlation id per request', () => {
    http.get('/a').subscribe();
    http.get('/b').subscribe();
    const r1 = httpMock.expectOne('/a');
    const r2 = httpMock.expectOne('/b');
    expect(r1.request.headers.get('X-Correlation-Id')).not.toBe(
      r2.request.headers.get('X-Correlation-Id'),
    );
    r1.flush({});
    r2.flush({});
  });
});
