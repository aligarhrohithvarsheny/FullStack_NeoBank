import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Transferfunds } from './transferfunds';

describe('Transferfunds', () => {
  let component: Transferfunds;
  let fixture: ComponentFixture<Transferfunds>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Transferfunds],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Transferfunds);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
