import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Kycupdate } from './kycupdate';

describe('Kycupdate', () => {
  let component: Kycupdate;
  let fixture: ComponentFixture<Kycupdate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Kycupdate],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Kycupdate);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
