import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Transactions } from './transactions';

describe('Transactions', () => {
  let component: Transactions;
  let fixture: ComponentFixture<Transactions>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Transactions],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Transactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('searches generated transaction IDs and exposes the matching record details', () => {
    const http = TestBed.inject(HttpTestingController);
    component.transactionLookupQuery = 'TXN-123';

    component.searchTransactionId();

    const request = http.expectOne(req =>
      req.url.endsWith('/api/admin/search/transactions') && req.params.get('q') === 'TXN-123'
    );
    request.flush({
      success: true,
      results: [{
        type: 'Transaction',
        transactionId: 'TXN-123',
        transactionType: 'Transfer',
        transferType: 'NEFT',
        senderName: 'Sender',
        senderAccountNumber: '1001',
        receiverName: 'Receiver',
        receiverAccountNumber: '2002',
        status: 'Completed',
        date: '2026-10-08T10:00:00'
      }]
    });

    expect(component.transactionLookupResults.length).toBe(1);
    expect(component.getLookupRecordId(component.transactionLookupResults[0])).toBe('TXN-123');
    expect(component.getLookupSenderAccount(component.transactionLookupResults[0])).toBe('1001');
    expect(component.getLookupReceiverAccount(component.transactionLookupResults[0])).toBe('2002');
    expect(component.isSearchingTransaction).toBeFalse();
  });
});
