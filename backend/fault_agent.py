#!/usr/bin/env python3
import argparse
import sys
from datetime import datetime


def main():
    parser = argparse.ArgumentParser(
        description='Fault Agent - Inject chaos engineering faults',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python fault_agent.py --fault-type cpu --target user-service --duration 60 --mode dry-run
  python fault_agent.py --fault-type network --target api-gateway --duration 120 --mode production
        """
    )

    parser.add_argument(
        '--fault-type',
        required=True,
        choices=['cpu', 'memory', 'network', 'disk', 'latency', 'exception'],
        help='Type of fault to inject'
    )

    parser.add_argument(
        '--target',
        required=True,
        help='Target service or component'
    )

    parser.add_argument(
        '--duration',
        required=True,
        type=int,
        help='Duration of fault in seconds'
    )

    parser.add_argument(
        '--mode',
        required=True,
        choices=['production', 'dry-run'],
        help='Execution mode: production or dry-run'
    )

    parser.add_argument(
        '--intensity',
        type=int,
        default=50,
        help='Fault intensity/severity (0-100, default: 50)'
    )

    args = parser.parse_args()

    # Print the received arguments
    print("=" * 60)
    print("FAULT AGENT - CHAOS INJECTION")
    print("=" * 60)
    print(f"Timestamp:    {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Fault Type:   {args.fault_type.upper()}")
    print(f"Target:       {args.target}")
    print(f"Duration:     {args.duration}s")
    print(f"Mode:         {args.mode.upper()}")
    print(f"Intensity:    {args.intensity}%")
    print("=" * 60)

    if args.mode == 'dry-run':
        print("\n[DRY-RUN MODE] No actual fault injection will occur.")
        print(f"[DRY-RUN] Would inject {args.fault_type} fault to {args.target}")
        print(f"[DRY-RUN] Would last for {args.duration} seconds at {args.intensity}% intensity")
    else:
        print("\n[PRODUCTION MODE] Injecting fault...")
        print(f"[PROD] Injecting {args.fault_type} fault to {args.target}")
        print(f"[PROD] Duration: {args.duration}s | Intensity: {args.intensity}%")
        print("[PROD] ⚠️  Fault injection would occur here in real implementation")

    print("\n✓ Fault agent execution completed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
