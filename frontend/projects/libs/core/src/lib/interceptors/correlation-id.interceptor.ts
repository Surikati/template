import { HttpInterceptorFn } from '@angular/common/http';

/** Attaches a per-request correlation ID so backend logs across services can be stitched together. */
export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = crypto.randomUUID();
  return next(req.clone({ setHeaders: { 'X-Correlation-Id': correlationId } }));
};
