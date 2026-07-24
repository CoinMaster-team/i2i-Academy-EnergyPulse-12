import assert from "node:assert/strict";
import test from "node:test";

import { buildNotificationEmail } from "./notificationEmail.js";

test("builds the complete generated email content", () => {
  const email = buildNotificationEmail({
    homeName: "Home B",
    applianceName: "Heat Pump",
    recipientEmail: "demo@example.com",
    eventType: "APPLIANCE_ANOMALY_DETECTED",
    occurredAt: "2026-07-24T14:43:08Z",
    recommendationText: "1. Cihazı kapatın.\n2. Yetkili servisi arayın.",
    generationStatus: "GENERATED",
  });

  assert.equal(email.to, "demo@example.com");
  assert.equal(email.subject, "[EnergyPulse] Enerji uyarısı - Heat Pump");
  assert.match(email.body, /Ev: Home B/);
  assert.match(email.body, /Cihaz: Heat Pump/);
  assert.match(email.body, /1\. Cihazı kapatın\.\n2\. Yetkili servisi arayın\./);
  assert.match(email.body, /Öneri Gemini tarafından oluşturuldu\./);
  assert.match(email.body, /Bu mesaj otomatik olarak oluşturulmuştur\./);
});

test("builds the fallback email content for a home-level event", () => {
  const email = buildNotificationEmail({
    homeName: "Home A",
    applianceName: null,
    recipientEmail: "demo@example.com",
    eventType: "HOME_WARNING",
    occurredAt: "2026-07-24T15:00:00Z",
    recommendationText: "Tüketimi kontrol edin.",
    generationStatus: "FALLBACK",
  });

  assert.equal(email.subject, "[EnergyPulse] Enerji uyarısı - Home A");
  assert.match(email.body, /Cihaz: Ev geneli/);
  assert.match(
    email.body,
    /Gemini servisine ulaşılamadığı için güvenli varsayılan öneri kullanıldı\./
  );
});
