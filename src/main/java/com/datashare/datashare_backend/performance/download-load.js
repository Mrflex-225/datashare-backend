
import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.DOWNLOAD_TOKEN;
const password = __ENV.DOWNLOAD_PASSWORD || null;

if (!token) {
  throw new Error('La variable DOWNLOAD_TOKEN est obligatoire.');
}

export const options = {
  vus: 5,
  duration: '30s',

  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const response = http.post(
    `${baseUrl}/api/files/download/${encodeURIComponent(token)}`,
    JSON.stringify({ password }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'download-link' },
    }
  );

  if (__ITER === 0) {
    console.log(`HTTP status: ${response.status}`);
    console.log(`URL appelée: ${response.request.url.split('?')[0].replace(/\/download\/[^/]+$/, '/download/[TOKEN]')}`);
  }

  check(response, {
    'HTTP 200': (r) => r.status === 200,
    'Lien de téléchargement reçu': (r) => {
      if (r.status !== 200) return false;
      try {
        return typeof r.json('downloadUrl') === 'string';
      } catch {
        return false;
      }
    },
  });

  sleep(1);
}
