import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { CreditCard } from './creditcard';
import { environment } from '../../../../environment/environment';

describe('CreditCard application', () => {
  let fixture: ComponentFixture<CreditCard>;
  let component: CreditCard;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    sessionStorage.setItem('currentUser', JSON.stringify({
      id: 8,
      accountNumber: 'ACC123',
      authToken: 'user-token'
    }));

    await TestBed.configureTestingModule({
      imports: [CreditCard],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CreditCard);
    component = fixture.componentInstance;
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/api/users/8`).flush({
      accountNumber: 'ACC123',
      username: 'Applicant',
      email: 'applicant@example.com',
      account: { name: 'Applicant', pan: 'ABCDE1234F', income: 84000 }
    });
    httpMock.expectOne(request =>
      request.url.endsWith('/api/credit-cards/account/ACC123')
    ).flush([]);
    httpMock.expectOne(request =>
      request.url.endsWith('/api/credit-card-requests/my-applications')
    ).flush([]);
  });

  afterEach(() => {
    fixture.destroy();
    httpMock.verify();
    sessionStorage.removeItem('currentUser');
    sessionStorage.removeItem('jwt');
  });

  it('submits only applicant data and displays admin-review status', () => {
    component.openApplyForm();
    expect(component.panInput).toBe('ABCDE1234F');
    expect(component.incomeInput).toBe(84000);

    component.submitCreditApplication();
    const request = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/credit-card-requests/create`
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Authorization')).toBe('Bearer user-token');
    expect(request.request.body).toEqual({ pan: 'ABCDE1234F', annualIncome: 84000 });
    request.flush({ id: 15, status: 'Pending' });

    httpMock.expectOne(request =>
      request.url.endsWith('/api/credit-card-requests/my-applications')
    ).flush([{
      id: 15,
      status: 'Pending',
      pan: 'ABCDE1234F',
      annualIncome: 84000,
      requestDate: '2026-11-13T00:00:00'
    }]);

    expect(component.creditCardApplications[0].status).toBe('Pending');
    expect(component.hasOpenCreditCardApplication).toBeTrue();
    expect(component.showApplyForm).toBeFalse();
  });

  it('requires a valid PAN and positive annual income before submission', () => {
    component.panInput = 'INVALID';
    component.incomeInput = 0;

    component.submitCreditApplication();

    expect(component.applicationError).toContain('valid 10-character PAN');
    httpMock.expectNone(`${environment.apiBaseUrl}/api/credit-card-requests/create`);
  });
});
