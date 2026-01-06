#!/bin/bash
echo "=== Testing Chaos Engineering Platform ==="
echo ""

BASE_URL="http://localhost:8080"

# 1. Create experiment and run it immediately
echo "1. Creating chaos experiment and running it immediately..."
RESPONSE=$(curl -s -X POST "$BASE_URL/api/experiments?autoRun=true&dryRun=false" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Pod Kill Experiment",
    "faultType": "POD_KILL",
    "parameters": {
      "intensity": "75"
    },
    "target": {
      "cluster": "dev-cluster",
      "namespace": "default",
      "selector": {}
    },
    "timeout": "PT1M",
    "slos": [
      {
        "metric": "LATENCY_P95",
        "query": "http_request_duration_seconds",
        "threshold": 1.0,
        "comparator": "<"
      }
    ],
    "dryRunAllowed": true,
    "createdBy": "test-user"
  }')

EXPERIMENT_ID=$(echo $RESPONSE | jq -r '.experimentId // empty')
RUN_ID=$(echo $RESPONSE | jq -r '.runId // empty')

if [ -z "$EXPERIMENT_ID" ] || [ -z "$RUN_ID" ]; then
  echo "❌ Failed to create and run experiment"
  echo "Response: $RESPONSE"
  exit 1
fi

echo "✅ Experiment created: $EXPERIMENT_ID"
echo "✅ Run started immediately: $RUN_ID"
echo "🔥 Chaos injection is happening NOW!"
echo ""

# 2. List experiments
echo "2. Listing all experiments..."
curl -s "$BASE_URL/api/experiments" | jq '.'
echo ""

# 3. Check run state
echo "3. Checking run state (wait 2s)..."
sleep 2
STATE=$(curl -s "$BASE_URL/api/runs/$RUN_ID/state")
echo "Run state: $STATE"
echo ""

# 4. Wait for completion and get report
echo "4. Waiting for run to complete (wait 10s)..."
sleep 10

echo "Fetching run report..."
curl -s "$BASE_URL/api/runs/$RUN_ID/report" | jq '.' || echo "Report not available yet"
echo ""

echo "=== Test Complete ==="
echo ""
echo "🎉 Next Steps:"
echo "  - Open Frontend: http://localhost"
echo "  - Open Grafana: http://localhost:3000 (admin/admin)"
echo "  - Open Prometheus: http://localhost:9090"
echo "  - Check backend logs: docker compose logs -f backend"
