import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  Home,
  Plus,
  Trash2,
  X,
  Zap,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import {
  ApiError,
  createHome,
  getConsumptionHistory,
  getHomesStatus,
} from "../services/energyService";
import DashboardHeader from "../components/DashboardHeader";
import DashboardNavigation from "../components/DashboardNavigation";
import "../App.css";

const createEmptyAppliance = () => ({
  name: "",
  safeLimitWatt: "",
  simulationMinWatt: "",
  simulationMaxWatt: "",
});

const LIVE_POLL_INTERVAL_MS = Math.max(
  Number(import.meta.env.VITE_LIVE_POLL_INTERVAL_MS) || 2000,
  1000
);

const createEmptyHomeForm = () => ({
  name: "",
  contactEmail: "",
  energyQuotaKwh: "",
  budgetLimit: "",
  baseTariff: "",
  penaltyTariff: "",
  appliances: [createEmptyAppliance()],
});

function normalizeCreatedHome(homeResponse) {
  return {
    id: homeResponse.id,
    name: homeResponse.name,
    contactEmail: homeResponse.contactEmail,
    status: "NORMAL",
    monthlyBudget: Number(homeResponse.budgetLimit || 0),
    currentCost: Number(homeResponse.accumulatedCost || 0),
    totalPowerWatts: 0,
    dailyKwh: Number(homeResponse.accumulatedEnergyKwh || 0),
    source: "api",
    devices: (homeResponse.appliances || []).map((appliance) => ({
      id: appliance.id,
      name: appliance.name,
      powerWatts: 0,
      status: "NORMAL",
      safeLimitWatt: Number(appliance.safeLimitWatt || 0),
      simulationMinWatt: Number(appliance.simulationMinWatt || 0),
      simulationMaxWatt: Number(appliance.simulationMaxWatt || 0),
    })),
    history: [],
  };
}

function normalizeHistory(historyResponse) {
  return (historyResponse || []).map((entry) => ({
    day: new Intl.DateTimeFormat("en", {
      month: "short",
      day: "numeric",
    }).format(new Date(`${entry.date}T00:00:00Z`)),
    date: entry.date,
    kwh: Number(entry.totalEnergyKwh || 0),
    cost: Number(entry.totalCost || 0),
  }));
}

function DashboardPage() {
  const [homes, setHomes] = useState([]);
  const [selectedHome, setSelectedHome] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);

  const [isDeviceFormOpen, setIsDeviceFormOpen] = useState(false);
  const [isWarningPanelOpen, setIsWarningPanelOpen] = useState(false);
  const [isHomeFormOpen, setIsHomeFormOpen] = useState(false);
  const [isCreatingHome, setIsCreatingHome] = useState(false);
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState("");
  const [homeForm, setHomeForm] = useState(createEmptyHomeForm);
  const [homeFormErrors, setHomeFormErrors] = useState({});

  const [newDevice, setNewDevice] = useState({
    name: "",
    powerWatts: "",
    status: "NORMAL",
  });

  const showToast = useCallback((message, type = "success") => {
    setToast({ message, type });
  }, []);

  const loadDashboard = useCallback(async ({ background = false } = {}) => {
    try {
      if (!background) {
        setError("");
        setIsRefreshing(true);
      }

      const homesData = await getHomesStatus();
      const safeHomes = Array.isArray(homesData) ? homesData : [];

      setHomes((currentHomes) => {
        return safeHomes.map((homeItem) => {
          const currentHome = currentHomes.find(
            (candidate) => candidate.id === homeItem.id
          );

          return currentHome?.history?.length
            ? { ...homeItem, history: currentHome.history }
            : homeItem;
        });
      });

      setSelectedHome((currentHome) => {
        if (!currentHome) {
          return currentHome;
        }

        const updatedHome = safeHomes.find(
          (homeItem) => homeItem.id === currentHome.id
        );

        if (!updatedHome) {
          return currentHome;
        }

        return currentHome.history?.length
          ? { ...updatedHome, history: currentHome.history }
          : updatedHome;
      });

      setLastUpdated(new Date());
    } catch (loadError) {
      console.error(loadError);
      if (!background) {
        setError("Dashboard data could not be loaded.");
      }
    } finally {
      if (!background) {
        setIsRefreshing(false);
      }
    }
  }, []);

  useEffect(() => {
    const loadTimer = window.setTimeout(() => {
      void loadDashboard();
    }, 0);
    const pollTimer = window.setInterval(() => {
      void loadDashboard({ background: true });
    }, LIVE_POLL_INTERVAL_MS);

    return () => {
      window.clearTimeout(loadTimer);
      window.clearInterval(pollTimer);
    };
  }, [loadDashboard]);

  useEffect(() => {
    if (!toast) {
      return undefined;
    }

    const toastTimer = window.setTimeout(() => setToast(null), 3500);
    return () => window.clearTimeout(toastTimer);
  }, [toast]);

  useEffect(() => {
    if (!selectedHome && !isWarningPanelOpen && !isHomeFormOpen) {
      return undefined;
    }

    const previousOverflow = document.body.style.overflow;

    const handleKeyDown = (event) => {
      if (event.key !== "Escape") {
        return;
      }

      setSelectedHome(null);
      setIsDeviceFormOpen(false);
      setIsWarningPanelOpen(false);
      setIsHomeFormOpen(false);
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [selectedHome, isWarningPanelOpen, isHomeFormOpen]);

  const totalPower = useMemo(
    () =>
      homes.reduce(
        (total, homeItem) => total + Number(homeItem.totalPowerWatts || 0),
        0
      ),
    [homes]
  );

  const warningHomes = useMemo(
    () =>
      homes
        .map((homeItem) => ({
          ...homeItem,
          anomalousDevices: (homeItem.devices || []).filter(
            (device) =>
              String(device.status || "NORMAL").toUpperCase() !== "NORMAL"
          ),
        }))
        .filter((homeItem) => homeItem.anomalousDevices.length > 0),
    [homes]
  );

  const warningCount = warningHomes.length;

  const loadHistory = async (homeItem) => {
    if (homeItem.source !== "api") {
      setHistoryError("");
      return;
    }

    try {
      setIsHistoryLoading(true);
      setHistoryError("");
      const historyResponse = await getConsumptionHistory(homeItem.id);
      const history = normalizeHistory(historyResponse);
      const latestDay = history.at(-1);
      const updatedHome = {
        ...homeItem,
        history,
        dailyKwh: latestDay?.kwh ?? homeItem.dailyKwh,
        currentCost: latestDay?.cost ?? homeItem.currentCost,
      };

      setSelectedHome((currentHome) =>
        currentHome?.id === homeItem.id ? updatedHome : currentHome
      );
      setHomes((currentHomes) =>
        currentHomes.map((currentHome) =>
          currentHome.id === homeItem.id ? updatedHome : currentHome
        )
      );
    } catch (historyLoadError) {
      setHistoryError(historyLoadError.message);
    } finally {
      setIsHistoryLoading(false);
    }
  };

  const openHomeDetails = (homeItem) => {
    setSelectedHome(homeItem);
    setIsDeviceFormOpen(false);
    setHistoryError("");
    setNewDevice({ name: "", powerWatts: "", status: "NORMAL" });
    void loadHistory(homeItem);
  };

  const closeHomeDetails = () => {
    setSelectedHome(null);
    setIsDeviceFormOpen(false);
    setHistoryError("");
    setNewDevice({ name: "", powerWatts: "", status: "NORMAL" });
  };

  const openHomeForm = () => {
    setHomeForm(createEmptyHomeForm());
    setHomeFormErrors({});
    setIsHomeFormOpen(true);
  };

  const closeHomeForm = () => {
    if (isCreatingHome) {
      return;
    }

    setIsHomeFormOpen(false);
    setHomeFormErrors({});
  };

  const handleHomeInputChange = (event) => {
    const { name, value } = event.target;
    setHomeForm((currentForm) => ({ ...currentForm, [name]: value }));
    setHomeFormErrors((currentErrors) => ({
      ...currentErrors,
      [name]: undefined,
    }));
  };

  const handleApplianceInputChange = (index, event) => {
    const { name, value } = event.target;
    const fieldName = `appliances[${index}].${name}`;

    setHomeForm((currentForm) => ({
      ...currentForm,
      appliances: currentForm.appliances.map((appliance, applianceIndex) =>
        applianceIndex === index ? { ...appliance, [name]: value } : appliance
      ),
    }));
    setHomeFormErrors((currentErrors) => ({
      ...currentErrors,
      [fieldName]: undefined,
    }));
  };

  const addApplianceField = () => {
    setHomeForm((currentForm) => ({
      ...currentForm,
      appliances: [...currentForm.appliances, createEmptyAppliance()],
    }));
  };

  const removeApplianceField = (index) => {
    setHomeForm((currentForm) => ({
      ...currentForm,
      appliances: currentForm.appliances.filter(
        (_, applianceIndex) => applianceIndex !== index
      ),
    }));
  };

  const validateHomeForm = () => {
    const validationErrors = {};

    if (Number(homeForm.penaltyTariff) <= Number(homeForm.baseTariff)) {
      validationErrors.penaltyTariff =
        "Penalty rate must be higher than standard rate.";
    }

    const applianceNames = new Set();
    homeForm.appliances.forEach((appliance, index) => {
      if (
        Number(appliance.simulationMaxWatt) <=
        Number(appliance.simulationMinWatt)
      ) {
        validationErrors[`appliances[${index}].simulationMaxWatt`] =
          "Max power must be higher than min power.";
      }

      const normalizedName = appliance.name.trim().toLowerCase();
      if (applianceNames.has(normalizedName)) {
        validationErrors[`appliances[${index}].name`] =
          "Device names must be unique.";
      }
      applianceNames.add(normalizedName);
    });

    setHomeFormErrors(validationErrors);
    return Object.keys(validationErrors).length === 0;
  };

  const handleCreateHome = async (event) => {
    event.preventDefault();

    if (!validateHomeForm()) {
      return;
    }

    const requestPayload = {
      name: homeForm.name.trim(),
      contactEmail: homeForm.contactEmail.trim(),
      energyQuotaKwh: Number(homeForm.energyQuotaKwh),
      budgetLimit: Number(homeForm.budgetLimit),
      baseTariff: Number(homeForm.baseTariff),
      penaltyTariff: Number(homeForm.penaltyTariff),
      appliances: homeForm.appliances.map((appliance) => ({
        name: appliance.name.trim(),
        safeLimitWatt: Number(appliance.safeLimitWatt),
        simulationMinWatt: Number(appliance.simulationMinWatt),
        simulationMaxWatt: Number(appliance.simulationMaxWatt),
      })),
    };

    try {
      setIsCreatingHome(true);
      setHomeFormErrors({});
      const createdHomeResponse = await createHome(requestPayload);
      const createdHome = normalizeCreatedHome(createdHomeResponse);

      setHomes((currentHomes) => [...currentHomes, createdHome]);
      setIsHomeFormOpen(false);
      setSelectedHome(createdHome);
      setLastUpdated(new Date());
      showToast(`${createdHome.name} was registered successfully.`);
      void loadHistory(createdHome);
    } catch (createError) {
      if (createError instanceof ApiError) {
        setHomeFormErrors(createError.fieldErrors);
      }
      showToast(createError.message, "error");
    } finally {
      setIsCreatingHome(false);
    }
  };

  const handleDeviceInputChange = (event) => {
    const { name, value } = event.target;
    setNewDevice((currentDevice) => ({ ...currentDevice, [name]: value }));
  };

  const handleAddDevice = (event) => {
    event.preventDefault();

    if (!selectedHome) {
      return;
    }

    const deviceName = newDevice.name.trim();
    const devicePower = Number(newDevice.powerWatts);

    if (!deviceName || devicePower <= 0) {
      setError("Please enter a valid device name and power value.");
      return;
    }

    const deviceToAdd = {
      id: Date.now(),
      name: deviceName,
      powerWatts: devicePower,
      status: newDevice.status,
    };
    const updatedSelectedHome = {
      ...selectedHome,
      devices: [...(selectedHome.devices || []), deviceToAdd],
      totalPowerWatts:
        Number(selectedHome.totalPowerWatts || 0) + devicePower,
      status:
        newDevice.status === "NORMAL" ? selectedHome.status : "WARNING",
    };

    setHomes((currentHomes) =>
      currentHomes.map((homeItem) =>
        homeItem.id === selectedHome.id ? updatedSelectedHome : homeItem
      )
    );
    setSelectedHome(updatedSelectedHome);
    setNewDevice({ name: "", powerWatts: "", status: "NORMAL" });
    setIsDeviceFormOpen(false);
    setError("");
  };

  return (
    <main className="app">
      <DashboardHeader
        isRefreshing={isRefreshing}
        lastUpdated={lastUpdated}
        onRefresh={loadDashboard}
      />
      <DashboardNavigation />

      {error && <div className="error-box">{error}</div>}

      <section className="summary-grid">
        <div className="summary-box homes">
          <Home />
          <span>{homes.length}</span>
          <p>Homes</p>
        </div>
        <div className="summary-box power">
          <Zap />
          <span>{totalPower.toLocaleString()} W</span>
          <p>Total Power</p>
        </div>
        <button
          className="summary-box warning warning-button"
          type="button"
          onClick={() => setIsWarningPanelOpen(true)}
        >
          <AlertTriangle />
          <span>{warningCount}</span>
          <p>Warnings</p>
          <small>View details →</small>
        </button>
      </section>

      <section className="homes-section">
        <div className="homes-section-heading">
          <div>
            <h2>Homes</h2>
            <p>Select a home to view its energy details.</p>
          </div>
          <button className="add-home-button" type="button" onClick={openHomeForm}>
            <Plus size={17} />
            Add Home
          </button>
        </div>

        {isRefreshing && homes.length === 0 ? (
          <div className="home-grid" aria-label="Loading homes">
            {[0, 1].map((skeletonItem) => (
              <div className="home-card home-card-skeleton" key={skeletonItem}>
                <span className="skeleton-block skeleton-icon" />
                <span className="skeleton-block skeleton-title" />
                <span className="skeleton-block skeleton-line" />
                <span className="skeleton-block skeleton-line short" />
              </div>
            ))}
          </div>
        ) : homes.length === 0 ? (
          <div className="empty-state">
            <Home size={30} />
            <h3>No homes found</h3>
            <p>Register your first home to start monitoring energy usage.</p>
          </div>
        ) : (
          <div className="home-grid">
            {homes.map((homeItem) => {
              const homeStatus = String(
                homeItem.status || "NORMAL"
              ).toUpperCase();
              const budgetPercentage = Math.min(
                Math.max(
                  Number(homeItem.budgetPercentage) ||
                    Math.round(
                      (Number(homeItem.currentCost || 0) /
                        Math.max(Number(homeItem.monthlyBudget || 0), 1)) *
                        100
                    ),
                  0
                ),
                100
              );

              return (
                <button
                  className="home-card"
                  type="button"
                  key={homeItem.id}
                  onClick={() => openHomeDetails(homeItem)}
                >
                  <div className="home-card-header">
                    <div className="home-icon">
                      <Home size={21} />
                    </div>
                    <span
                      className={`home-status ${homeStatus.toLowerCase()}`}
                    >
                      {homeStatus}
                    </span>
                  </div>
                  <h3>{homeItem.name}</h3>
                  <div className="home-card-values">
                    <div>
                      <span>Current Power</span>
                      <strong>
                        {Number(homeItem.totalPowerWatts || 0).toLocaleString()} W
                      </strong>
                    </div>
                    <div>
                      <span>Total Usage</span>
                      <strong>
                        {Number(homeItem.dailyKwh || 0).toFixed(1)} kWh
                      </strong>
                    </div>
                  </div>
                  <div className="budget-area">
                    <div className="budget-label">
                      <span>Energy budget</span>
                      <strong>{budgetPercentage}%</strong>
                    </div>
                    <div className="budget-track">
                      <div
                        className="budget-progress"
                        style={{ width: `${budgetPercentage}%` }}
                      />
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </section>

      {isWarningPanelOpen && (
        <div
          className="warning-panel-backdrop"
          onClick={() => setIsWarningPanelOpen(false)}
        >
          <aside
            className="warning-panel"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="warning-panel-title"
          >
            <div className="warning-panel-header">
              <div>
                <span className="warning-panel-label">
                  <AlertTriangle size={18} />
                  Warning details
                </span>
                <h2 id="warning-panel-title">Homes requiring attention</h2>
              </div>
              <button
                type="button"
                className="warning-panel-close"
                onClick={() => setIsWarningPanelOpen(false)}
                aria-label="Close warning panel"
              >
                <X size={20} />
              </button>
            </div>

            {warningHomes.length === 0 ? (
              <div className="all-normal-message">
                <Zap size={26} />
                <h3>All homes are operating normally.</h3>
                <p>No anomalous devices were detected.</p>
              </div>
            ) : (
              <div className="warning-home-list">
                {warningHomes.map((homeItem) => (
                  <article className="warning-home-card" key={homeItem.id}>
                    <div className="warning-home-header">
                      <div>
                        <Home size={19} />
                        <h3>{homeItem.name}</h3>
                      </div>
                      <span>
                        {homeItem.anomalousDevices.length}{" "}
                        {homeItem.anomalousDevices.length === 1
                          ? "anomaly"
                          : "anomalies"}
                      </span>
                    </div>
                    <div className="anomaly-device-list">
                      {homeItem.anomalousDevices.map((device) => (
                        <div className="anomaly-device" key={device.id}>
                          <div>
                            <strong>{device.name}</strong>
                            <p>
                              {device.anomalyReason ||
                                "Unusual energy consumption detected."}
                            </p>
                          </div>
                          <span>
                            {Number(device.powerWatts || 0).toLocaleString()} W
                          </span>
                        </div>
                      ))}
                    </div>
                  </article>
                ))}
              </div>
            )}
          </aside>
        </div>
      )}

      {isHomeFormOpen && (
        <div className="modal-backdrop" onClick={closeHomeForm}>
          <section
            className="create-home-modal"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="create-home-title"
          >
            <div className="modal-header">
              <div>
                <span className="modal-label">New home</span>
                <h2 id="create-home-title">Add Home</h2>
              </div>
              <button
                className="modal-close"
                type="button"
                onClick={closeHomeForm}
                aria-label="Close Add Home"
                disabled={isCreatingHome}
              >
                <X size={21} />
              </button>
            </div>

            <form className="create-home-form" onSubmit={handleCreateHome}>
              <div className="home-form-grid">
                <label>
                  Home Name
                  <input
                    name="name"
                    value={homeForm.name}
                    onChange={handleHomeInputChange}
                    maxLength="120"
                    required
                  />
                  {homeFormErrors.name && (
                    <small className="form-field-error">{homeFormErrors.name}</small>
                  )}
                </label>
                <label>
                  Email
                  <input
                    name="contactEmail"
                    type="email"
                    value={homeForm.contactEmail}
                    onChange={handleHomeInputChange}
                    required
                  />
                  {homeFormErrors.contactEmail && (
                    <small className="form-field-error">
                      {homeFormErrors.contactEmail}
                    </small>
                  )}
                </label>
                <label>
                  Energy Limit (kWh)
                  <input
                    name="energyQuotaKwh"
                    type="number"
                    min="0.0001"
                    step="0.0001"
                    value={homeForm.energyQuotaKwh}
                    onChange={handleHomeInputChange}
                    required
                  />
                  {homeFormErrors.energyQuotaKwh && (
                    <small className="form-field-error">
                      {homeFormErrors.energyQuotaKwh}
                    </small>
                  )}
                </label>
                <label>
                  Budget
                  <input
                    name="budgetLimit"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={homeForm.budgetLimit}
                    onChange={handleHomeInputChange}
                    required
                  />
                  {homeFormErrors.budgetLimit && (
                    <small className="form-field-error">
                      {homeFormErrors.budgetLimit}
                    </small>
                  )}
                </label>
                <label>
                  Standard Rate
                  <input
                    name="baseTariff"
                    type="number"
                    min="0"
                    step="0.000001"
                    value={homeForm.baseTariff}
                    onChange={handleHomeInputChange}
                    required
                  />
                  {homeFormErrors.baseTariff && (
                    <small className="form-field-error">
                      {homeFormErrors.baseTariff}
                    </small>
                  )}
                </label>
                <label>
                  Penalty Rate
                  <input
                    name="penaltyTariff"
                    type="number"
                    min="0.000001"
                    step="0.000001"
                    value={homeForm.penaltyTariff}
                    onChange={handleHomeInputChange}
                    required
                  />
                  {homeFormErrors.penaltyTariff && (
                    <small className="form-field-error">
                      {homeFormErrors.penaltyTariff}
                    </small>
                  )}
                </label>
              </div>

              <div className="appliances-form-heading">
                <div>
                  <h3>Devices</h3>
                  <p>Add at least one device.</p>
                </div>
                <button type="button" onClick={addApplianceField}>
                  <Plus size={15} />
                  Add Device
                </button>
              </div>

              <div className="appliances-form-list">
                {homeForm.appliances.map((appliance, index) => (
                  <div className="appliance-form-card" key={`appliance-${index}`}>
                    <div className="appliance-form-card-header">
                      <strong>Device {index + 1}</strong>
                      {homeForm.appliances.length > 1 && (
                        <button
                          type="button"
                          onClick={() => removeApplianceField(index)}
                          aria-label={`Remove device ${index + 1}`}
                        >
                          <Trash2 size={16} />
                        </button>
                      )}
                    </div>
                    <div className="appliance-form-grid">
                      {[
                        ["name", "Device Name", "text", undefined, undefined],
                        ["safeLimitWatt", "Power Limit (W)", "number", "0.01", "0.01"],
                        ["simulationMinWatt", "Min Power (W)", "number", "0", "0.01"],
                        ["simulationMaxWatt", "Max Power (W)", "number", "0.01", "0.01"],
                      ].map(([field, label, type, min, step]) => {
                        const errorKey = `appliances[${index}].${field}`;
                        return (
                          <label key={field}>
                            {label}
                            <input
                              name={field}
                              type={type}
                              min={min}
                              step={step}
                              value={appliance[field]}
                              onChange={(event) =>
                                handleApplianceInputChange(index, event)
                              }
                              required
                            />
                            {homeFormErrors[errorKey] && (
                              <small className="form-field-error">
                                {homeFormErrors[errorKey]}
                              </small>
                            )}
                          </label>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>

              {homeFormErrors.appliances && (
                <p className="form-field-error">{homeFormErrors.appliances}</p>
              )}

              <div className="create-home-actions">
                <button type="button" onClick={closeHomeForm} disabled={isCreatingHome}>
                  Cancel
                </button>
                <button type="submit" disabled={isCreatingHome}>
                  {isCreatingHome ? "Saving..." : "Save Home"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}

      {selectedHome && (
        <div className="modal-backdrop" onClick={closeHomeDetails}>
          <section
            className="home-details-modal"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="home-details-title"
          >
            <div className="modal-header">
              <div>
                <span className="modal-label">Home details</span>
                <h2 id="home-details-title">{selectedHome.name}</h2>
              </div>
              <button
                className="modal-close"
                type="button"
                onClick={closeHomeDetails}
                aria-label="Close home details"
              >
                <X size={21} />
              </button>
            </div>

            <div className="home-detail-summary">
              <div>
                <span>Current Power</span>
                <strong>
                  {Number(selectedHome.totalPowerWatts || 0).toLocaleString()} W
                </strong>
              </div>
              <div>
                <span>Total Usage</span>
                <strong>
                  {Number(selectedHome.dailyKwh || 0).toFixed(1)} kWh
                </strong>
              </div>
              <div>
                <span>Status</span>
                <strong
                  className={`detail-status ${String(
                    selectedHome.status || "NORMAL"
                  ).toLowerCase()}`}
                >
                  {selectedHome.status || "NORMAL"}
                </strong>
              </div>
            </div>

            <div className="chart-card">
              <div className="chart-heading">
                <div>
                  <h3>Weekly Consumption</h3>
                  <p>Daily energy usage and cost for the selected home</p>
                </div>
              </div>

              <div className="chart-container">
                {isHistoryLoading ? (
                  <div className="chart-loading" aria-label="Loading consumption history">
                    <span className="skeleton-block" />
                  </div>
                ) : historyError ? (
                  <div className="chart-message error">{historyError}</div>
                ) : (selectedHome.history || []).length === 0 ? (
                  <div className="chart-message">
                    Consumption history will appear when data is available.
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={selectedHome.history || []}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="day" axisLine={false} tickLine={false} />
                      <YAxis axisLine={false} tickLine={false} width={38} />
                      <Tooltip />
                      <Legend />
                      <Bar
                        dataKey="kwh"
                        name="Energy (kWh)"
                        fill="#2563eb"
                        radius={[6, 6, 0, 0]}
                      />
                      <Bar
                        dataKey="cost"
                        name="Cost"
                        fill="#93c5fd"
                        radius={[6, 6, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>

            <div className="devices-section">
              <div className="devices-heading">
                <div>
                  <h3>Devices</h3>
                  <p>{(selectedHome.devices || []).length} connected devices</p>
                </div>
                <button
                  className="add-device-button"
                  type="button"
                  onClick={() =>
                    setIsDeviceFormOpen((currentValue) => !currentValue)
                  }
                >
                  {isDeviceFormOpen ? (
                    "Cancel"
                  ) : (
                    <>
                      <Plus size={16} />
                      Add Device
                    </>
                  )}
                </button>
              </div>

              {isDeviceFormOpen && (
                <form className="device-form" onSubmit={handleAddDevice}>
                  <label>
                    Device name
                    <input
                      type="text"
                      name="name"
                      value={newDevice.name}
                      onChange={handleDeviceInputChange}
                      placeholder="Example: Refrigerator"
                      required
                    />
                  </label>
                  <label>
                    Power
                    <input
                      type="number"
                      name="powerWatts"
                      value={newDevice.powerWatts}
                      onChange={handleDeviceInputChange}
                      placeholder="Watts"
                      min="1"
                      required
                    />
                  </label>
                  <label>
                    Status
                    <select
                      name="status"
                      value={newDevice.status}
                      onChange={handleDeviceInputChange}
                    >
                      <option value="NORMAL">Normal</option>
                      <option value="WARNING">Warning</option>
                      <option value="ANOMALY">Anomaly</option>
                    </select>
                  </label>
                  <button type="submit">Save Device</button>
                </form>
              )}

              <div className="device-table-scroll">
                <table className="device-table">
                  <thead>
                    <tr>
                      <th>Device</th>
                      <th>Power</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(selectedHome.devices || []).length === 0 ? (
                      <tr>
                        <td colSpan="3" className="empty-table-message">
                          No devices found.
                        </td>
                      </tr>
                    ) : (
                      selectedHome.devices.map((device) => {
                        const deviceStatus = String(
                          device.status || "NORMAL"
                        ).toUpperCase();

                        return (
                          <tr key={device.id}>
                            <td>{device.name}</td>
                            <td>
                              {Number(device.powerWatts || 0).toLocaleString()} W
                            </td>
                            <td>
                              <span
                                className={`table-status ${deviceStatus.toLowerCase()}`}
                              >
                                {deviceStatus}
                              </span>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </div>
      )}

      {toast && (
        <div className={`toast ${toast.type}`} role="status">
          {toast.type === "success" ? (
            <CheckCircle2 size={19} />
          ) : (
            <AlertTriangle size={19} />
          )}
          <span>{toast.message}</span>
          <button type="button" onClick={() => setToast(null)} aria-label="Close message">
            <X size={16} />
          </button>
        </div>
      )}
    </main>
  );
}

export default DashboardPage;
