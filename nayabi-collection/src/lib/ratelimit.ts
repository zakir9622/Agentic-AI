import { Ratelimit } from "@upstash/ratelimit";
import { Redis } from "@upstash/redis";

function makeRatelimit(requests: number, window: `${number} s` | `${number} m` | `${number} h`) {
  if (!process.env.UPSTASH_REDIS_REST_URL || !process.env.UPSTASH_REDIS_REST_TOKEN) {
    return null;
  }
  const redis = new Redis({
    url: process.env.UPSTASH_REDIS_REST_URL,
    token: process.env.UPSTASH_REDIS_REST_TOKEN,
  });
  return new Ratelimit({
    redis,
    limiter: Ratelimit.slidingWindow(requests, window),
    analytics: false,
  });
}

export const checkoutRatelimit = makeRatelimit(5, "1 m");
export const authRatelimit = makeRatelimit(10, "1 m");
export const apiRatelimit = makeRatelimit(30, "1 m");

export async function ratelimitCheck(
  limiter: ReturnType<typeof makeRatelimit>,
  identifier: string
): Promise<boolean> {
  if (!limiter) return true; // no-op if Redis not configured
  const { success } = await limiter.limit(identifier);
  return success;
}
