import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { User } from './user';

describe('User', () => {
  let component: User;
  let fixture: ComponentFixture<User>;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [User],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(User);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpTesting.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('uses the customer access login endpoint for a supported customer ID', () => {
    component.unifiedAccountNumber = 'CUST12345';
    component.unifiedPassword = 'ExamplePassword123';

    component.unifiedLogin();

    const request = httpTesting.expectOne(
      request => request.url.endsWith('/api/customer-access/login')
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      customerId: 'CUST12345',
      password: 'ExamplePassword123'
    });
    request.flush({ success: false, message: 'Invalid credentials' });
  });
});
