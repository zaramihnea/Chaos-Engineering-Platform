import http from 'k6/http';
import { sleep } from 'k6';

const TARGET = __ENV.TARGET || 'http://host.docker.internal:5002';

export let options = {
  scenarios: {
    burst_total: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 40,  
      maxDuration: '180s', 
    },
    followup_total: {
      executor: 'per-vu-iterations',
      startTime: '180s',
      vus: 1,
      iterations: 10, 
      maxDuration: '60s',
    },
  },
};

export default function () {
  // Use TARGET env var to control service + port. Default targets cart on the host.
  http.get(`${TARGET}/checkout`);
  // spread the requests roughly: 180s/40 = 4.5s per request -> use sleep(4)
  sleep(4);
}
