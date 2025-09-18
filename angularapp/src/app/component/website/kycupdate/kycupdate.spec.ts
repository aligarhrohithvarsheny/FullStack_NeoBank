import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Kycupdate } from './kycupdate';

describe('Kycupdate', () => {
  let component: Kycupdate;
  let fixture: ComponentFixture<Kycupdate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Kycupdate]
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
