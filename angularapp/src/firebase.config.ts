import { FirebaseApp, initializeApp } from 'firebase/app';
import { Analytics, getAnalytics, isSupported } from 'firebase/analytics';

const firebaseConfig = {
  apiKey: 'AIzaSyBLheKxiTOgczWWY5Gz7ziJiiuxDnws7FI',
  authDomain: 'neobank-18ee2.firebaseapp.com',
  projectId: 'neobank-18ee2',
  storageBucket: 'neobank-18ee2.firebasestorage.app',
  messagingSenderId: '97943033894',
  appId: '1:97943033894:web:95972579aed5366fc2aee7',
  measurementId: 'G-9LCWVWRX78',
};

let firebaseApp: FirebaseApp | null = null;
let analyticsInstance: Analytics | null = null;

export const initFirebase = async (): Promise<{
  app: FirebaseApp;
  analytics: Analytics | null;
}> => {
  if (!firebaseApp) {
    firebaseApp = initializeApp(firebaseConfig);
  }

  if (!analyticsInstance && typeof window !== 'undefined') {
    const supported = await isSupported().catch(() => false);
    if (supported) {
      analyticsInstance = getAnalytics(firebaseApp);
    }
  }

  return { app: firebaseApp, analytics: analyticsInstance };
};

export { firebaseConfig };



