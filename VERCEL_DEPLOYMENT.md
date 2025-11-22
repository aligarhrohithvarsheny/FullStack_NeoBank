# Vercel Deployment Configuration Guide

## Issue: Connection Refused to localhost:8080

If you're seeing `ERR_CONNECTION_REFUSED` errors for `localhost:8080` in your deployed Vercel app, follow these steps:

## Solution 1: Configure Backend URL in vercel.json (Recommended)

1. Open `vercel.json` in the root directory
2. Find the line with `"destination": "https://YOUR_BACKEND_URL_HERE/api/$1"`
3. Replace `YOUR_BACKEND_URL_HERE` with your actual backend URL
   - Example: `"https://your-backend.railway.app/api/$1"`
   - Example: `"https://your-backend.herokuapp.com/api/$1"`
   - Example: `"https://api.yourdomain.com/api/$1"`

## Solution 2: Use Environment Variables (Alternative)

1. Go to your Vercel Dashboard
2. Navigate to: **Your Project > Settings > Environment Variables**
3. Add a new environment variable:
   - **Name**: `BACKEND_API_URL`
   - **Value**: `https://your-backend-url.com/api`
   - **Environment**: Production (and Preview if needed)
4. Redeploy your application

The `replace-env.js` script will automatically use this environment variable during build.

## Verify Your Configuration

After updating, make sure:
- ✅ Your backend is deployed and accessible
- ✅ CORS is configured on your backend to allow requests from your Vercel domain
- ✅ The backend URL in `vercel.json` matches your actual backend URL
- ✅ You've redeployed your Vercel application after making changes

## Testing

After deployment, test by:
1. Opening your Vercel app in the browser
2. Opening browser DevTools (F12)
3. Checking the Network tab for API requests
4. Verify requests go to `/api/...` (which will be proxied to your backend)

## Common Backend URLs

- **Railway**: `https://your-app.railway.app`
- **Heroku**: `https://your-app.herokuapp.com`
- **Render**: `https://your-app.onrender.com`
- **AWS/Other**: Your custom domain or API gateway URL

## Additional Notes

### Amplitude Warning

If you see an Amplitude warning in the browser console:
```
Amplitude Logger [Warn]: `options.defaultTracking` is set to undefined...
```

This warning is coming from a **browser extension** (contentScript.bundle.js), not from your application code. You can safely ignore this warning, or disable the browser extension that's causing it.

### Sentry 403 Error

If you see a Sentry POST request getting 403 Forbidden, this is also likely from a browser extension or third-party service. It doesn't affect your application functionality.

