import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { LoadingIndicatorService } from './loading-indicator.service';

describe('LoadingIndicatorService', () => {
  let service: LoadingIndicatorService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [LoadingIndicatorService] });
    service = TestBed.inject(LoadingIndicatorService);
  });

  it('delays the branded preloader for short requests and keeps it visible briefly for long requests', fakeAsync(() => {
    service.start();
    tick(200);
    service.finish();
    tick(500);
    expect(service.isVisible()).toBeFalse();

    service.start();
    tick(250);
    expect(service.isVisible()).toBeTrue();
    service.finish();
    tick(399);
    expect(service.isVisible()).toBeTrue();
    tick(1);
    expect(service.isVisible()).toBeFalse();
  }));

  it('keeps the upload bar active until all concurrent file uploads finish', () => {
    service.start(true);
    service.start(true);
    expect(service.isUploadActive()).toBeTrue();

    service.finish(true);
    expect(service.isUploadActive()).toBeTrue();
    service.finish(true);
    expect(service.isUploadActive()).toBeFalse();
  });
});
