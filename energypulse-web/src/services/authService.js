const API_BASE_URL = (
  import.meta.env?.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/$/, "");

const TOKEN_KEY = "energyPulseAuthToken";
const USER_KEY = "energyPulseAuthUser";
export const AUTH_EXPIRED_EVENT = "energypulse:auth-expired";

export class AuthApiError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = "AuthApiError";
    this.status = details.status || 0;
    this.code = details.code || "AUTH_REQUEST_FAILED";
    this.fieldErrors = details.fieldErrors || {};
  }
}

export function getAuthToken(storage = globalThis.localStorage) {
  return storage?.getItem(TOKEN_KEY) || "";
}

export function getStoredUser(storage = globalThis.localStorage) {
  const serializedUser = storage?.getItem(USER_KEY);
  if (!serializedUser) {
    return null;
  }

  try {
    return JSON.parse(serializedUser);
  } catch {
    storage?.removeItem(USER_KEY);
    return null;
  }
}

export function saveAuthSession(
  authResponse,
  storage = globalThis.localStorage
) {
  if (!authResponse?.token || !authResponse?.user) {
    throw new AuthApiError("The authentication response was incomplete.");
  }

  storage?.setItem(TOKEN_KEY, authResponse.token);
  storage?.setItem(USER_KEY, JSON.stringify(authResponse.user));
}

export function saveStoredUser(user, storage = globalThis.localStorage) {
  if (user) {
    storage?.setItem(USER_KEY, JSON.stringify(user));
  }
}

export function clearAuthSession(storage = globalThis.localStorage) {
  storage?.removeItem(TOKEN_KEY);
  storage?.removeItem(USER_KEY);

  storage?.removeItem("energyPulseRegisteredUser");
  storage?.removeItem("energyPulseLoggedIn");
  storage?.removeItem("energyPulseUserName");
}

export function notifyAuthExpired() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
  }
}

async function authRequest(path, options = {}) {
  const token = getAuthToken();
  let response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new AuthApiError(
      "The EnergyPulse service could not be reached. Please try again.",
      { code: "NETWORK_ERROR" }
    );
  }

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : null;

  if (!response.ok) {
    throw new AuthApiError(
      payload?.message || "The authentication request could not be completed.",
      {
        status: response.status,
        code: payload?.code,
        fieldErrors: payload?.fieldErrors,
      }
    );
  }

  return payload;
}

export function registerUser(registration) {
  return authRequest("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(registration),
  });
}

export function loginUser(credentials) {
  return authRequest("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(credentials),
  });
}

export function getCurrentUser() {
  return authRequest("/api/auth/me");
}

export async function logoutUser() {
  try {
    if (getAuthToken()) {
      await authRequest("/api/auth/logout", { method: "POST" });
    }
  } finally {
    clearAuthSession();
  }
}
