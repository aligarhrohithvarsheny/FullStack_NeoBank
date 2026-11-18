import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../../../environment/environment';

import { Landing } from './landing';

describe('Landing', () => {
  let component: Landing;
  let fixture: ComponentFixture<Landing>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Landing],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Landing);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the product catalogue and shows eligibility and terms in card details', () => {
    const product = component.creditCardProducts[0];
    component.openCreditCardDetails(product);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.credit-product-card').length).toBe(6);
    expect(fixture.nativeElement.querySelector('.credit-product-modal h3')?.textContent).toContain('Benefits');
    expect(fixture.nativeElement.querySelector('.credit-product-terms')?.textContent).toContain('card agreement');
  });

  it('respects an empty active catalogue returned by the API', () => {
    httpTestingController.expectOne(`${environment.apiBaseUrl}/api/credit-card-products`).flush([]);
    fixture.detectChanges();

    expect(component.creditCardProducts).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('NeoBank credit card products will be available soon.');
  });

  it('shows the login and account shortcuts in the Corporate menu', () => {
    const corporateMenu = fixture.nativeElement.querySelector('#corporate-menu') as HTMLElement;
    const shortcuts = Array.from(corporateMenu.querySelectorAll('.dropdown-item'))
      .map(item => item.textContent?.trim());

    expect(shortcuts).toEqual([
      'Admin Login',
      'Manager Login',
      'HOD Login',
      'FASTag Login',
      'Merchant Login',
      'Open Account'
    ]);
    expect(fixture.nativeElement.querySelector('.nav-actions')).toBeNull();
  });

  it('opens Corporate without leaving another navigation menu open', () => {
    const event = { preventDefault: () => {} } as Event;
    component.showBusinessDropdown = true;

    component.toggleCorporateDropdown(event);

    expect(component.showCorporateDropdown).toBeTrue();
    expect(component.showBusinessDropdown).toBeFalse();
  });

  it('finds account opening when searching with multiple words', () => {
    component.searchQuery = 'open account';

    expect(component.searchResults[0].label).toBe('Open Account');
    expect(component.searchResults[0].route).toBe('website/createaccount');
  });

  it('routes the Open Account search result to account creation', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const result = component.searchItems.find(item => item.label === 'Open Account')!;
    const event = { preventDefault: () => {} } as Event;

    component.selectSearchResult(result, event);

    expect(router.navigate).toHaveBeenCalledWith(['/website/createaccount']);
  });

  it('finds banking features by keyword', () => {
    component.searchQuery = 'fastag recharge';

    expect(component.searchResults[0].route).toBe('website/fasttag-login');
  });

  it('opens account recovery from the landing-page search result', () => {
    component.searchQuery = 'recover blocked account';
    const result = component.searchResults[0];
    const event = { preventDefault: () => {} } as Event;

    expect(result.label).toBe('Recover Account');
    component.selectSearchResult(result, event);
    fixture.detectChanges();

    expect(component.accountRecoveryOpen).toBeTrue();
    expect(fixture.nativeElement.querySelector('#account-recovery-title')?.textContent).toContain('Recover your account');
  });
});
