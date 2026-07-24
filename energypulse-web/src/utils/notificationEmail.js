export function buildNotificationEmail(notification) {
  const homeName = notification.homeName || "Bilinmeyen ev";
  const applianceName = notification.applianceName || "Ev geneli";
  const subjectTarget = notification.applianceName || homeName;
  const sourceNote =
    notification.generationStatus === "FALLBACK"
      ? "Gemini servisine ulaşılamadığı için güvenli varsayılan öneri kullanıldı."
      : "Öneri Gemini tarafından oluşturuldu.";

  return {
    to: notification.recipientEmail || "—",
    subject: `[EnergyPulse] Enerji uyarısı - ${subjectTarget}`,
    body: [
      "EnergyPulse enerji uyarısı",
      "",
      `Ev: ${homeName}`,
      `Cihaz: ${applianceName}`,
      `Olay: ${notification.eventType || "ENERGY_EVENT"}`,
      `Zaman: ${notification.occurredAt || "—"}`,
      "",
      "Öneri:",
      notification.recommendationText || "Öneri metni bulunamadı.",
      "",
      sourceNote,
      "",
      "Bu mesaj otomatik olarak oluşturulmuştur.",
    ].join("\n"),
  };
}
