import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('opens and loads the bill payments section', () => {
    const loadBillPayments = spyOn(component, 'loadBillPayments');

    component.setActiveSection('bill-payments');
    fixture.detectChanges();

    expect(component.showBillPaymentsSection).toBeTrue();
    expect(loadBillPayments).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.section-content h2 .fa-file-invoice-dollar')).not.toBeNull();
  });

  it('refreshes UPI transactions while UPI Management is open', fakeAsync(() => {
    spyOn(component, 'loadAdminUpiAccounts');
    const loadTransactions = spyOn(component, 'loadAdminUpiTransactions');
    spyOn(component, 'loadAdminUpiFlagged');

    component.setActiveSection('upi-management');
    expect(loadTransactions).toHaveBeenCalledTimes(1);

    tick(5000);

    expect(loadTransactions).toHaveBeenCalledTimes(2);
    component.goToHome();
    tick(5000);
    expect(loadTransactions).toHaveBeenCalledTimes(2);
    component.ngOnDestroy();
  }));
});
