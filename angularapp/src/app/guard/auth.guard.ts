import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';

export const userAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return false;
  }

  const currentUser = sessionStorage.getItem('currentUser');
  if (currentUser) {
    try {
      const user = JSON.parse(currentUser);
      if (user && user.id) {
        return true;
      }
    } catch (e) {
      // Invalid session data
    }
  }

  return router.createUrlTree(['/website/user']);
};

export const adminAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return false;
  }

  const admin = sessionStorage.getItem('admin');
  const role = sessionStorage.getItem('userRole');
  if (admin && role === 'ADMIN') {
    return true;
  }

  return router.createUrlTree(['/admin/login']);
};

export const managerAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return false;
  }

  const admin = sessionStorage.getItem('admin');
  const role = sessionStorage.getItem('userRole');
  if (admin && role === 'MANAGER') {
    return true;
  }

  return router.createUrlTree(['/admin/login']);
};

export const adminOrManagerAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return false;
  }

  const admin = sessionStorage.getItem('admin');
  const role = sessionStorage.getItem('userRole');
  if (admin && (role === 'ADMIN' || role === 'MANAGER')) {
    return true;
  }

  return router.createUrlTree(['/admin/login']);
};

export const salaryAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return false;
  }

  const salaryEmployee = sessionStorage.getItem('salaryEmployee');
  if (salaryEmployee) {
    try {
      const emp = JSON.parse(salaryEmployee);
      if (emp && emp.id) {
        return true;
      }
    } catch (e) {
      // Invalid session data
    }
  }

  return router.createUrlTree(['/website/user']);
};

export const currentAccountAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return false;
  }

  const currentAccount = sessionStorage.getItem('currentAccount');
  if (currentAccount) {
    try {
      const acc = JSON.parse(currentAccount);
      if (acc && acc.accountNumber) {
        return true;
      }
    } catch (e) {
      // Invalid session data
    }
  }

  return router.createUrlTree(['/website/current-account-login']);
};

export const merchantSoundboxAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return false;
  }

  const merchantSoundbox = sessionStorage.getItem('merchantSoundbox');
  if (merchantSoundbox) {
    try {
      const merchant = JSON.parse(merchantSoundbox);
      if (merchant && (merchant.accountNumber || merchant.merchantId)) {
        return true;
      }
    } catch (e) {
      // Invalid session data
    }
  }

  return router.createUrlTree(['/website/merchant-login']);
};

export const agentAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return false;
  }

  const agentData = sessionStorage.getItem('agent');
  if (agentData) {
    try {
      const agent = JSON.parse(agentData);
      if (agent && agent.agentId) {
        return true;
      }
    } catch (e) {
      // Invalid session data
    }
  }

  return router.createUrlTree(['/website/agent-login']);
};

export const pgMerchantAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return false;
  }

  const pgMerchant = sessionStorage.getItem('pgMerchant');
  if (pgMerchant) {
    try {
      const merchant = JSON.parse(pgMerchant);
      if (merchant && merchant.merchantId) {
        return true;
      }
    } catch (e) {
      // Invalid session data
    }
  }

  return router.createUrlTree(['/website/pg-login']);
};
