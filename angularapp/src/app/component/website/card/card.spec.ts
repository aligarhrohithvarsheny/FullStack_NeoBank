import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Card } from './card';

describe('Card', () => {
  let component: Card;
  let fixture: ComponentFixture<Card>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Card],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Card);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    fixture.destroy();
    httpMock.verify();
    sessionStorage.removeItem('jwt');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display an active debit card instead of the first account card', () => {
    component.accountNumberInput = 'ACC123';
    fixture.detectChanges();

    httpMock.expectOne(request => request.url.endsWith('/api/cards/account/ACC123')).flush([
      { id: 8, cardType: 'Credit', status: 'Active', cardNumber: '4111' },
      { id: 9, cardType: 'Visa Debit', status: 'Blocked', cardNumber: '5222' },
      { id: 10, cardType: 'Debit', status: 'Active', cardNumber: '6333' }
    ]);
    httpMock.expectOne(request => request.url.endsWith('/api/cards/10/user-action-history')).flush([]);

    expect(component.card?.id).toBe('10');
    expect(component.card?.cardType).toBe('Debit');
  });

  it('should not show a credit card as a debit card when no debit card exists', () => {
    component.accountNumberInput = 'ACC123';
    fixture.detectChanges();

    httpMock.expectOne(request => request.url.endsWith('/api/cards/account/ACC123')).flush([
      { id: 8, cardType: 'Credit', status: 'Active', cardNumber: '4111' }
    ]);
    fixture.detectChanges();

    expect(component.card).toBeNull();
    expect(fixture.nativeElement.querySelector('.card-container')).toBeNull();
    expect(fixture.nativeElement.querySelector('.card-actions')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('No debit card is currently linked');
  });

  it('should require an explicit action and save generated debit card history', () => {
    component.accountNumberInput = 'ACC123';
    fixture.detectChanges();

    httpMock.expectOne(request => request.url.endsWith('/api/cards/account/ACC123')).flush([]);
    fixture.detectChanges();

    expect(component.card).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Generate Debit Card');

    sessionStorage.setItem('jwt', 'test-token');
    component.generateDebitCard();
    const generateRequest = httpMock.expectOne(request => request.url.endsWith('/api/cards/generate/debit'));
    expect(generateRequest.request.method).toBe('POST');
    expect(generateRequest.request.headers.get('Authorization')).toBe('Bearer test-token');
    generateRequest.flush({
      id: 21,
      accountNumber: 'ACC123',
      userName: 'Test User',
      cardNumber: '4111111111111111',
      cardType: 'Visa Debit',
      cvv: '123',
      expiryDate: '06/29',
      status: 'Active',
      pinSet: false,
      createdAt: '2026-01-01T00:00:00',
      lastUpdated: '2026-01-01T00:00:00'
    });

    const historyRequest = httpMock.expectOne(request =>
      request.url.endsWith('/api/cards/21/user-action-history')
    );
    expect(historyRequest.request.headers.get('Authorization')).toBe('Bearer test-token');
    historyRequest.flush([{
      id: 1,
      actionType: 'GENERATE',
      oldValue: 'NONE',
      newValue: '**** **** **** 1111',
      changedBy: 'USER',
      createdAt: '2026-01-01T00:00:00'
    }]);
    fixture.detectChanges();

    expect(component.card?.id).toBe('21');
    expect(component.cardHistory.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Debit Card History');
    expect(fixture.nativeElement.textContent).toContain('**** **** **** 1111');
  });

  it('should not generate a local card if the card lookup fails', () => {
    component.accountNumberInput = 'ACC123';
    fixture.detectChanges();
    httpMock.expectOne(request => request.url.endsWith('/api/cards/account/ACC123')).flush(
      { message: 'Unavailable' },
      { status: 500, statusText: 'Server Error' }
    );

    fixture.detectChanges();
    expect(component.card).toBeNull();
    expect(component.cardLoadError).toContain('Unable to load debit card');
    expect(fixture.nativeElement.textContent).not.toContain('Generate Debit Card');
  });

  it('should set the PIN on the displayed debit card, not the first account card', () => {
    component.accountNumberInput = 'ACC123';
    fixture.detectChanges();
    httpMock.expectOne(request => request.url.endsWith('/api/cards/account/ACC123')).flush([
      { id: 8, cardType: 'Credit', status: 'Active', cardNumber: '4111' },
      { id: 9, cardType: 'Debit', status: 'Active', cardNumber: '5222' }
    ]);
    httpMock.expectOne(request => request.url.endsWith('/api/cards/9/user-action-history')).flush([]);

    component.newPin = '1234';
    component.setPin();
    httpMock.expectOne(request => request.url.endsWith('/api/cards/account/ACC123')).flush([
      { id: 8, cardType: 'Credit', status: 'Active', cardNumber: '4111' },
      { id: 9, cardType: 'Debit', status: 'Active', cardNumber: '5222' }
    ]);

    const pinRequest = httpMock.expectOne(request => request.url.endsWith('/api/cards/9/pin?pin=1234'));
    expect(pinRequest.request.method).toBe('PUT');
    pinRequest.flush({});
  });
});
