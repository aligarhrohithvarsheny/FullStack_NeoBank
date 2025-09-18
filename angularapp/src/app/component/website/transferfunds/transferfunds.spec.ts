import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Transferfunds } from './transferfunds';

describe('Transferfunds', () => {
  let component: Transferfunds;
  let fixture: ComponentFixture<Transferfunds>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Transferfunds]
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
