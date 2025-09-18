import { Component, ViewEncapsulation, HostListener } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.html',
  styleUrls: ['./landing.css'],
  encapsulation: ViewEncapsulation.None   // makes CSS global
})
export class Landing {
  showPersonalDropdown = false;
  showBusinessDropdown = false;
  currentLanguage = 'en';

  // Translation object
  translations = {
    en: {
      // Top Navigation
      skipToMain: 'Skip to Main Content',
      aboutUs: 'About Us',
      neobankOnline: 'NeoBank Online',
      investorCorner: 'Investor Corner',
      forexCardRate: 'Forex Card Rate',
      digitalProducts: 'Digital Products',
      internetBanking: 'Internet Banking',
      
      // Main Navigation
      personal: 'PERSONAL',
      business: 'BUSINESS',
      forex: 'FOREX',
      investInsure: 'INVEST & INSURE',
      nri: 'NRI',
      agri: 'AGRI',
      
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
      
      // Footer
      connectWithUs: 'Connect with us',
      seeLess: 'See Less',
      popularProductsFooter: 'Popular Products',
      usefulLinks: 'Useful Links',
      importantLinks: 'Important Links',
      otherLinks: 'Other Links',
      installBizPay: 'Install NeoBank BIZ PAY',
      installMobileBanking: 'Install NeoBank Mobile Banking',
      allRightsReserved: 'All rights reserved',
      siteBestViewed: 'Site best viewed at 1366*768 resolution in Microsoft Edge 105+, Mozilla 105+ and Google Chrome 106+ browser versions',
      visitorsCount: 'Visitor\'s Count 41623399',
      lastModified: 'Last Modified On Tuesday, 16-09-2024'
    },
    hi: {
      // Top Navigation
      skipToMain: 'मुख्य सामग्री पर जाएं',
      aboutUs: 'हमारे बारे में',
      neobankOnline: 'नियोबैंक ऑनलाइन',
      investorCorner: 'निवेशक कोना',
      forexCardRate: 'फॉरेक्स कार्ड दर',
      digitalProducts: 'डिजिटल उत्पाद',
      internetBanking: 'इंटरनेट बैंकिंग',
      
      // Main Navigation
      personal: 'व्यक्तिगत',
      business: 'व्यापार',
      forex: 'विदेशी मुद्रा',
      investInsure: 'निवेश और बीमा',
      nri: 'एनआरआई',
      agri: 'कृषि',
      
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
      
      // Footer
      connectWithUs: 'हमसे जुड़ें',
      seeLess: 'कम देखें',
      popularProductsFooter: 'लोकप्रिय उत्पाद',
      usefulLinks: 'उपयोगी लिंक',
      importantLinks: 'महत्वपूर्ण लिंक',
      otherLinks: 'अन्य लिंक',
      installBizPay: 'नियोबैंक बिज़ पे इंस्टॉल करें',
      installMobileBanking: 'नियोबैंक मोबाइल बैंकिंग इंस्टॉल करें',
      allRightsReserved: 'सभी अधिकार सुरक्षित',
      siteBestViewed: 'साइट 1366*768 रिज़ॉल्यूशन पर Microsoft Edge 105+, Mozilla 105+ और Google Chrome 106+ ब्राउज़र संस्करणों में सबसे अच्छी दिखती है',
      visitorsCount: 'आगंतुकों की संख्या 41623399',
      lastModified: 'अंतिम बार संशोधित मंगलवार, 16-09-2024'
    }
  };

  constructor(private router: Router) {}

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
}
