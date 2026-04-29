import { Routes } from '@angular/router';
import { userAuthGuard, adminAuthGuard, managerAuthGuard, salaryAuthGuard, currentAccountAuthGuard, merchantSoundboxAuthGuard, agentAuthGuard, adminOrManagerAuthGuard, pgMerchantAuthGuard } from './guard/auth.guard';

import { Dashboard } from './component/admin/dashboard/dashboard';
import { Users } from './component/admin/users/users';
import { Loans } from './component/admin/loans/loans';
import { LoansSimple } from './component/admin/loans/loans-simple';
import { Cards } from './component/admin/cards/cards';
import { Transactions } from './component/admin/transactions/transactions';
import { Kyc } from './component/admin/kyc/kyc';
import { Login } from './component/admin/login/login';
import { AdminCheques } from './component/admin/cheques/cheques';


import { Userdashboard } from './component/website/userdashboard/userdashboard';
import { Transferfunds } from './component/website/transferfunds/transferfunds';
import { Createaccount } from './component/website/createaccount/createaccount';
import { User } from './component/website/user/user';
import { Landing } from './component/website/landing/landing';
import { Loan } from './component/website/loan/loan';
import { Card } from './component/website/card/card';
import { Transaction } from './component/website/transaction/transaction';
import { Kycupdate } from './component/website/kycupdate/kycupdate';
import { Profile } from './component/website/profile/profile';
import { ChequeComponent } from './component/website/cheque/cheque';
import { Goldloan } from './component/website/goldloan/goldloan';
import { AdminGoldLoans } from './component/admin/goldloans/goldloans';
import { UserControl } from './component/admin/usercontrol/usercontrol';
import { Chat } from './component/website/chat/chat';
import { AdminChat } from './component/admin/chat/chat';
import { SubsidyClaims } from './component/admin/subsidy-claims/subsidy-claims';
import { SubsidyClaim } from './component/website/subsidy-claim/subsidy-claim';
import { Logout } from './component/website/logout/logout';
import { EducationLoanApplicationsComponent } from './component/admin/education-loan-applications/education-loan-applications';
import { AdminProfile } from './component/admin/profile/profile';
import { ManagerDashboard } from './component/admin/manager/dashboard';
import { CompleteProfile } from './component/admin/complete-profile/complete-profile';
import { Investments } from './component/admin/investments/investments';
import { FixedDeposits } from './component/admin/fixed-deposits/fixed-deposits';
import { EmiManagement } from './component/admin/emi-management/emi-management';
import { CreditCards } from './component/admin/credit-cards/credit-cards';
import { CurrentAccounts } from './component/admin/current-accounts/current-accounts';
import { AdminInsuranceDashboard } from './component/admin/insurance-dashboard/insurance-dashboard';
import { Insurance } from './component/website/insurance/insurance';
import { InsuranceLogin } from './component/website/insurance-login/insurance-login';
import { FasttagApply } from './component/website/fasttag/fasttag-apply';
import { FasttagUser } from './component/website/fasttag/fasttag-user';
import { FasttagAdmin } from './component/admin/fasttag/fasttag-admin';
import { SalaryDashboard } from './component/website/salary-dashboard/salary-dashboard';
import { CurrentAccountLogin } from './component/website/current-account-login/current-account-login';
import { CurrentAccountDashboard } from './component/website/current-account-dashboard/current-account-dashboard';
import { FasttagLogin } from './component/website/fasttag-login/fasttag-login';
import { FasttagDashboard } from './component/website/fasttag-dashboard/fasttag-dashboard';
import { ChequeManagementComponent } from './component/admin/cheque-management/cheque-management.component';
import { BusinessChequeManagementComponent } from './component/admin/business-cheque-management/business-cheque-management.component';
import { AiSecurityDashboardComponent } from './component/admin/ai-security-dashboard/ai-security-dashboard';
import { MerchantLogin } from './component/website/merchant-login/merchant-login';
import { SoundboxPayment } from './component/website/soundbox-payment/soundbox-payment';
import { VideoKycDashboard } from './component/admin/video-kyc-dashboard/video-kyc-dashboard';
import { AgentLogin } from './component/website/agent-login/agent-login';
import { AgentDashboard } from './component/website/agent-dashboard/agent-dashboard';
import { AddMerchant } from './component/website/add-merchant/add-merchant';
import { AdminMerchantOnboarding } from './component/admin/merchant-onboarding/admin-merchant-onboarding';
import { AgentManagement } from './component/admin/agent-management/agent-management';
import { AdminOpenAccount } from './component/admin/admin-open-account/admin-open-account';
import { PaymentGateway } from './component/website/payment-gateway/payment-gateway';
import { PgLogin } from './component/website/pg-login/pg-login';
import { PgDashboard } from './component/website/pg-dashboard/pg-dashboard';
import { PasswordSetupComponent } from './component/website/password-setup/password-setup.component';



export const routes: Routes = [
  // ------------------ ADMIN ------------------
  { path: 'admin/login', component: Login },
  { path: 'admin/dashboard', component: Dashboard, canActivate: [adminAuthGuard] },
  // ------------------ MANAGER ------------------
  { path: 'manager/dashboard', component: ManagerDashboard, canActivate: [managerAuthGuard] },
  { path: 'admin/users', component: Users, canActivate: [adminAuthGuard] },
  { path: 'admin/loans', component: Loans, canActivate: [adminAuthGuard] },
  { path: 'admin/cards', component: Cards, canActivate: [adminAuthGuard] },
  { path: 'admin/transactions', component: Transactions, canActivate: [adminAuthGuard] },
  { path: 'admin/kyc', component: Kyc, canActivate: [adminAuthGuard] },
  { path: 'admin/cheques', component: AdminCheques, canActivate: [adminAuthGuard] },
  { path: 'admin/cheque-draw-management', component: ChequeManagementComponent, canActivate: [adminAuthGuard] },
  { path: 'admin/business-cheque-management', component: BusinessChequeManagementComponent, canActivate: [adminAuthGuard] },
  { path: 'admin/gold-loans', component: AdminGoldLoans, canActivate: [adminAuthGuard] },
  { path: 'admin/user-control', component: UserControl, canActivate: [adminAuthGuard] },
  { path: 'admin/chat', component: AdminChat, canActivate: [adminAuthGuard] },
  { path: 'admin/subsidy-claims', component: SubsidyClaims, canActivate: [adminAuthGuard] },
  { path: 'admin/education-loan-applications', component: EducationLoanApplicationsComponent, canActivate: [adminAuthGuard] },
  { path: 'admin/profile', component: AdminProfile, canActivate: [adminAuthGuard] },
  { path: 'admin/complete-profile', component: CompleteProfile, canActivate: [adminAuthGuard] },
  { path: 'admin/investments', component: Investments, canActivate: [adminAuthGuard] },
  { path: 'admin/fixed-deposits', component: FixedDeposits, canActivate: [adminAuthGuard] },
  { path: 'admin/emi-management', component: EmiManagement, canActivate: [adminAuthGuard] },
  { path: 'admin/credit-cards', component: CreditCards, canActivate: [adminAuthGuard] },
  { path: 'admin/current-accounts', component: CurrentAccounts, canActivate: [adminAuthGuard] },
  { path: 'admin/insurance-dashboard', component: AdminInsuranceDashboard, canActivate: [adminAuthGuard] },
  { path: 'admin/ai-security', component: AiSecurityDashboardComponent, canActivate: [adminAuthGuard] },
  { path: 'admin/video-kyc', component: VideoKycDashboard, canActivate: [adminAuthGuard] },
  { path: 'admin/merchant-onboarding', component: AdminMerchantOnboarding, canActivate: [adminAuthGuard] },
  { path: 'admin/agent-management', component: AgentManagement, canActivate: [adminAuthGuard] },
  { path: 'admin/admin-open-account', component: AdminOpenAccount, canActivate: [adminOrManagerAuthGuard] },

  // ------------------ WEBSITE ------------------
  {
    path: 'website',
    children: [
      { path: 'userdashboard', component: Userdashboard, canActivate: [userAuthGuard] },
      { path: 'transferfunds', component: Transferfunds, canActivate: [userAuthGuard] },
      { path: 'createaccount', component: Createaccount },
      { path: 'user', component: User },
      { path: 'insurance', component: Insurance, canActivate: [userAuthGuard] },
      { path: 'insurance-login', component: InsuranceLogin },
      { path: 'loan', component: Loan, canActivate: [userAuthGuard] },
      { path: 'card', component: Card, canActivate: [userAuthGuard] },
      { path: 'transaction', component: Transaction, canActivate: [userAuthGuard] },
      { path: 'landing', component: Landing },
      { path: 'kycupdate', component: Kycupdate, canActivate: [userAuthGuard] },
      { path: 'profile', component: Profile, canActivate: [userAuthGuard] },
      { path: 'cheque', component: ChequeComponent, canActivate: [userAuthGuard] },
      { path: 'fasttag', component: FasttagUser, canActivate: [userAuthGuard] },
      { path: 'goldloan', component: Goldloan, canActivate: [userAuthGuard] },
      { path: 'chat', component: Chat, canActivate: [userAuthGuard] },
      { path: 'subsidy-claim', component: SubsidyClaim, canActivate: [userAuthGuard] },
      { path: 'logout', component: Logout },
      { path: 'salary-dashboard', component: SalaryDashboard, canActivate: [salaryAuthGuard] },
      { path: 'current-account-login', component: CurrentAccountLogin },
      { path: 'current-account-dashboard', component: CurrentAccountDashboard, canActivate: [currentAccountAuthGuard] },
      { path: 'fasttag-login', component: FasttagLogin },
      { path: 'fasttag-dashboard', component: FasttagDashboard },
      { path: 'merchant-login', component: MerchantLogin },
      { path: 'soundbox-payment', component: SoundboxPayment, canActivate: [merchantSoundboxAuthGuard] },
      { path: 'agent-login', component: AgentLogin },
      { path: 'agent-dashboard', component: AgentDashboard, canActivate: [agentAuthGuard] },
      { path: 'agent-add-merchant', component: AddMerchant, canActivate: [agentAuthGuard] },
      { path: 'agent-edit-merchant/:merchantId', component: AddMerchant, canActivate: [agentAuthGuard] },
      { path: 'payment-gateway', component: PaymentGateway },
      { path: 'pg-login', component: PgLogin },
      { path: 'pg-dashboard', component: PgDashboard, canActivate: [pgMerchantAuthGuard] },
    ],
  },

  // ------------------ PASSWORD SETUP ------------------
  { path: 'password-setup', component: PasswordSetupComponent },

  // ------------------ DEFAULT REDIRECT ------------------
  { path: '', redirectTo: 'website/landing', pathMatch: 'full' },

  // FastTag routes
  { path: 'website/fasttag-apply', component: FasttagApply, canActivate: [userAuthGuard] },
  { path: 'admin/fasttags', component: FasttagAdmin, canActivate: [adminAuthGuard] },

  // ------------------ WILDCARD FIX ------------------
  { path: '**', redirectTo: 'website/landing', pathMatch: 'full' },
];
