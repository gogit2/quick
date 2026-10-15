import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

import { ApiError } from './models';

// Typed error for every failed API call. `body` is the ApiError body when the server sent one.
export class ApiRequestError extends Error {
  constructor(
    readonly status: number,
    readonly body: ApiError | null,
  ) {
    super(body?.message ?? `Request failed with status ${status}`);
    this.name = 'ApiRequestError';
  }
}

function isApiError(value: unknown): value is ApiError {
  return typeof value === 'object' && value !== null && 'fieldErrors' in value && 'status' in value;
}

// Converts ApiError bodies into ApiRequestError and shows a snackbar for 5xx responses.
export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) {
        return throwError(() => err);
      }
      const body = isApiError(err.error) ? err.error : null;
      if (err.status >= 500) {
        snackBar.open(body?.message ?? 'The server had a problem. Please try again.', 'Dismiss', {
          duration: 6000,
        });
      }
      return throwError(() => new ApiRequestError(err.status, body));
    }),
  );
};
