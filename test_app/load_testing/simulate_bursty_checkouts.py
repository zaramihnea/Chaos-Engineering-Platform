import argparse
import asyncio
import itertools
import math
import random
import time
from typing import List

import aiohttp


async def do_checkout(session: aiohttp.ClientSession, host: str, port: int, user: str, product_id: int):
    base = f"http://{host}:{port}"
    try:
        await session.post(f"{base}/cart/{user}/add", json={"product_id": product_id}, timeout=5)
        async with session.post(f"{base}/cart/{user}/checkout", timeout=10) as resp:
            text = await resp.text()
            return resp.status, text
    except Exception as e:
        return None, str(e)


def rps_for_time(t: float, burst_duration: float, base_rps: float, peak_rps: float, decay_power: float) -> float:
    frac = min(max(t / burst_duration, 0.0), 1.0)
    return base_rps + (peak_rps - base_rps) * (1 - frac ** decay_power)


async def burst_loop(host: str, port: int, users: List[str], product_id: int,
                     burst_interval: float, burst_duration: float,
                     peak_rps: float, base_rps: float, decay_power: float):
    user_cycle = itertools.cycle(users)
    async with aiohttp.ClientSession() as session:
        next_burst = time.time()
        while True:
            now = time.time()
            sleep_for = max(0, next_burst - now)
            if sleep_for > 0:
                await asyncio.sleep(sleep_for)

            start = time.time()
            end = start + burst_duration
            t = 0.0
            last_sent = time.time()
            sent = 0
            while time.time() < end:
                t = time.time() - start
                target_rps = rps_for_time(t, burst_duration, base_rps, peak_rps, decay_power)
                if target_rps <= 0:
                    await asyncio.sleep(1)
                    continue

                interval = 1.0 / target_rps
                interval = max(0.001, random.gauss(interval, interval * 0.1))
                user = next(user_cycle)
                asyncio.create_task(do_checkout(session, host, port, user, product_id))
                sent += 1
                await asyncio.sleep(interval)

            print(f"Burst finished: sent ~{sent} events")
            next_burst = time.time() + burst_interval


async def pattern_loop(host: str, port: int, users: List[str], product_id: int,
                       burst_requests: int, burst_duration: float,
                       followup_requests: int, followup_duration: float,
                       cycle_interval: float):
    user_cycle = itertools.cycle(users)
    async with aiohttp.ClientSession() as session:
        while True:
            if burst_requests > 0 and burst_duration > 0:
                interval = burst_duration / float(burst_requests)
                sent = 0
                for _ in range(burst_requests):
                    user = next(user_cycle)
                    asyncio.create_task(do_checkout(session, host, port, user, product_id))
                    sent += 1
                    await asyncio.sleep(max(0.001, random.gauss(interval, interval * 0.05)))
                print(f"Pattern burst finished: sent {sent} events over {burst_duration}s")

            if followup_requests > 0 and followup_duration > 0:
                interval = followup_duration / float(followup_requests)
                sent2 = 0
                for _ in range(followup_requests):
                    user = next(user_cycle)
                    asyncio.create_task(do_checkout(session, host, port, user, product_id))
                    sent2 += 1
                    await asyncio.sleep(max(0.001, random.gauss(interval, interval * 0.05)))
                print(f"Pattern follow-up finished: sent {sent2} events over {followup_duration}s")

            if cycle_interval and cycle_interval > 0:
                await asyncio.sleep(cycle_interval)


def parse_users(arg: str) -> List[str]:
    if "-" in arg:
        a, b = arg.split("-", 1)
        return [str(i) for i in range(int(a), int(b) + 1)]
    return [x.strip() for x in arg.split(",") if x.strip()]


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--host", default="localhost")
    p.add_argument("--port", type=int, default=5002)
    p.add_argument("--users", default="1-100", help="User ids (range e.g. 1-100 or comma list)")
    p.add_argument("--product-id", type=int, default=1)
    p.add_argument("--burst-interval", type=float, default=3600.0, help="Seconds between bursts")
    p.add_argument("--burst-duration", type=float, default=300.0, help="Seconds a burst lasts")
    p.add_argument("--peak-rps", type=float, default=50.0, help="Peak requests/sec during burst")
    p.add_argument("--base-rps", type=float, default=0.2, help="Background requests/sec outside bursts")
    p.add_argument("--decay-power", type=float, default=2.0, help="Decay curve power (higher -> faster drop)")
    p.add_argument("--pattern", action="store_true", help="Enable pattern mode (use --burst-requests and --followup-requests)")
    p.add_argument("--burst-requests", type=int, default=0, help="Number of requests to send during the primary burst window")
    p.add_argument("--followup-requests", type=int, default=0, help="Number of requests to send during the follow-up window")
    p.add_argument("--followup-duration", type=float, default=60.0, help="Seconds for the follow-up window (default 60s)")
    p.add_argument("--cycle-interval", type=float, default=0.0, help="Seconds to wait after a full pattern cycle before starting the next; 0 repeats immediately")
    args = p.parse_args()

    users = parse_users(args.users)

    print(f"Simulating checkouts to http://{args.host}:{args.port}/ - users={len(users)}")
    try:
        if args.pattern and args.burst_requests > 0:
            asyncio.run(pattern_loop(
                args.host,
                args.port,
                users,
                args.product_id,
                args.burst_requests,
                args.burst_duration,
                args.followup_requests,
                args.followup_duration,
                args.cycle_interval,
            ))
        else:
            asyncio.run(burst_loop(args.host, args.port, users, args.product_id,
                                   args.burst_interval, args.burst_duration,
                                   args.peak_rps, args.base_rps, args.decay_power))
    except KeyboardInterrupt:
        print("Stopped by user")


if __name__ == "__main__":
    main()