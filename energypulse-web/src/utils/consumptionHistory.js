export function normalizeHistory(historyResponse) {
  let previousEnergy = 0;
  let previousCost = 0;

  return [...(historyResponse || [])]
    .sort((left, right) => String(left.date).localeCompare(String(right.date)))
    .map((entry) => {
      const totalEnergy = Number(entry.totalEnergyKwh || 0);
      const totalCost = Number(entry.totalCost || 0);
      const dailyEnergy = Math.max(0, totalEnergy - previousEnergy);
      const dailyCost = Math.max(0, totalCost - previousCost);

      previousEnergy = totalEnergy;
      previousCost = totalCost;

      return {
        day: new Intl.DateTimeFormat("en", {
          month: "short",
          day: "numeric",
        }).format(new Date(`${entry.date}T00:00:00Z`)),
        date: entry.date,
        kwh: Number(dailyEnergy.toFixed(2)),
        cost: Number(dailyCost.toFixed(2)),
        totalEnergyKwh: totalEnergy,
        totalCost,
      };
    });
}
