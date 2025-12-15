import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { initFirebase } from './firebase.config';

initFirebase().catch((err) => console.error('Failed to init Firebase', err));

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
