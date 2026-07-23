import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Bot,
  CheckCircle2,
  CircleAlert,
  Clock3,
  MailCheck,
  Search,
  TriangleAlert,
  X,
} from "lucide-react";

import DashboardHeader from "../components/DashboardHeader";
import DashboardNavigation from "../components/DashboardNavigation";
import { getNotifications } from "../services/energyService";
import "../App.css";

const ALL_FILTER = "ALL";

function formatDate(value, includeSeconds = false) {
  if (!value) {
    return "—";
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return "—";
  }

  return new Intl.DateTimeFormat("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    ...(includeSeconds ? { second: "2-digit" } : {}),
  }).format(parsedDate);
}

function humanizeEventType(eventType) {
  return String(eventType || "Energy event")
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function StatusBadge({ type, value }) {
  const normalizedValue = String(value || "UNKNOWN").toUpperCase();
  return (
    <span
      className={`notification-status notification-status-${type} notification-status-${normalizedValue.toLowerCase()}`}
    >
      {normalizedValue}
    </span>
  );
}

function NotificationConsolePage() {
  const [notifications, setNotifications] = useState([]);
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [lastUpdated, setLastUpdated] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [emailFilter, setEmailFilter] = useState(ALL_FILTER);
  const [generationFilter, setGenerationFilter] = useState(ALL_FILTER);

  const loadNotifications = useCallback(async () => {
    try {
      setError("");
      setIsLoading(true);
      const response = await getNotifications();
      setNotifications(Array.isArray(response) ? response : []);
      setLastUpdated(new Date());
    } catch (loadError) {
      console.error(loadError);
      setError(loadError.message || "Notification history could not be loaded.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const loadTimer = window.setTimeout(loadNotifications, 0);
    return () => window.clearTimeout(loadTimer);
  }, [loadNotifications]);

  useEffect(() => {
    if (!selectedNotification) {
      return undefined;
    }

    const previousOverflow = document.body.style.overflow;
    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        setSelectedNotification(null);
      }
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [selectedNotification]);

  const summary = useMemo(
    () => ({
      total: notifications.length,
      sent: notifications.filter((item) => item.emailStatus === "SENT").length,
      failed: notifications.filter((item) => item.emailStatus === "FAILED").length,
      fallback: notifications.filter(
        (item) => item.generationStatus === "FALLBACK"
      ).length,
    }),
    [notifications]
  );

  const homeOptions = useMemo(
    () => [...new Set(notifications.map((item) => item.homeName).filter(Boolean))],
    [notifications]
  );
  const [homeFilter, setHomeFilter] = useState(ALL_FILTER);

  const filteredNotifications = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase();

    return notifications.filter((item) => {
      const matchesSearch =
        !normalizedSearch ||
        [
          item.homeName,
          item.applianceName,
          item.recipientEmail,
          item.eventType,
          item.recommendationText,
        ].some((value) =>
          String(value || "")
            .toLowerCase()
            .includes(normalizedSearch)
        );
      const matchesEmail =
        emailFilter === ALL_FILTER || item.emailStatus === emailFilter;
      const matchesGeneration =
        generationFilter === ALL_FILTER ||
        item.generationStatus === generationFilter;
      const matchesHome =
        homeFilter === ALL_FILTER || item.homeName === homeFilter;

      return (
        matchesSearch && matchesEmail && matchesGeneration && matchesHome
      );
    });
  }, [emailFilter, generationFilter, homeFilter, notifications, searchTerm]);

  return (
    <main className="app notification-console-page">
      <DashboardHeader
        isRefreshing={isLoading}
        lastUpdated={lastUpdated}
        onRefresh={loadNotifications}
      />
      <DashboardNavigation />

      <section className="notification-page-heading">
        <div>
          <span className="section-eyebrow">Notification history</span>
          <h2>Notification Console</h2>
          <p>Review AI recommendations and email delivery results.</p>
        </div>
        <div className="notification-heading-mark" aria-hidden="true">
          <MailCheck size={24} />
        </div>
      </section>

      {error && (
        <div className="notification-error" role="alert">
          <CircleAlert size={19} />
          <div>
            <strong>Could not load notifications</strong>
            <span>{error}</span>
          </div>
          <button type="button" onClick={loadNotifications}>
            Try again
          </button>
        </div>
      )}

      <section className="notification-summary-grid" aria-label="Notification summary">
        <article className="notification-summary-card">
          <span className="notification-summary-icon total">
            <MailCheck size={20} />
          </span>
          <div>
            <strong>{summary.total}</strong>
            <span>Total notifications</span>
          </div>
        </article>
        <article className="notification-summary-card">
          <span className="notification-summary-icon sent">
            <CheckCircle2 size={20} />
          </span>
          <div>
            <strong>{summary.sent}</strong>
            <span>Email sent</span>
          </div>
        </article>
        <article className="notification-summary-card">
          <span className="notification-summary-icon failed">
            <TriangleAlert size={20} />
          </span>
          <div>
            <strong>{summary.failed}</strong>
            <span>Email failed</span>
          </div>
        </article>
        <article className="notification-summary-card">
          <span className="notification-summary-icon fallback">
            <Bot size={20} />
          </span>
          <div>
            <strong>{summary.fallback}</strong>
            <span>AI fallback</span>
          </div>
        </article>
      </section>

      <section className="notification-list-panel">
        <div className="notification-filter-bar">
          <label className="notification-search-field">
            <Search size={18} />
            <span className="sr-only">Search notifications</span>
            <input
              type="search"
              value={searchTerm}
              placeholder="Search home, device or email..."
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </label>
          <label>
            <span>Email status</span>
            <select
              value={emailFilter}
              onChange={(event) => setEmailFilter(event.target.value)}
            >
              <option value={ALL_FILTER}>All email statuses</option>
              <option value="SENT">Sent</option>
              <option value="PENDING">Pending</option>
              <option value="FAILED">Failed</option>
            </select>
          </label>
          <label>
            <span>AI status</span>
            <select
              value={generationFilter}
              onChange={(event) => setGenerationFilter(event.target.value)}
            >
              <option value={ALL_FILTER}>All AI statuses</option>
              <option value="GENERATED">Generated</option>
              <option value="FALLBACK">Fallback</option>
            </select>
          </label>
          <label>
            <span>Home</span>
            <select
              value={homeFilter}
              onChange={(event) => setHomeFilter(event.target.value)}
            >
              <option value={ALL_FILTER}>All homes</option>
              {homeOptions.map((homeName) => (
                <option key={homeName} value={homeName}>
                  {homeName}
                </option>
              ))}
            </select>
          </label>
        </div>

        {isLoading && notifications.length === 0 ? (
          <div className="notification-loading" aria-label="Loading notifications">
            {[0, 1, 2, 3].map((item) => (
              <span className="notification-loading-row" key={item} />
            ))}
          </div>
        ) : filteredNotifications.length === 0 ? (
          <div className="notification-empty-state">
            <MailCheck size={30} />
            <h3>{notifications.length === 0 ? "No notifications yet" : "No matching notifications"}</h3>
            <p>
              {notifications.length === 0
                ? "AI recommendations and email results will appear here when an energy event is processed."
                : "Try changing the search term or filters."}
            </p>
          </div>
        ) : (
          <>
            <div className="notification-table-wrap">
              <table className="notification-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Home &amp; device</th>
                    <th>Event</th>
                    <th>AI</th>
                    <th>Email</th>
                    <th aria-label="Details" />
                  </tr>
                </thead>
                <tbody>
                  {filteredNotifications.map((notification) => (
                    <tr key={notification.id}>
                      <td>
                        <span className="notification-date">
                          <Clock3 size={15} />
                          {formatDate(notification.createdAt)}
                        </span>
                      </td>
                      <td>
                        <strong>{notification.homeName}</strong>
                        <span>{notification.applianceName || "Home level event"}</span>
                      </td>
                      <td>{humanizeEventType(notification.eventType)}</td>
                      <td>
                        <StatusBadge type="generation" value={notification.generationStatus} />
                      </td>
                      <td>
                        <StatusBadge type="email" value={notification.emailStatus} />
                      </td>
                      <td>
                        <button
                          className="notification-view-button"
                          type="button"
                          onClick={() => setSelectedNotification(notification)}
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="notification-mobile-list">
              {filteredNotifications.map((notification) => (
                <button
                  className="notification-mobile-card"
                  type="button"
                  key={notification.id}
                  onClick={() => setSelectedNotification(notification)}
                >
                  <span className="notification-mobile-card-top">
                    <strong>{notification.homeName}</strong>
                    <span>{formatDate(notification.createdAt)}</span>
                  </span>
                  <span className="notification-mobile-device">
                    {notification.applianceName || "Home level event"}
                  </span>
                  <span className="notification-mobile-statuses">
                    <StatusBadge type="generation" value={notification.generationStatus} />
                    <StatusBadge type="email" value={notification.emailStatus} />
                  </span>
                </button>
              ))}
            </div>
          </>
        )}
      </section>

      {selectedNotification && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              setSelectedNotification(null);
            }
          }}
        >
          <section
            className="notification-detail-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="notification-detail-title"
          >
            <div className="modal-header">
              <div>
                <span className="section-eyebrow">Notification details</span>
                <h2 id="notification-detail-title">
                  {selectedNotification.homeName}
                </h2>
                <p>
                  {selectedNotification.applianceName || "Home level event"}
                </p>
              </div>
              <button
                className="modal-close"
                type="button"
                aria-label="Close notification details"
                onClick={() => setSelectedNotification(null)}
              >
                <X size={22} />
              </button>
            </div>

            <div className="notification-detail-statuses">
              <StatusBadge type="generation" value={selectedNotification.generationStatus} />
              <StatusBadge type="email" value={selectedNotification.emailStatus} />
            </div>

            <div className="notification-detail-section">
              <span>Energy event</span>
              <strong>{humanizeEventType(selectedNotification.eventType)}</strong>
              <p>
                The event was detected at {formatDate(selectedNotification.occurredAt, true)}.
              </p>
            </div>

            <div className="notification-recommendation">
              <span>
                <Bot size={17} /> AI recommendation
              </span>
              <p>{selectedNotification.recommendationText}</p>
            </div>

            <dl className="notification-detail-grid">
              <div>
                <dt>Recipient</dt>
                <dd>{selectedNotification.recipientEmail}</dd>
              </div>
              <div>
                <dt>Provider</dt>
                <dd>{selectedNotification.provider}</dd>
              </div>
              <div>
                <dt>Created</dt>
                <dd>{formatDate(selectedNotification.createdAt, true)}</dd>
              </div>
              <div>
                <dt>Sent</dt>
                <dd>{formatDate(selectedNotification.sentAt, true)}</dd>
              </div>
            </dl>

            {(selectedNotification.generationError || selectedNotification.emailError) && (
              <div className="notification-detail-errors">
                {selectedNotification.generationError && (
                  <p><strong>AI error:</strong> {selectedNotification.generationError}</p>
                )}
                {selectedNotification.emailError && (
                  <p><strong>Email error:</strong> {selectedNotification.emailError}</p>
                )}
              </div>
            )}
          </section>
        </div>
      )}
    </main>
  );
}

export default NotificationConsolePage;
