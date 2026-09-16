import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { NewCardRequestService } from './new-card-request';

describe('NewCardRequestService', () => {
  let service: NewCardRequestService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(NewCardRequestService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
