# k6 load testing examples (Docker)

This folder contains two example k6 scripts and Docker run instructions to exercise the `test_app` microservices.

Files
- `burst.js` - ramp to 40 VUs, hold ~3 minutes, then drop to 10 VUs for ~1 minute.
- `spread.js` - send ~40 requests over 180s, then ~10 requests over the following 60s (approximate).

Why Docker?
- Using the official `loadimpact/k6` image avoids installing k6 locally. The container runs the k6 binary and can mount the scripts from this repo.
- When running the Docker container on Windows and targeting services running on the host, use `host.docker.internal` as the hostname (works on Docker Desktop).

docker run --rm -v ${PWD}:/scripts -w /scripts loadimpact/k6 run /scripts/load_testing/k6/burst.js
docker run --rm -v ${PWD}:/scripts -w /scripts loadimpact/k6 run /scripts/load_testing/k6/spread.js
Run the examples (Windows PowerShell)

From the repo root (`d:/programming/Master/Chaos-Engineering-Platform/test_app`):

```powershell
# Docker: run burst script using the official k6 image and target the cart service on the host
docker run --rm -v ${PWD}:/scripts -w /scripts -e TARGET=http://host.docker.internal:5002 loadimpact/k6 run /scripts/load_testing/k6/burst.js

# Docker: run spread script and target the cart service
docker run --rm -v ${PWD}:/scripts -w /scripts -e TARGET=http://host.docker.internal:5002 loadimpact/k6 run /scripts/load_testing/k6/spread.js
```

Run locally (k6 installed on host)

```powershell
# Target a host service on localhost (example: cart on port 5002)
$env:TARGET = 'http://localhost:5002'
k6 run load_testing/k6/burst.js

# Or one-liners without setting envvar globally
k6 run -e TARGET=http://localhost:5002 load_testing/k6/burst.js
k6 run -e TARGET=http://localhost:5002 load_testing/k6/spread.js
```

Notes
- The test scripts default to targeting the cart service on port 5002. Your stack exposes services on `localhost:5000` (gateway), `5001` (catalog), `5002` (cart), and `5003` (payment).
- When running k6 in Docker and targeting services running on the host machine, use `host.docker.internal` as the hostname; when running k6 on the host, use `localhost`.
- If you want to target a different service/port, set `TARGET` accordingly, e.g. `-e TARGET=http://host.docker.internal:5000` for the gateway.
- If you need to run many VUs or want to run in CI, consider using a dedicated machine or an orchestration approach.

Prometheus / Grafana integration
- Easiest: write k6 metrics to InfluxDB and visualize in Grafana (k6 supports `--out influxdb=...` natively).
  - Example: `k6 run --out influxdb=http://influx:8086/mydb script.js`
- Advanced: use `xk6` to build a custom k6 with Prometheus remote-write extension (`xk6-output-prometheus-remote-write`), then push metrics to a Prometheus remote-write receiver.

If you want, I can:
- Add these scripts to the repo (done).
- Add a small `docker-compose` service to run k6 from Docker and target the stack.
- Add detailed steps to build an `xk6` binary for Prometheus remote-write.

Which of the above should I do next? (add docker-compose entry / add xk6 remote-write guide / nothing else)