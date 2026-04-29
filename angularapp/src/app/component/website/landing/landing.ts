import { Component, ViewEncapsulation, HostListener, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.html',
  styleUrls: ['./landing.css'],
  encapsulation: ViewEncapsulation.None,
  imports: [CommonModule]
})
export class Landing implements OnInit, OnDestroy {
  showPersonalDropdown = false;
  showBusinessDropdown = false;
  showInvestInsureDropdown = false;
  showAnimatedLogo = true;
  logoAnimationComplete = false;
  isScrolled = false;
  isBrowser = false;

  // Scroll-triggered visibility flags
  heroVisible = false;
  zigzag1Visible = false;
  zigzag2Visible = false;
  zigzag3Visible = false;
  zigzag4Visible = false;
  cardsVisible = false;
  paymentGatewayVisible = false;
  dashboardVisible = false;
  securityVisible = false;
  downloadVisible = false;

  private observer?: IntersectionObserver;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    this.startLogoAnimation();
  }

  ngOnInit() {
    if (!this.isBrowser) {
      // SSR: make everything visible
      this.heroVisible = true;
      this.zigzag1Visible = true;
      this.zigzag2Visible = true;
      this.zigzag3Visible = true;
      this.zigzag4Visible = true;
      this.cardsVisible = true;
      this.paymentGatewayVisible = true;
      this.dashboardVisible = true;
      this.securityVisible = true;
      this.downloadVisible = true;
      return;
    }

    // Set hero visible after short delay
    setTimeout(() => this.heroVisible = true, 300);

    // IntersectionObserver for scroll animations
    this.observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const id = entry.target.getAttribute('data-anim');
          if (id) {
            (this as any)[id] = true;
          }
        }
      });
    }, { threshold: 0.15 });

    // Observe sections after DOM renders
    setTimeout(() => this.setupObservers(), 100);
  }

  ngOnDestroy() {
    this.observer?.disconnect();
  }

  private setupObservers() {
    if (!this.isBrowser || !this.observer) return;
    const sections = document.querySelectorAll('[data-anim]');
    sections.forEach(el => this.observer!.observe(el));
  }

  @HostListener('window:scroll')
  onScroll() {
    if (this.isBrowser) {
      this.isScrolled = window.scrollY > 50;
    }
  }

  scrollToTop() {
    if (this.isBrowser) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  togglePersonalDropdown(event: Event) {
    event.preventDefault();
    this.showPersonalDropdown = !this.showPersonalDropdown;
    this.showBusinessDropdown = false;
    this.showInvestInsureDropdown = false;
  }

  toggleBusinessDropdown(event: Event) {
    event.preventDefault();
    this.showBusinessDropdown = !this.showBusinessDropdown;
    this.showPersonalDropdown = false;
    this.showInvestInsureDropdown = false;
  }

  toggleInvestInsureDropdown(event: Event) {
    event.preventDefault();
    this.showInvestInsureDropdown = !this.showInvestInsureDropdown;
    this.showPersonalDropdown = false;
    this.showBusinessDropdown = false;
  }

  goTo(path: string, event?: Event) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.router.navigate([`/${path}`]);
    this.showPersonalDropdown = false;
    this.showBusinessDropdown = false;
    this.showInvestInsureDropdown = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    const dropdown = target.closest('.dropdown');
    if (!dropdown) {
      this.showPersonalDropdown = false;
      this.showBusinessDropdown = false;
      this.showInvestInsureDropdown = false;
    }
  }

  startLogoAnimation() {
    if (!this.isBrowser) {
      this.showAnimatedLogo = false;
      return;
    }
    setTimeout(() => {
      this.logoAnimationComplete = true;
      setTimeout(() => {
        this.showAnimatedLogo = false;
      }, 800);
    }, 1500);
  }
}
