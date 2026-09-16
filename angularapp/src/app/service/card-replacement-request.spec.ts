import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { CardReplacementRequestService } from './card-replacement-request';

describe('CardReplacementRequestService', () => {
  let service: CardReplacementRequestService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CardReplacementRequestService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
