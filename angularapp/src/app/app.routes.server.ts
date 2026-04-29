import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Auth-protected routes must be client-rendered so guards can check sessionStorage
  {
    path: 'admin/chat',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/chat',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/dashboard',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/users',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/loans',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/cards',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/transactions',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/kyc',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/cheques',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/gold-loans',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/user-control',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/subsidy-claims',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/education-loan-applications',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/profile',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/complete-profile',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/investments',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/fixed-deposits',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/emi-management',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/credit-cards',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/current-accounts',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/insurance-dashboard',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/fasttags',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/ai-security',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/video-kyc',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/merchant-onboarding',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/cheque-draw-management',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/business-cheque-management',
    renderMode: RenderMode.Client
  },
  {
    path: 'manager/dashboard',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/userdashboard',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/transferfunds',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/loan',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/card',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/transaction',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/kycupdate',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/profile',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/cheque',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/goldloan',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/subsidy-claim',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/insurance',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/fasttag',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/fasttag-apply',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/salary-dashboard',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/current-account-dashboard',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/soundbox-payment',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/agent-dashboard',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/agent-add-merchant',
    renderMode: RenderMode.Client
  },
  {
    path: 'website/fasttag-dashboard',
    renderMode: RenderMode.Client
  },
  // Public routes - server render or prerender for SEO
  {
    path: 'website/createaccount',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/user',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/insurance-login',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/current-account-login',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/merchant-login',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/agent-login',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/fasttag-login',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/logout',
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
    renderMode: RenderMode.Client
  },
  {
    path: '',
    renderMode: RenderMode.Prerender
  },
  // Catch-all for any other routes - client render them
  {
    path: '**',
    renderMode: RenderMode.Client
  }
];
