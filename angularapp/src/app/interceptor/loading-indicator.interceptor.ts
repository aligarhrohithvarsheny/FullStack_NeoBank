import { HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { finalize } from 'rxjs';
import { LoadingIndicatorService } from '../service/loading-indicator.service';

export const loadingIndicatorInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isPlatformBrowser(inject(PLATFORM_ID))) return next(request);

  const loading = inject(LoadingIndicatorService);
  const contentType = request.headers.get('Content-Type')?.toLowerCase() || '';
  const isFormData = typeof FormData !== 'undefined' && request.body instanceof FormData;
  const isBlob = typeof Blob !== 'undefined' && request.body instanceof Blob;
  const isUpload = isFormData || isBlob || contentType.startsWith('multipart/form-data');

  loading.start(isUpload);
  return next(request).pipe(finalize(() => loading.finish(isUpload)));
};
