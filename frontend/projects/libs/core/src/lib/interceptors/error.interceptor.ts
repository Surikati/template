import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ProblemDetail } from '../models/problem-detail';

/** Translates HTTP error responses into ProblemDetail-shaped errors. */
export const errorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const problem: ProblemDetail =
        err.error && typeof err.error === 'object' && 'code' in err.error
          ? (err.error as ProblemDetail)
          : {
              type: 'about:blank',
              title: err.statusText,
              status: err.status,
              detail: err.message,
              code: 'http.' + err.status,
            };
      return throwError(() => problem);
    }),
  );
