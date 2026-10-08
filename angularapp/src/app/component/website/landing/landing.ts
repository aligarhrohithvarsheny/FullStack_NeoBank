import { Component, ViewEncapsulation, HostListener, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';

type FooterInformation = {
  title: string;
  description: string;
  details: string[];
};

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
  activeFooterInformation: FooterInformation | null = null;

  readonly footerInformation: Record<string, Omit<FooterInformation, 'title'>> = {
    'Savings Account': {
      description: 'A digital account experience for everyday banking and managing your money.',
      details: [
        'Explore account balances, transaction history, and digital payment options from one place.',
        'Account opening, features, fees, and eligibility are subject to the applicable product terms.'
      ]
    },
    'Current Account': {
      description: 'Banking tools designed to support businesses and their day-to-day finances.',
      details: [
        'Manage business banking activity and access services such as payments and cheque facilities.',
        'Available features and account requirements depend on the business profile and account terms.'
      ]
    },
    'Personal Loan': {
      description: 'Learn about borrowing options for planned expenses and personal goals.',
      details: [
        'Review repayment obligations, interest, fees, and the total cost of borrowing before applying.',
        'Loan approval, amount, and terms are subject to eligibility and credit assessment.'
      ]
    },
    'Credit Card': {
      description: 'A convenient way to make eligible purchases and manage card activity digitally.',
      details: [
        'Use the card experience to review transactions and understand payment due dates.',
        'Credit limits, charges, benefits, and approval are governed by the applicable card terms.'
      ]
    },
    FASTag: {
      description: 'Manage FASTag services for convenient electronic toll payments.',
      details: [
        'Eligible users can explore FASTag application, account linking, and recharge services.',
        'Toll transactions and FASTag services are subject to issuer, vehicle, and network rules.'
      ]
    },
    'About Us': {
      description: 'NeoBank brings everyday banking services together in a convenient digital experience.',
      details: [
        'Our platform is designed to help customers explore accounts, payments, cards, and other financial services online.',
        'We aim to make banking information easier to access while keeping security and responsible use in focus.'
      ]
    },
    Careers: {
      description: 'Build useful digital banking experiences with the NeoBank team.',
      details: [
        'We value people who care about customer experience, technology, and responsible financial services.',
        'Check NeoBank announcements for current opportunities and application details.'
      ]
    },
    Press: {
      description: 'Find company announcements and updates about NeoBank.',
      details: [
        'Press materials and official announcements will be shared through NeoBank communication channels.',
        'For media-related enquiries, contact the support team using the contact details shown on this page.'
      ]
    },
    Blog: {
      description: 'Explore updates and practical information about digital banking.',
      details: [
        'Topics may include digital payments, account safety, and ways to use online banking services.',
        'General information is educational and should not be treated as personal financial advice.'
      ]
    },
    Partners: {
      description: 'NeoBank works with service providers to support digital banking experiences.',
      details: [
        'Partner services may support payments, technology, and other parts of the customer experience.',
        'Availability and terms for a partner service depend on the relevant provider and product.'
      ]
    },
    FAQs: {
      description: 'Quick guidance for common questions about using NeoBank services.',
      details: [
        'You can review the relevant service information on this page for details about accounts, cards, loans, and FASTag.',
        'For account-specific help, contact support and never share your password, PIN, or one-time passcode.'
      ]
    },
    'Contact Support': {
      description: 'Get help with NeoBank services and general enquiries.',
      details: [
        'Call +91 7093976680 or email support@neobank.co.in using the contact details displayed on this page.',
        'Do not include passwords, PINs, or one-time passcodes in support messages.'
      ]
    },
    'Privacy Policy': {
      description: 'NeoBank takes the privacy of customer information seriously.',
      details: [
        'Personal and account information should be handled only for legitimate service, security, and legal purposes.',
        'Avoid sharing confidential credentials. Refer to the applicable privacy notice and service terms for complete details.'
      ]
    },
    'Terms of Service': {
      description: 'The terms explain the conditions for using NeoBank products and digital services.',
      details: [
        'Product eligibility, charges, responsibilities, and service availability may differ by product.',
        'Please review the applicable agreements before applying for or using a service.'
      ]
    },
    'Grievance Redressal': {
      description: 'If you have a concern about a NeoBank service, contact the support team so it can be reviewed.',
      details: [
        'Share a clear description of the issue and relevant reference details, but never send passwords, PINs, or one-time passcodes.',
        'Use the support email or phone number shown on this page to raise your concern and request follow-up.'
      ]
    }
  };

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

  openFooterInformation(title: string, event: Event) {
    event.preventDefault();
    const information = this.footerInformation[title];
    if (information) {
      this.activeFooterInformation = { title, ...information };
    }
  }

  closeFooterInformation() {
    this.activeFooterInformation = null;
  }

  @HostListener('document:keydown.escape')
  onEscapeKey() {
    this.closeFooterInformation();
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

  goToAdminRole(role: 'ADMIN' | 'MANAGER', event?: Event) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.router.navigate(['/admin/login'], { queryParams: { role } });
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
