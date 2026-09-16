import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { ChequeComponent } from './cheque';

describe('ChequeComponent', () => {
  let component: ChequeComponent;
  let fixture: ComponentFixture<ChequeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChequeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChequeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
