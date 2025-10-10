import { TestBed } from '@angular/core/testing';

import { NewCardRequest } from './new-card-request';

describe('NewCardRequest', () => {
  let service: NewCardRequest;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NewCardRequest);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
