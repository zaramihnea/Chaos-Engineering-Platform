#!/usr/bin/env python3
"""
Fault Agent Wrapper
Maps Control Plane parameters to chaos_agent.py
"""
import argparse
import subprocess
import sys
import os


def main():
    parser = argparse.ArgumentParser(description='Fault Agent - Chaos Engineering')
    parser.add_argument('--fault-type', required=True,
                       choices=['cpu', 'memory', 'network', 'disk', 'latency', 'exception'])
    parser.add_argument('--target', required=True, help='Target service/cluster')
    parser.add_argument('--duration', required=True, type=int, help='Duration in seconds')
    parser.add_argument('--mode', required=True, choices=['production', 'dry-run'])
    parser.add_argument('--intensity', type=int, default=50, help='Intensity 0-100')

    args = parser.parse_args()

    print("=" * 70)
    print("FAULT AGENT - Chaos Injection Wrapper")
    print("=" * 70)
    print(f"Fault Type:   {args.fault_type}")
    print(f"Target:       {args.target}")
    print(f"Duration:     {args.duration}s")
    print(f"Mode:         {args.mode}")
    print(f"Intensity:    {args.intensity}%")
    print("=" * 70)

    if args.mode == 'dry-run':
        print("\n[DRY-RUN] Simulating fault injection...")
        print(f"[DRY-RUN] Would inject {args.fault_type} fault for {args.duration}s")
        return 0

    # Map fault types to chaos_agent.py faults
    fault_map = {
        'cpu': 'cpu_hog',
        'memory': 'memory_hog',
        'exception': 'kill_restart',  # Kill and restart = pod kill
        'network': 'pause',  # Network issues simulated by pause
        'disk': 'disk_fill',
        'latency': 'cpu_quota'  # CPU quota to simulate latency
    }

    chaos_fault = fault_map.get(args.fault_type, 'kill_restart')

    # Find containers to target
    targets = discover_targets(args.target)

    if not targets:
        print(f"[ERROR] No containers found for target: {args.target}")
        print("[INFO] Defaulting to gateway container...")
        targets = "cep-gateway"

    print(f"\n[CHAOS AGENT] Executing fault injection...")
    print(f"[CHAOS AGENT] Fault: {chaos_fault}")
    print(f"[CHAOS AGENT] Targets: {targets}")

    # Build chaos_agent.py command
    chaos_cmd = [
        'python3',
        '/app/backend/chaos_agent.py',
        '--mode', 'focused',
        '--fault', chaos_fault,
        '--targets', targets,
        '--duration', str(args.duration),
        '--interval', str(max(5, args.duration // 2)),  # Half duration or 5s min
    ]

    try:
        # Execute chaos_agent.py
        result = subprocess.run(
            chaos_cmd,
            capture_output=True,
            text=True,
            timeout=args.duration + 30
        )

        print("\n" + "=" * 70)
        print("CHAOS AGENT OUTPUT:")
        print("=" * 70)
        print(result.stdout)

        if result.stderr:
            print("\nSTDERR:")
            print(result.stderr)

        if result.returncode == 0:
            print("\n✅ Fault injection completed successfully")
        else:
            print(f"\n❌ Fault injection failed with code {result.returncode}")

        return result.returncode

    except subprocess.TimeoutExpired:
        print(f"\n⚠️  Chaos agent timed out after {args.duration + 30}s")
        return 1
    except Exception as e:
        print(f"\n❌ Error executing chaos agent: {e}")
        return 1


def discover_targets(target_hint):
    """
    Discover containers based on target hint
    """
    import subprocess

    try:
        result = subprocess.run(
            ['docker', 'ps', '--format', '{{.Names}}'],
            capture_output=True,
            text=True,
            check=True
        )

        containers = result.stdout.strip().split('\n')

        # Filter containers based on target hint
        matching = []
        for container in containers:
            if (target_hint.lower() in container.lower() or
                'gateway' in container.lower() or
                'catalog' in container.lower() or
                'cart' in container.lower() or
                'payment' in container.lower()):

                # Exclude monitoring containers
                if not any(x in container.lower() for x in ['prometheus', 'grafana', 'postgres', 'backend', 'frontend']):
                    matching.append(container)

        if matching:
            return ','.join(matching[:2])  # Limit to 2 containers for safety

        return None

    except Exception as e:
        print(f"[WARN] Failed to discover containers: {e}")
        return None


if __name__ == "__main__":
    sys.exit(main())
