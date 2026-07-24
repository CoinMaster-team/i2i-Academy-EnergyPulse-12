import assert from "node:assert/strict";
import test from "node:test";

import {
  clearAuthSession,
  getAuthToken,
  getStoredUser,
  saveAuthSession,
} from "./authService.js";

function createMemoryStorage() {
  const values = new Map();

  return {
    getItem(key) {
      return values.has(key) ? values.get(key) : null;
    },
    setItem(key, value) {
      values.set(key, String(value));
    },
    removeItem(key) {
      values.delete(key);
    },
  };
}

test("stores only the opaque token and safe user summary", () => {
  const storage = createMemoryStorage();

  saveAuthSession(
    {
      token: "opaque-session-token",
      user: {
        id: "user-id",
        fullName: "Energy User",
        email: "user@example.com",
      },
    },
    storage
  );

  assert.equal(getAuthToken(storage), "opaque-session-token");
  assert.deepEqual(getStoredUser(storage), {
    id: "user-id",
    fullName: "Energy User",
    email: "user@example.com",
  });
  assert.equal(storage.getItem("energyPulseRegisteredUser"), null);
});

test("clears current and legacy authentication data", () => {
  const storage = createMemoryStorage();
  storage.setItem("energyPulseAuthToken", "token");
  storage.setItem("energyPulseAuthUser", "{}");
  storage.setItem("energyPulseRegisteredUser", '{"password":"unsafe"}');
  storage.setItem("energyPulseLoggedIn", "true");

  clearAuthSession(storage);

  assert.equal(getAuthToken(storage), "");
  assert.equal(getStoredUser(storage), null);
  assert.equal(storage.getItem("energyPulseRegisteredUser"), null);
  assert.equal(storage.getItem("energyPulseLoggedIn"), null);
});
