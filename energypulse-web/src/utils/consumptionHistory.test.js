import assert from "node:assert/strict";
import test from "node:test";

import { normalizeHistory } from "./consumptionHistory.js";

test("converts cumulative readings into sorted daily values", () => {
  const result = normalizeHistory([
    {
      date: "2026-07-23",
      totalEnergyKwh: 15.3,
      totalCost: 34.43,
    },
    {
      date: "2026-07-22",
      totalEnergyKwh: 7.2,
      totalCost: 16.2,
    },
    {
      date: "2026-07-24",
      totalEnergyKwh: 22.1,
      totalCost: 49.73,
    },
  ]);

  assert.deepEqual(
    result.map(({ date, kwh, cost }) => ({ date, kwh, cost })),
    [
      { date: "2026-07-22", kwh: 7.2, cost: 16.2 },
      { date: "2026-07-23", kwh: 8.1, cost: 18.23 },
      { date: "2026-07-24", kwh: 6.8, cost: 15.3 },
    ]
  );
});

test("never renders negative bars when a cumulative meter resets", () => {
  const result = normalizeHistory([
    {
      date: "2026-07-22",
      totalEnergyKwh: 10,
      totalCost: 20,
    },
    {
      date: "2026-07-23",
      totalEnergyKwh: 2,
      totalCost: 4,
    },
  ]);

  assert.equal(result[1].kwh, 0);
  assert.equal(result[1].cost, 0);
});
