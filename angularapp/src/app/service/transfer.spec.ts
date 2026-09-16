import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { TransferService } from './transfer';

describe('TransferService', () => {
  let service: TransferService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(TransferService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
