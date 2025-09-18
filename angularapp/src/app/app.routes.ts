import { Routes } from '@angular/router';

import { Dashboard } from './component/admin/dashboard/dashboard';
import { Users } from './component/admin/users/users';
import { Loans } from './component/admin/loans/loans';
import { Cards } from './component/admin/cards/cards';
import { Transactions } from './component/admin/transactions/transactions';
import { Kyc } from './component/admin/kyc/kyc';
import { Login } from './component/admin/login/login';


import { Userdashboard } from './component/website/userdashboard/userdashboard';
import { Transferfunds } from './component/website/transferfunds/transferfunds';
import { Createaccount } from './component/website/createaccount/createaccount';
import { User } from './component/website/user/user';
import { Landing } from './component/website/landing/landing';
import { Loan } from './component/website/loan/loan';
import { Card } from './component/website/card/card';
import { Transaction } from './component/website/transaction/transaction';
import { Kycupdate } from './component/website/kycupdate/kycupdate';



export const routes: Routes = [
  // ------------------ ADMIN ------------------
  { path: 'admin/login', component: Login },
  { path: 'admin/dashboard', component: Dashboard },
  { path: 'admin/users', component: Users },
  { path: 'admin/loans', component: Loans },
  { path: 'admin/cards', component: Cards },
  { path: 'admin/transactions', component: Transactions },
  { path: 'admin/kyc', component: Kyc },

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
    ],
  },

  // ------------------ DEFAULT REDIRECT ------------------
  { path: '', redirectTo: 'website/landing', pathMatch: 'full' },

  // ------------------ WILDCARD FIX ------------------
  { path: '**', redirectTo: 'website/landing', pathMatch: 'full' },
];
