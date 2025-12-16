/**
 * Backend Health Check Script
 * 
 * This script helps you verify if your backend is deployed and accessible.
 * Run this with: node check-backend.js <backend-url>
 * 
 * Example: node check-backend.js https://your-app.railway.app
 */

const https = require('https');
const http = require('http');

function checkBackend(url) {
  if (!url) {
    console.error('❌ Error: Please provide a backend URL');
    console.log('\nUsage: node check-backend.js <backend-url>');
    console.log('Example: node check-backend.js https://your-app.railway.app');
    process.exit(1);
  }

  // Remove trailing slash
  const baseUrl = url.replace(/\/$/, '');
  
  // Test health endpoint
  const healthUrl = `${baseUrl}/actuator/health`;
  const apiTestUrl = `${baseUrl}/api/users/test`; // Just to test if API is accessible
  
  console.log('🔍 Checking backend health...\n');
  console.log(`Backend URL: ${baseUrl}`);
  console.log(`Health Check: ${healthUrl}\n`);

  const protocol = baseUrl.startsWith('https') ? https : http;

  // Check health endpoint
  protocol.get(healthUrl, (res) => {
    let data = '';

    res.on('data', (chunk) => {
      data += chunk;
    });

    res.on('end', () => {
      if (res.statusCode === 200) {
        console.log('✅ Backend is UP and accessible!');
        try {
          const health = JSON.parse(data);
          console.log(`   Status: ${health.status || 'UP'}`);
          if (health.components) {
            console.log(`   Components: ${Object.keys(health.components).join(', ')}`);
          }
        } catch (e) {
          console.log(`   Response: ${data.substring(0, 100)}...`);
        }
        console.log(`\n✅ Use this URL in vercel.json:`);
        console.log(`   "destination": "${baseUrl}/api/$1"`);
        console.log(`\n📝 Update your vercel.json file with the above destination.`);
      } else {
        console.log(`⚠️  Backend responded with status ${res.statusCode}`);
        console.log(`   Response: ${data.substring(0, 200)}`);
        console.log(`\n💡 The backend might be running but health endpoint might not be available.`);
        console.log(`   Try using the URL anyway: "${baseUrl}/api/$1"`);
      }
    });
  }).on('error', (err) => {
    console.error('❌ Error connecting to backend:');
    console.error(`   ${err.message}`);
    console.error('\n💡 Possible issues:');
    console.error('   1. Backend is not deployed yet');
    console.error('   2. Backend URL is incorrect');
    console.error('   3. Backend is down or not accessible');
    console.error('   4. Network/firewall issues');
    console.error('\n📝 If backend is not deployed, deploy it first:');
    console.error('   - Railway: https://railway.app');
    console.error('   - Render: https://render.com');
    process.exit(1);
  });
}

// Get URL from command line arguments
const backendUrl = process.argv[2];
checkBackend(backendUrl);
