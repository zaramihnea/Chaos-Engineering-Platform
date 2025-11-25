import http from 'k6/http';
import { sleep } from 'k6';

// TARGET env var: e.g. http://host.docker.internal:5002 or http://localhost:5002
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

  // Send a POST to add an item (this calls the instrumented add_to_cart method)
  http.post(url, payload, params);
  sleep(0.5);
}
