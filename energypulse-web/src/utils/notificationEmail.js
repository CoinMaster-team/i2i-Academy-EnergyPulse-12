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

export function getFriendlyAiNotice(notification) {
  const error = String(notification.generationError || "").toLowerCase();

  if (!error) {
    return "";
  }

  if (
    error.includes("503") ||
    error.includes("unavailable") ||
    error.includes("high demand")
  ) {
    return "Gemini geçici olarak yoğun olduğu için güvenli varsayılan öneri kullanıldı.";
  }

  if (error.includes("429") || error.includes("resource_exhausted")) {
    return "Gemini kullanım limiti geçici olarak aşıldığı için güvenli varsayılan öneri kullanıldı.";
  }

  if (error.includes("incomplete") || error.includes("max_tokens")) {
    return "Gemini yanıtı tamamlanamadığı için güvenli varsayılan öneri kullanıldı.";
  }

  return notification.generationStatus === "FALLBACK"
    ? "Gemini öneri oluşturamadığı için güvenli varsayılan öneri kullanıldı."
    : "AI önerisi oluşturulurken geçici bir sorun oluştu.";
}
