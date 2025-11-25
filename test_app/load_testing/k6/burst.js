import http from 'k6/http';
import { sleep } from 'k6';

const TARGET = __ENV.TARGET || 'http://host.docker.internal:5002';

export let options = {
  stages: [
    { duration: '30s', target: 40 }, 
    { duration: '150s', target: 40 },
    { duration: '10s', target: 10 }, 
    { duration: '50s', target: 10 }, 
  ],
  thresholds: {
    'http_req_duration': ['p(95)<1000']
  }
};

export default function () {
  // exercise the cart checkout endpoint (POST to /cart/<user>/checkout)
  const user = 'loaduser';
  const url = `${TARGET}/cart/${user}/checkout`;
  http.post(url, null);
  sleep(1);
}
