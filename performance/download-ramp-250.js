
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.DOWNLOAD_TOKEN;
const PASSWORD = __ENV.DOWNLOAD_PASSWORD || null;

const businessErrors = new Rate('business_errors');

if (!TOKEN) {
  throw new Error('DOWNLOAD_TOKEN est obligatoire');
}

export const options = {
  scenarios: {
    download_ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 150 },
        { duration: '30s', target: 200 },
        { duration: '30s', target: 250 },
        { duration: '60s', target: 250 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    business_errors: ['rate<0.01'],
  },
};

export default function () {
  const response = http.post(
    `${BASE_URL}/api/files/download/${encodeURIComponent(TOKEN)}`,
    JSON.stringify({ password: PASSWORD }),
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '10s',
    }
  );

  let linkOK = false;

  if (response.status === 200) {
    try {
      const url = response.json('downloadUrl');
      linkOK = typeof url === 'string' && url.length > 0;
    } catch {
      linkOK = false;
    }
  }

  check(response, {
    'Statut HTTP 200': (r) => r.status === 200,
    'URL de téléchargement présente': () => linkOK,
  });

  businessErrors.add(!linkOK);

  if (response.status !== 200 && __VU <= 2 && __ITER === 0) {
    console.error(`Erreur HTTP : ${response.status}`);
  }

  sleep(1);
}
