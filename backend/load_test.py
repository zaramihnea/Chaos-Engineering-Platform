#!/usr/bin/env python3
"""
Load Testing Script for Chaos Engineering Platform
Tests the REST API under various load conditions
"""

import requests
import time
import statistics
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime

# Configuration
BASE_URL = "http://localhost:8080"
HEALTH_ENDPOINT = f"{BASE_URL}/api/health"
EXPERIMENTS_ENDPOINT = f"{BASE_URL}/api/experiments"

class LoadTestResults:
    def __init__(self):
        self.response_times = []
        self.successful_requests = 0
        self.failed_requests = 0
        self.start_time = None
        self.end_time = None

    def add_result(self, success, response_time):
        if success:
            self.successful_requests += 1
            self.response_times.append(response_time)
        else:
            self.failed_requests += 1

    def calculate_statistics(self):
        if not self.response_times:
            return None

        return {
            'min': min(self.response_times),
            'max': max(self.response_times),
            'mean': statistics.mean(self.response_times),
            'median': statistics.median(self.response_times),
            'std_dev': statistics.stdev(self.response_times) if len(self.response_times) > 1 else 0,
            'total_requests': self.successful_requests + self.failed_requests,
            'successful': self.successful_requests,
            'failed': self.failed_requests,
            'success_rate': (self.successful_requests / (self.successful_requests + self.failed_requests)) * 100
        }

def make_request(url, method='GET', data=None):
    """Make a single HTTP request and measure response time"""
    start = time.time()
    try:
        if method == 'GET':
            response = requests.get(url, timeout=10)
        elif method == 'POST':
            response = requests.post(url, json=data, timeout=10)

        end = time.time()
        response_time = (end - start) * 1000  # Convert to milliseconds

        success = response.status_code < 400
        return success, response_time
    except Exception as e:
        end = time.time()
        response_time = (end - start) * 1000
        return False, response_time

def test_concurrent_load(url, num_requests, num_threads):
    """Test concurrent load with multiple threads"""
    results = LoadTestResults()
    results.start_time = time.time()

    with ThreadPoolExecutor(max_workers=num_threads) as executor:
        futures = [executor.submit(make_request, url) for _ in range(num_requests)]

        for future in as_completed(futures):
            success, response_time = future.result()
            results.add_result(success, response_time)

    results.end_time = time.time()
    return results

def test_sustained_load(url, duration_seconds, requests_per_second):
    """Test sustained load over a period of time"""
    results = LoadTestResults()
    results.start_time = time.time()

    end_time = time.time() + duration_seconds
    request_interval = 1.0 / requests_per_second

    while time.time() < end_time:
        request_start = time.time()
        success, response_time = make_request(url)
        results.add_result(success, response_time)

        # Sleep to maintain target request rate
        elapsed = time.time() - request_start
        sleep_time = max(0, request_interval - elapsed)
        time.sleep(sleep_time)

    results.end_time = time.time()
    return results

def print_results(test_name, results):
    """Print test results in a formatted way"""
    stats = results.calculate_statistics()
    if not stats:
        print(f"\n{test_name}: No successful requests")
        return

    duration = results.end_time - results.start_time
    throughput = stats['total_requests'] / duration

    print(f"\n{'='*70}")
    print(f"{test_name}")
    print(f"{'='*70}")
    print(f"Duration:           {duration:.2f} seconds")
    print(f"Total Requests:     {stats['total_requests']}")
    print(f"Successful:         {stats['successful']}")
    print(f"Failed:             {stats['failed']}")
    print(f"Success Rate:       {stats['success_rate']:.2f}%")
    print(f"Throughput:         {throughput:.2f} req/s")
    print(f"\nResponse Times (ms):")
    print(f"  Min:              {stats['min']:.2f}")
    print(f"  Max:              {stats['max']:.2f}")
    print(f"  Mean:             {stats['mean']:.2f}")
    print(f"  Median:           {stats['median']:.2f}")
    print(f"  Std Dev:          {stats['std_dev']:.2f}")
    print(f"{'='*70}")

def check_server_availability():
    """Check if the server is running"""
    try:
        response = requests.get(HEALTH_ENDPOINT, timeout=5)
        if response.status_code == 200:
            print("✓ Server is running and responding")
            return True
    except:
        pass

    print("✗ Server is not responding")
    print(f"  Please start the application first:")
    print(f"  ./mvnw spring-boot:run")
    return False

def main():
    print("\n" + "="*70)
    print("CHAOS ENGINEERING PLATFORM - LOAD TESTING")
    print("="*70)
    print(f"Test Start Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    # Check if server is available
    if not check_server_availability():
        return

    print("\nStarting load tests...\n")

    # Test 1: Baseline Performance (Single Request)
    print("Test 1: Baseline Performance")
    success, response_time = make_request(HEALTH_ENDPOINT)
    print(f"  Single request response time: {response_time:.2f}ms")
    print(f"  Status: {'SUCCESS' if success else 'FAILED'}")

    # Test 2: Moderate Concurrent Load
    print("\nTest 2: Running Moderate Concurrent Load Test...")
    results = test_concurrent_load(HEALTH_ENDPOINT, num_requests=100, num_threads=10)
    print_results("Moderate Concurrent Load (100 requests, 10 threads)", results)

    # Test 3: High Concurrent Load
    print("\nTest 3: Running High Concurrent Load Test...")
    results = test_concurrent_load(HEALTH_ENDPOINT, num_requests=500, num_threads=50)
    print_results("High Concurrent Load (500 requests, 50 threads)", results)

    # Test 4: Sustained Load
    print("\nTest 4: Running Sustained Load Test...")
    results = test_sustained_load(HEALTH_ENDPOINT, duration_seconds=30, requests_per_second=20)
    print_results("Sustained Load (30 seconds, 20 req/s)", results)

    # Test 5: Stress Test
    print("\nTest 5: Running Stress Test...")
    results = test_concurrent_load(HEALTH_ENDPOINT, num_requests=1000, num_threads=100)
    print_results("Stress Test (1000 requests, 100 threads)", results)

    print(f"\nTest End Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("\nLoad testing completed!")

if __name__ == "__main__":
    main()
