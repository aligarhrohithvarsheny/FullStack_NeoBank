import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';

import { Landing } from './landing';

describe('Landing', () => {
  let component: Landing;
  let fixture: ComponentFixture<Landing>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Landing],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Landing);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
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
});
