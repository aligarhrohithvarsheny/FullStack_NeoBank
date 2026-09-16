import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Createaccount } from './createaccount';

describe('Createaccount', () => {
  let component: Createaccount;
  let fixture: ComponentFixture<Createaccount>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Createaccount],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Createaccount);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
