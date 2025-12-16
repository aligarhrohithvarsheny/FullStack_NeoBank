import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Skip prerendering for routes that require API calls or authentication
  // These will be server-rendered on-demand instead
  {
    path: 'admin/chat',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/chat',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/dashboard',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/users',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/loans',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/cards',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/transactions',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/kyc',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/cheques',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/gold-loans',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/user-control',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/subsidy-claims',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/education-loan-applications',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/profile',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/complete-profile',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/investments',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/fixed-deposits',
    renderMode: RenderMode.Server
  },
  {
    path: 'admin/emi-management',
    renderMode: RenderMode.Server
  },
  {
    path: 'manager/dashboard',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/userdashboard',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/transferfunds',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/createaccount',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/user',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/loan',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/card',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/transaction',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/kycupdate',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/profile',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/cheque',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/goldloan',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/subsidy-claim',
    renderMode: RenderMode.Server
  },
  {
    path: 'website',
    renderMode: RenderMode.Server
  },
  // Prerender only static/public routes
  {
    path: 'website/landing',
    renderMode: RenderMode.Prerender
  },
  {
    path: 'admin/login',
    renderMode: RenderMode.Prerender
  },
  {
    path: '',
    renderMode: RenderMode.Prerender
  },
  // Catch-all for any other routes - server render them
  {
    path: '**',
    renderMode: RenderMode.Server
  }
];
