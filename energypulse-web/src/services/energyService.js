const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/$/, "");

const numberOrZero = (value) => {
  const parsedValue = Number(value);
  return Number.isFinite(parsedValue) ? parsedValue : 0;
};

export class ApiError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = "ApiError";
    this.status = details.status || 0;
    this.code = details.code || "REQUEST_FAILED";
    this.fieldErrors = details.fieldErrors || {};
  }
}

async function request(path, options = {}) {
  let response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new ApiError(
      "The EnergyPulse service could not be reached. Please try again.",
      { code: "NETWORK_ERROR" }
    );
  }

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : null;

  if (!response.ok) {
    throw new ApiError(
      payload?.message || "The request could not be completed.",
      {
        status: response.status,
        code: payload?.code,
        fieldErrors: payload?.fieldErrors,
      }
    );
  }

  return payload;
}

export function normalizeLiveHome(home = {}) {
  const appliances = Array.isArray(home.appliances) ? home.appliances : [];

  return {
    id: home.homeId,
    name: home.name || "Unnamed home",
    status: String(home.status || "NORMAL").toUpperCase(),
    monthlyBudget: numberOrZero(home.budgetLimit),
    currentCost: numberOrZero(home.accumulatedCost),
    budgetPercentage: numberOrZero(home.budgetPercentage),
    totalPowerWatts: numberOrZero(home.totalPowerWatts),
    dailyKwh: numberOrZero(home.accumulatedEnergyKwh),
    quotaPercentage: numberOrZero(home.quotaPercentage),
    source: "api",
    devices: appliances.map((appliance) => {
      const status = String(appliance.status || "NORMAL").toUpperCase();
      const currentWatt = numberOrZero(appliance.currentWatt);
      const safeLimitWatt = numberOrZero(appliance.safeLimitWatt);

      return {
        id: appliance.applianceId,
        name: appliance.name || "Unnamed device",
        powerWatts: currentWatt,
        safeLimitWatt,
        usagePercentage: numberOrZero(appliance.usagePercentage),
        status,
        measuredAt: appliance.measuredAt || null,
        anomalyReason:
          status === "ANOMALY"
            ? `Power remained above the ${safeLimitWatt.toLocaleString()} W safe limit.`
            : undefined,
      };
    }),
    history: [],
    updatedAt: home.updatedAt || null,
  };
}

export async function getHomesStatus() {
  const payload = await request("/api/telemetry/live");
  const homes = Array.isArray(payload) ? payload : payload?.homes;

  return Array.isArray(homes) ? homes.map(normalizeLiveHome) : [];
}

export function createHome(homeRequest) {
  return request("/api/homes", {
    method: "POST",
    body: JSON.stringify(homeRequest),
  });
}

export function getConsumptionHistory(homeId, dateRange = {}) {
  const searchParams = new URLSearchParams();

  if (dateRange.from) {
    searchParams.set("from", dateRange.from);
  }

  if (dateRange.to) {
    searchParams.set("to", dateRange.to);
  }

  const query = searchParams.toString();

  return request(
    `/api/homes/${encodeURIComponent(homeId)}/consumption-history${
      query ? `?${query}` : ""
    }`
  );
}

export function getNotifications() {
  return request("/api/notifications");
}

export function getNotification(notificationId) {
  return request(`/api/notifications/${encodeURIComponent(notificationId)}`);
}
