import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
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
});
