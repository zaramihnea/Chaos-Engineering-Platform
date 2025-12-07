import http from 'k6/http';
import { sleep } from 'k6';

const TARGET = __ENV.TARGET || 'http://host.docker.internal:5002';

export let options = {
  vus: 10,
  duration: '30s',
};

export default function () {
  const user = 'loaduser';
  const url = `${TARGET}/cart/${user}/add`;
  const payload = JSON.stringify({ product_id: 1 });
  const params = { headers: { 'Content-Type': 'application/json' } };

  http.post(url, payload, params);
  sleep(0.5);
}
