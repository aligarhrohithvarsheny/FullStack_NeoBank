import { Component, ViewEncapsulation, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.html',
  styleUrls: ['./landing.css'],
  encapsulation: ViewEncapsulation.None,   // makes CSS global
  imports: [CommonModule]
})
export class Landing {
  showPersonalDropdown = false;
  showBusinessDropdown = false;
  currentLanguage = 'en';
  showAnimatedLogo = true;
  logoAnimationComplete = false;

  // Translation object
  translations = {
    en: {
      // Navigation
      internetBanking: 'Internet Banking',
      
      // Main Navigation
      personal: 'PERSONAL',
      business: 'BUSINESS',
      forex: 'FOREX',
      investInsure: 'INVEST & INSURE',
      
      // Dropdown Items
      createAccount: 'Create Account',
      userLogin: 'User Login',
      internetBankingLogin: 'Internet Banking Login',
      corporateLogin: 'Corporate Login',
      
      // Main Content
      bankName: 'NeoBank',
      tagline: 'Relationship beyond banking',
      announcementTitle: 'COMPLETION OF 5 YEARS OF DIGITAL BANKING REVOLUTION',
      announcementSubtitle: 'OUR POPULAR PRODUCTS',
      
      // Product Categories
      accounts: 'Accounts',
      cards: 'Cards',
      loans: 'Loans',
      savingsAccount: 'Savings Account',
      currentAccount: 'Current Account',
      debitCard: 'Debit Card',
      creditCard: 'Credit Card',
      personalLoan: 'Personal Loan',
      homeLoan: 'Home Loan',
      
      // Popular Products Section
      popularProducts: 'OUR POPULAR PRODUCTS',
      starHomeLoan: 'STAR HOME LOAN',
      starAssetBackedLoan: 'STAR ASSET BACKED LOAN',
      goldLoan: 'GOLD LOAN',
      governmentSalaryAccount: 'GOVERNMENT SALARY ACCOUNT',
      learnMore: 'Learn More',
      
      // Features Section
      bankingMadeSimple: 'Banking Made Simple',
      bankingDescription: 'Discover the features that make NeoBank your perfect banking partner',
      secureSafe: 'Secure & Safe',
      secureDescription: 'Admin-controlled access with comprehensive KYC verification and fraud detection',
      adminApproved: 'Admin Approved',
      adminDescription: 'All accounts and transactions require admin approval for maximum security',
      completeBanking: 'Complete Banking',
      completeDescription: 'Loan applications, virtual cards, KYC verification, and fund transfers',
      
      // Seamless Banking Section
      seamlessInternetBanking: 'Seamless Internet Banking',
      seamlessDescription: 'Experience banking like never before with our cutting-edge digital platform designed for the modern world',
      instantTransfers: 'Instant Transfers',
      instantTransfersDesc: 'Send money anywhere, anytime with instant processing',
      secureBanking: 'Secure Banking',
      secureBankingDesc: 'Bank-level security with advanced encryption and fraud protection',
      mobileFirst: 'Mobile First',
      mobileFirstDesc: 'Designed specifically for mobile devices with intuitive interface',
      startBanking: 'Start Banking',
      openAccount: 'Open Account',
      scanToDownload: 'Scan to Download',
      webbanking: 'Web Banking',
      mobileApp: 'NeoBank Mobile App',
      bankingHours: 'Banking Hours',
      uptime: 'Uptime',
      activeUsers: 'Active Users',
      
      // Footer
      connectWithUs: 'Connect with us',
      seeLess: 'See Less',
      popularProductsFooter: 'Popular Products',
      usefulLinks: 'Useful Links',
      importantLinks: 'Important Links',
      otherLinks: 'Other Links',
      allRightsReserved: 'All rights reserved',
      siteBestViewed: 'Site best viewed at 1366*768 resolution in Microsoft Edge 105+, Mozilla 105+ and Google Chrome 106+ browser versions',
      visitorsCount: 'Visitor\'s Count 41623399',
      lastModified: 'Last Modified On Tuesday, 16-09-2024'
    },
    hi: {
      // Navigation
      internetBanking: 'इंटरनेट बैंकिंग',
      
      // Main Navigation
      personal: 'व्यक्तिगत',
      business: 'व्यापार',
      forex: 'विदेशी मुद्रा',
      investInsure: 'निवेश और बीमा',
      
      // Dropdown Items
      createAccount: 'खाता बनाएं',
      userLogin: 'उपयोगकर्ता लॉगिन',
      internetBankingLogin: 'इंटरनेट बैंकिंग लॉगिन',
      corporateLogin: 'कॉर्पोरेट लॉगिन',
      
      // Main Content
      bankName: 'नियोबैंक',
      tagline: 'बैंकिंग से परे रिश्ता',
      announcementTitle: 'डिजिटल बैंकिंग क्रांति के 5 वर्ष पूरे',
      announcementSubtitle: 'हमारे लोकप्रिय उत्पाद',
      
      // Product Categories
      accounts: 'खाते',
      cards: 'कार्ड',
      loans: 'ऋण',
      savingsAccount: 'बचत खाता',
      currentAccount: 'चालू खाता',
      debitCard: 'डेबिट कार्ड',
      creditCard: 'क्रेडिट कार्ड',
      personalLoan: 'व्यक्तिगत ऋण',
      homeLoan: 'गृह ऋण',
      
      // Popular Products Section
      popularProducts: 'हमारे लोकप्रिय उत्पाद',
      starHomeLoan: 'स्टार होम लोन',
      starAssetBackedLoan: 'स्टार एसेट बैक्ड लोन',
      goldLoan: 'सोने का ऋण',
      governmentSalaryAccount: 'सरकारी वेतन खाता',
      learnMore: 'और जानें',
      
      // Features Section
      bankingMadeSimple: 'बैंकिंग सरल बनाई गई',
      bankingDescription: 'उन सुविधाओं की खोज करें जो नियोबैंक को आपका सही बैंकिंग साथी बनाती हैं',
      secureSafe: 'सुरक्षित और सुरक्षित',
      secureDescription: 'व्यापक केवाईसी सत्यापन और धोखाधड़ी पहचान के साथ व्यवस्थापक-नियंत्रित पहुंच',
      adminApproved: 'व्यवस्थापक अनुमोदित',
      adminDescription: 'अधिकतम सुरक्षा के लिए सभी खाते और लेनदेन के लिए व्यवस्थापक अनुमोदन आवश्यक है',
      completeBanking: 'पूर्ण बैंकिंग',
      completeDescription: 'ऋण आवेदन, वर्चुअल कार्ड, केवाईसी सत्यापन, और फंड ट्रांसफर',
      
      // Seamless Banking Section
      seamlessInternetBanking: 'सहज इंटरनेट बैंकिंग',
      seamlessDescription: 'आधुनिक दुनिया के लिए डिज़ाइन किए गए हमारे अत्याधुनिक डिजिटल प्लेटफॉर्म के साथ बैंकिंग का अनुभव करें',
      instantTransfers: 'तत्काल ट्रांसफर',
      instantTransfersDesc: 'तत्काल प्रसंस्करण के साथ कहीं भी, कभी भी पैसा भेजें',
      secureBanking: 'सुरक्षित बैंकिंग',
      secureBankingDesc: 'उन्नत एन्क्रिप्शन और धोखाधड़ी सुरक्षा के साथ बैंक-स्तरीय सुरक्षा',
      mobileFirst: 'मोबाइल फर्स्ट',
      mobileFirstDesc: 'सहज इंटरफेस के साथ विशेष रूप से मोबाइल उपकरणों के लिए डिज़ाइन किया गया',
      startBanking: 'बैंकिंग शुरू करें',
      openAccount: 'खाता खोलें',
      scanToDownload: 'डाउनलोड करने के लिए स्कैन करें',
      webbanking: 'वेब बैंकिंग',
      mobileApp: 'नियोबैंक मोबाइल ऐप',
      bankingHours: 'बैंकिंग घंटे',
      uptime: 'अपटाइम',
      activeUsers: 'सक्रिय उपयोगकर्ता',
      
      // Footer
      connectWithUs: 'हमसे जुड़ें',
      seeLess: 'कम देखें',
      popularProductsFooter: 'लोकप्रिय उत्पाद',
      usefulLinks: 'उपयोगी लिंक',
      importantLinks: 'महत्वपूर्ण लिंक',
      otherLinks: 'अन्य लिंक',
      allRightsReserved: 'सभी अधिकार सुरक्षित',
      siteBestViewed: 'साइट 1366*768 रिज़ॉल्यूशन पर Microsoft Edge 105+, Mozilla 105+ और Google Chrome 106+ ब्राउज़र संस्करणों में सबसे अच्छी दिखती है',
      visitorsCount: 'आगंतुकों की संख्या 41623399',
      lastModified: 'अंतिम बार संशोधित मंगलवार, 16-09-2024'
    }
  };

  constructor(private router: Router) {
    this.startLogoAnimation();
  }

  togglePersonalDropdown(event: Event) {
    event.preventDefault();
    this.showPersonalDropdown = !this.showPersonalDropdown;
    // Close business dropdown when opening personal
    this.showBusinessDropdown = false;
  }

  toggleBusinessDropdown(event: Event) {
    event.preventDefault();
    this.showBusinessDropdown = !this.showBusinessDropdown;
    // Close personal dropdown when opening business
    this.showPersonalDropdown = false;
  }

  goTo(path: string, event?: Event) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    
    // Always add '/' so it's absolute
    this.router.navigate([`/${path}`]);
    // Close both dropdowns after navigation
    this.showPersonalDropdown = false;
    this.showBusinessDropdown = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    const dropdown = target.closest('.dropdown');
    if (!dropdown) {
      this.showPersonalDropdown = false;
      this.showBusinessDropdown = false;
    }
  }

  changeLanguage(event: Event) {
    const target = event.target as HTMLSelectElement;
    this.currentLanguage = target.value;
    this.translatePage();
  }

  translatePage() {
    // Language change is handled by Angular's change detection
    // The template will automatically update based on currentLanguage
    console.log('Language changed to:', this.currentLanguage);
  }

  // Helper method to get translation
  t(key: string): string {
    return this.translations[this.currentLanguage as keyof typeof this.translations][key as keyof typeof this.translations['en']] || key;
  }

  startLogoAnimation() {
    // Show animated logo for 1.5 seconds, then animate upward
    setTimeout(() => {
      this.logoAnimationComplete = true;
      // Hide animated logo after animation completes
      setTimeout(() => {
        this.showAnimatedLogo = false;
      }, 800); // Animation duration
    }, 1500); // Initial display duration
  }
}
