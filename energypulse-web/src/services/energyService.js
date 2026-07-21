import { mockHomes, mockNotifications } from "../data/mockEnergyData";

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export async function getHomesStatus() {
  await wait(350);
  return JSON.parse(JSON.stringify(mockHomes));
}

export async function getNotifications() {
  await wait(250);
  return JSON.parse(JSON.stringify(mockNotifications));
}