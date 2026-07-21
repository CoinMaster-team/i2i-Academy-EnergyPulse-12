import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  Bell,
  Home,
  Mail,
  RefreshCw,
  Zap,
} from "lucide-react";
import {
  Bar,
  BarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { getHomesStatus, getNotifications } from "./services/energyService";
import "./App.css";

function App() {
  const [homes, setHomes] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [selectedHome, setSelectedHome] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState("");

  const [isDeviceFormOpen, setIsDeviceFormOpen] = useState(false);
  const [newDevice, setNewDevice] = useState({
    name: "",
    powerWatts: "",
    status: "NORMAL",
  });

  const loadDashboard = useCallback(async () => {
    try {
      setError("");
      setIsRefreshing(true);

      const [homesData, notificationsData] = await Promise.all([
        getHomesStatus(),
        getNotifications(),
      ]);

      setHomes(homesData);
      setNotifications(notificationsData);
      setLastUpdated(new Date());
    } catch {
      setError("Dashboard data could not be loaded.");
    } finally {
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const totalPower = useMemo(() => {
    return homes.reduce(
      (total, homeItem) => total + homeItem.totalPowerWatts,
      0
    );
  }, [homes]);

  const warningCount = useMemo(() => {
    return homes.filter((homeItem) => homeItem.status !== "NORMAL").length;
  }, [homes]);

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

  const handleAddDevice = (event) => {
    event.preventDefault();

    const deviceName = newDevice.name.trim();
    const devicePower = Number(newDevice.powerWatts);

    if (!selectedHome || !deviceName || devicePower <= 0) {
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
      devices: [...selectedHome.devices, deviceToAdd],
      totalPowerWatts: selectedHome.totalPowerWatts + devicePower,
    };

    setHomes((currentHomes) =>
      currentHomes.map((homeItem) =>
        homeItem.id === selectedHome.id ? updatedSelectedHome : homeItem
      )
    );

    setSelectedHome(updatedSelectedHome);
    setNewDevice({
      name: "",
      powerWatts: "",
      status: "NORMAL",
    });
    setIsDeviceFormOpen(false);
  };

  return (
    <main className="app">
      <header className="header">
        <h1 className="brand-title">EnergyPulse</h1>

        <div className="monitor-panel">
          <button
            type="button"
            className="refresh-button"
            onClick={loadDashboard}
            disabled={isRefreshing}
          >
            <RefreshCw
              size={17}
              className={isRefreshing ? "spinning" : ""}
            />
            {isRefreshing ? "Refreshing..." : "Refresh"}
          </button>

          <span>
            Last updated:{" "}
            {lastUpdated
              ? lastUpdated.toLocaleTimeString("en-GB")
              : "Not yet"}
          </span>
        </div>
      </header>

      {error && <div className="error-box">{error}</div>}

      <section className="summary-grid">
        <div className="summary-box">
          <Home />
          <span>{homes.length}</span>
          <p>Homes</p>
        </div>

        <div className="summary-box">
          <Zap />
          <span>{totalPower} W</span>
          <p>Total power</p>
        </div>

        <div className="summary-box warning">
          <AlertTriangle />
          <span>{warningCount}</span>
          <p>Warnings</p>
        </div>
      </section>

      <section className="notification-panel">
        <div className="section-title">
          <Bell size={20} />
          <h2>Notifications</h2>
        </div>

        {notifications.length === 0 ? (
          <p className="muted">No active notifications.</p>
        ) : (
          <div className="notification-list">
            {notifications.map((notification) => (
              <article
                className="notification-card"
                key={notification.id}
              >
                <div>
                  <span className="notification-home">
                    {notification.homeName}
                  </span>

                  <h3>{notification.title}</h3>
                  <p>{notification.message}</p>
                </div>

                <div className="email-status">
                  <Mail size={16} />
                  {notification.emailStatus}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="home-grid">
        {homes.map((homeItem) => {
          const budgetRate = Math.min(
            Math.round(
              (homeItem.currentCost / homeItem.monthlyBudget) * 100
            ),
            100
          );

          return (
            <article
              className="home-card"
              key={homeItem.id}
              onClick={() => openHomeDetails(homeItem)}
            >
              <div className="card-top">
                <h2>{homeItem.name}</h2>

                <span
                  className={`badge ${homeItem.status.toLowerCase()}`}
                >
                  {homeItem.status}
                </span>
              </div>

              <p className="power">{homeItem.totalPowerWatts} W</p>
              <p className="muted">{homeItem.dailyKwh} kWh today</p>

              <div className="budget-row">
                <span>Budget usage</span>
                <strong>{budgetRate}%</strong>
              </div>

              <div className="progress">
                <div style={{ width: `${budgetRate}%` }} />
              </div>
            </article>
          );
        })}
      </section>

      {selectedHome && (
        <div className="modal-backdrop" onClick={closeHomeDetails}>
          <section
            className="modal"
            onClick={(event) => event.stopPropagation()}
          >
            <button
              type="button"
              className="close-button"
              onClick={closeHomeDetails}
              aria-label="Close"
            >
              ×
            </button>

            <h2>{selectedHome.name} Details</h2>
            <p className="muted">
              Weekly consumption and registered devices
            </p>

            <div className="chart-box">
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={selectedHome.history}>
                  <XAxis dataKey="day" />
                  <YAxis />
                  <Tooltip />
                  <Bar
                    dataKey="kwh"
                    fill="#2563eb"
                    radius={[6, 6, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="device-table-header">
              <h3>Devices</h3>

              <button
                type="button"
                className="add-device-button"
                onClick={() =>
                  setIsDeviceFormOpen((currentValue) => !currentValue)
                }
              >
                {isDeviceFormOpen ? "Cancel" : "+ Add Device"}
              </button>
            </div>

            {isDeviceFormOpen && (
              <form className="device-form" onSubmit={handleAddDevice}>
                <input
                  type="text"
                  placeholder="Device name"
                  value={newDevice.name}
                  onChange={(event) =>
                    setNewDevice({
                      ...newDevice,
                      name: event.target.value,
                    })
                  }
                  required
                />

                <input
                  type="number"
                  placeholder="Power W"
                  min="1"
                  value={newDevice.powerWatts}
                  onChange={(event) =>
                    setNewDevice({
                      ...newDevice,
                      powerWatts: event.target.value,
                    })
                  }
                  required
                />

                <select
                  value={newDevice.status}
                  onChange={(event) =>
                    setNewDevice({
                      ...newDevice,
                      status: event.target.value,
                    })
                  }
                >
                  <option value="NORMAL">NORMAL</option>
                  <option value="ANOMALY">ANOMALY</option>
                </select>

                <button type="submit">Save</button>
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
                  {selectedHome.devices.map((device) => (
                    <tr key={device.id}>
                      <td>{device.name}</td>
                      <td>{device.powerWatts} W</td>
                      <td>
                        <span
                          className={`table-status ${device.status.toLowerCase()}`}
                        >
                          {device.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}
    </main>
  );
}

export default App;