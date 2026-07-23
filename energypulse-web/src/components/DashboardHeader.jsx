import { RefreshCw } from "lucide-react";
import { useNavigate } from "react-router-dom";

function DashboardHeader({ isRefreshing, lastUpdated, onRefresh }) {
  const navigate = useNavigate();
  const userName =
    localStorage.getItem("energyPulseUserName") || "EnergyPulse User";

  const handleLogout = () => {
    localStorage.removeItem("energyPulseLoggedIn");
    localStorage.removeItem("energyPulseUserName");
    navigate("/login", { replace: true });
  };

  return (
    <header className="topbar">
      <div className="dashboard-title-area">
        <h1 className="dashboard-logo">EnergyPulse</h1>
        <span className="welcome-user">Welcome, {userName}</span>
      </div>

      <div className="dashboard-header-right">
        <div className="dashboard-header-actions">
          <button
            className="refresh-button"
            type="button"
            onClick={onRefresh}
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
  );
}

export default DashboardHeader;
