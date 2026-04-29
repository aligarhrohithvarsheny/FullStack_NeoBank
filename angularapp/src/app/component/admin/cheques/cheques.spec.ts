import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminCheques } from './cheques';

describe('AdminCheques', () => {
  let component: AdminCheques;
  let fixture: ComponentFixture<AdminCheques>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminCheques]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminCheques);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

