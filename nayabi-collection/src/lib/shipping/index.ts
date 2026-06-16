import type { CarrierId, ShippingProvider } from "./types";
import { shiprocketProvider } from "./shiprocket";
import { delhiveryProvider } from "./delhivery";
import { amazonProvider } from "./amazon";

export * from "./types";

const PROVIDERS: Record<CarrierId, ShippingProvider> = {
  shiprocket: shiprocketProvider,
  delhivery: delhiveryProvider,
  amazon: amazonProvider,
};

export function getProvider(id: string): ShippingProvider | null {
  return (PROVIDERS as Record<string, ShippingProvider>)[id] ?? null;
}

export function listProviders(): ShippingProvider[] {
  return Object.values(PROVIDERS);
}

/** Carriers with their credentials configured, in priority order. */
export function configuredProviders(): ShippingProvider[] {
  return listProviders().filter((p) => p.isConfigured());
}

/** First configured carrier, falling back to Shiprocket. */
export function defaultProvider(): ShippingProvider {
  return configuredProviders()[0] ?? shiprocketProvider;
}
