import { TestBed } from '@angular/core/testing';

import { CardReplacementRequest } from './card-replacement-request';

describe('CardReplacementRequest', () => {
  let service: CardReplacementRequest;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CardReplacementRequest);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
