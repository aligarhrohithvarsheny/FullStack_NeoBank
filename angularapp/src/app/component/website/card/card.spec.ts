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

  it('should set the PIN on the displayed debit card, not the first account card', () => {
    component.accountNumberInput = 'ACC123';
    fixture.detectChanges();
    httpMock.expectOne(request => request.url.endsWith('/api/cards/account/ACC123')).flush([
      { id: 8, cardType: 'Credit', status: 'Active', cardNumber: '4111' },
      { id: 9, cardType: 'Debit', status: 'Active', cardNumber: '5222' }
    ]);

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
