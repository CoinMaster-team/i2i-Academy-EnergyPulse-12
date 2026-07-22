import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  Home,
  Plus,
  RefreshCw,
  X,
  Zap,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

import {
  Bar,
  BarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { getHomesStatus } from "../services/energyService";
import "../App.css";

function DashboardPage() {
  const navigate = useNavigate();

  const userName =
    localStorage.getItem("energyPulseUserName") || "EnergyPulse User";

  const [homes, setHomes] = useState([]);
  const [selectedHome, setSelectedHome] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState("");

  const [isDeviceFormOpen, setIsDeviceFormOpen] = useState(false);
  const [isWarningPanelOpen, setIsWarningPanelOpen] = useState(false);

  const [newDevice, setNewDevice] = useState({
    name: "",
    powerWatts: "",
    status: "NORMAL",
  });

  const handleLogout = () => {
    localStorage.removeItem("energyPulseLoggedIn");
    localStorage.removeItem("energyPulseUserName");

    navigate("/login", { replace: true });
  };

  const loadDashboard = useCallback(async () => {
    try {
      setError("");
      setIsRefreshing(true);

      const homesData = await getHomesStatus();
      const safeHomes = Array.isArray(homesData) ? homesData : [];

      setHomes(safeHomes);

      setSelectedHome((currentHome) => {
        if (!currentHome) {
          return null;
        }

        return (
          safeHomes.find((homeItem) => homeItem.id === currentHome.id) ||
          currentHome
        );
      });

      setLastUpdated(new Date());
    } catch (loadError) {
      console.error(loadError);
      setError("Dashboard data could not be loaded.");
    } finally {
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    const loadTimer = window.setTimeout(loadDashboard, 0);

    return () => window.clearTimeout(loadTimer);
  }, [loadDashboard]);

  useEffect(() => {
    if (!selectedHome && !isWarningPanelOpen) {
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
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [selectedHome, isWarningPanelOpen]);

  const totalPower = useMemo(() => {
    return homes.reduce(
      (total, homeItem) => total + Number(homeItem.totalPowerWatts || 0),
      0
    );
  }, [homes]);

  const warningHomes = useMemo(() => {
    return homes
      .map((homeItem) => {
        const anomalousDevices = (homeItem.devices || []).filter(
          (device) =>
            String(device.status || "NORMAL").toUpperCase() !== "NORMAL"
        );

        return {
          ...homeItem,
          anomalousDevices,
        };
      })
      .filter((homeItem) => homeItem.anomalousDevices.length > 0);
  }, [homes]);

  const warningCount = warningHomes.length;

  const openHomeDetails = (homeItem) => {
    setSelectedHome(homeItem);
    setIsDeviceFormOpen(false);

    setNewDevice({
      name: "",
      powerWatts: "",
      status: "NORMAL",
    });
  };

  const closeHomeDetails = () => {
    setSelectedHome(null);
    setIsDeviceFormOpen(false);

    setNewDevice({
      name: "",
      powerWatts: "",
      status: "NORMAL",
    });
  };

  const handleDeviceInputChange = (event) => {
    const { name, value } = event.target;

    setNewDevice((currentDevice) => ({
      ...currentDevice,
      [name]: value,
    }));
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

    const updatedDevices = [
      ...(selectedHome.devices || []),
      deviceToAdd,
    ];

    const updatedSelectedHome = {
      ...selectedHome,
      devices: updatedDevices,
      totalPowerWatts:
        Number(selectedHome.totalPowerWatts || 0) + devicePower,
      status:
        newDevice.status === "NORMAL"
          ? selectedHome.status
          : "WARNING",
    };

    setHomes((currentHomes) =>
      currentHomes.map((homeItem) =>
        homeItem.id === selectedHome.id
          ? updatedSelectedHome
          : homeItem
      )
    );

    setSelectedHome(updatedSelectedHome);

    setNewDevice({
      name: "",
      powerWatts: "",
      status: "NORMAL",
    });

    setIsDeviceFormOpen(false);
    setError("");
  };

  return (
    <main className="app">
      <header className="topbar">
        <div className="dashboard-title-area">
          <h1 className="dashboard-logo">EnergyPulse</h1>
          <span className="welcome-user">
            Welcome, {userName}
          </span>
        </div>

        <div className="dashboard-header-right">
          <div className="dashboard-header-actions">
            <button
              className="refresh-button"
              type="button"
              onClick={loadDashboard}
              disabled={isRefreshing}
            >
              <RefreshCw
                size={17}
                className={isRefreshing ? "spinning" : ""}
              />

              {isRefreshing ? "Refreshing..." : "Refresh"}
            </button>

            <button
              className="logout-button"
              type="button"
              onClick={handleLogout}
            >
              Logout
            </button>
          </div>

          <span className="last-updated">
            Last updated:{" "}
            {lastUpdated
              ? lastUpdated.toLocaleTimeString("en-GB", {
                  hour: "2-digit",
                  minute: "2-digit",
                  second: "2-digit",
                })
              : "--:--:--"}
          </span>
        </div>
      </header>

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
        </div>

        {homes.length === 0 && !isRefreshing ? (
          <div className="empty-state">
            <Home size={30} />
            <h3>No homes found</h3>
            <p>Home data will appear here when available.</p>
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
                        {Number(
                          homeItem.totalPowerWatts || 0
                        ).toLocaleString()}{" "}
                        W
                      </strong>
                    </div>

                    <div>
                      <span>Daily Usage</span>
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
                  <article
                    className="warning-home-card"
                    key={homeItem.id}
                  >
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
                        <div
                          className="anomaly-device"
                          key={device.id}
                        >
                          <div>
                            <strong>{device.name}</strong>

                            <p>
                              {device.anomalyReason ||
                                "Unusual energy consumption detected."}
                            </p>
                          </div>

                          <span>
                            {Number(
                              device.powerWatts || 0
                            ).toLocaleString()}{" "}
                            W
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

      {selectedHome && (
        <div
          className="modal-backdrop"
          onClick={closeHomeDetails}
        >
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
                  {Number(
                    selectedHome.totalPowerWatts || 0
                  ).toLocaleString()}{" "}
                  W
                </strong>
              </div>

              <div>
                <span>Daily Usage</span>
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
                  <p>Energy usage for the selected home</p>
                </div>
              </div>

              <div className="chart-container">
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={selectedHome.history || []}>
                    <XAxis
                      dataKey="day"
                      axisLine={false}
                      tickLine={false}
                    />

                    <YAxis
                      axisLine={false}
                      tickLine={false}
                      width={36}
                    />

                    <Tooltip />

                    <Bar
                      dataKey="kwh"
                      fill="#2563eb"
                      radius={[6, 6, 0, 0]}
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="devices-section">
              <div className="devices-heading">
                <div>
                  <h3>Devices</h3>
                  <p>
                    {(selectedHome.devices || []).length} connected
                    devices
                  </p>
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
                <form
                  className="device-form"
                  onSubmit={handleAddDevice}
                >
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
                              {Number(
                                device.powerWatts || 0
                              ).toLocaleString()}{" "}
                              W
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
    </main>
  );
}

export default DashboardPage;
