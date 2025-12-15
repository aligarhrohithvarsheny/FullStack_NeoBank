import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Skip prerendering for chat routes that require API calls
  {
    path: 'admin/chat',
    renderMode: RenderMode.Server
  },
  {
    path: 'website/chat',
    renderMode: RenderMode.Server
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
