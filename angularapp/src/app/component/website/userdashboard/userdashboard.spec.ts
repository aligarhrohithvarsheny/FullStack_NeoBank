import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Userdashboard } from './userdashboard';

describe('Userdashboard', () => {
  let component: Userdashboard;
  let fixture: ComponentFixture<Userdashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Userdashboard],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Userdashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('routes NeoBank payment QR scans into UPI Pay with QR details prefilled', () => {
    spyOn(component, 'selectFeature').and.callFake(feature => {
      component.selectedFeature = feature;
    });
    const verifyUpiSendId = spyOn(component, 'verifyUpiSendId');

    component.onQrCodeScanned('upi://pay?pa=merchant%40neobank&pn=Merchant&am=125.50&cu=INR&tn=Invoice');

    expect(component.selectedFeature).toBe('upi-pay');
    expect(component.upiSendForm).toEqual({
      receiverUpiId: 'merchant@neobank',
      amount: 125.5,
      remark: 'Invoice',
      transactionPin: ''
    });
    expect(verifyUpiSendId).toHaveBeenCalled();
  });

  it('rejects UPI payment QR scans outside NeoBank', () => {
    const verifyUpiSendId = spyOn(component, 'verifyUpiSendId');

    component.onQrCodeScanned('upi://pay?pa=merchant%40otherbank&am=125&cu=INR');

    expect(component.upiSendForm.receiverUpiId).toBe('');
    expect(verifyUpiSendId).not.toHaveBeenCalled();
  });

  it('routes official ATM UPI QR scans to the withdrawal approval flow', () => {
    const http = TestBed.inject(HttpTestingController);
    component.userAccountNumber = '1234567890';

    component.onQrCodeScanned(
      'upi://pay?pa=atmwithdrawal%40neobank&am=500&cu=INR&tn=ATMREQ%3Arequest-token'
    );

    const request = http.expectOne(req =>
      req.url.endsWith('/api/atm/upi-withdrawals/requests/request-token')
        && req.params.get('accountNumber') === '1234567890'
    );
    request.flush({ status: 'PENDING', amount: 500, atmId: 'ATM-01' });

    expect(component.showAtmUpiApproval).toBeTrue();
    expect(component.atmUpiAmount).toBe(500);
    expect(component.atmUpiAtmId).toBe('ATM-01');
  });
});
