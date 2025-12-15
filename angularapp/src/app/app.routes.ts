import { Routes } from '@angular/router';

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
import { EducationLoanApplicationsComponent } from './component/admin/education-loan-applications/education-loan-applications';
import { AdminProfile } from './component/admin/profile/profile';
import { ManagerDashboard } from './component/admin/manager/dashboard';
import { CompleteProfile } from './component/admin/complete-profile/complete-profile';
import { Investments } from './component/admin/investments/investments';
import { FixedDeposits } from './component/admin/fixed-deposits/fixed-deposits';
import { EmiManagement } from './component/admin/emi-management/emi-management';



export const routes: Routes = [
  // ------------------ ADMIN ------------------
  { path: 'admin/login', component: Login },
  { path: 'admin/dashboard', component: Dashboard },
  // ------------------ MANAGER ------------------
  { path: 'manager/dashboard', component: ManagerDashboard },
  { path: 'admin/users', component: Users },
  { path: 'admin/loans', component: Loans },
  { path: 'admin/cards', component: Cards },
  { path: 'admin/transactions', component: Transactions },
  { path: 'admin/kyc', component: Kyc },
  { path: 'admin/cheques', component: AdminCheques },
  { path: 'admin/gold-loans', component: AdminGoldLoans },
  { path: 'admin/user-control', component: UserControl },
  { path: 'admin/chat', component: AdminChat },
  { path: 'admin/subsidy-claims', component: SubsidyClaims },
  { path: 'admin/education-loan-applications', component: EducationLoanApplicationsComponent },
  { path: 'admin/profile', component: AdminProfile },
  { path: 'admin/complete-profile', component: CompleteProfile },
  { path: 'admin/investments', component: Investments },
  { path: 'admin/fixed-deposits', component: FixedDeposits },
  { path: 'admin/emi-management', component: EmiManagement },

  // ------------------ WEBSITE ------------------
  {
    path: 'website',
    children: [
      { path: 'userdashboard', component: Userdashboard },
      { path: 'transferfunds', component: Transferfunds },
      { path: 'createaccount', component: Createaccount },
      { path: 'user', component: User },
      { path: 'loan', component: Loan },
      { path: 'card', component: Card },
      { path: 'transaction', component: Transaction },
      { path: 'landing', component: Landing },
      { path: 'kycupdate', component: Kycupdate },
      { path: 'profile', component: Profile },
      { path: 'cheque', component: ChequeComponent },
      { path: 'goldloan', component: Goldloan },
      { path: 'chat', component: Chat },
      { path: 'subsidy-claim', component: SubsidyClaim },
    ],
  },

  // ------------------ DEFAULT REDIRECT ------------------
  { path: '', redirectTo: 'website/landing', pathMatch: 'full' },

  // ------------------ WILDCARD FIX ------------------
  { path: '**', redirectTo: 'website/landing', pathMatch: 'full' },
];
