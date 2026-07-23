import { LayoutDashboard, MailCheck } from "lucide-react";
import { NavLink } from "react-router-dom";

function DashboardNavigation() {
  return (
    <nav className="dashboard-navigation" aria-label="Dashboard sections">
      <NavLink
        className={({ isActive }) =>
          `dashboard-navigation-link${isActive ? " active" : ""}`
        }
        to="/dashboard"
        end
      >
        <LayoutDashboard size={17} />
        Dashboard
      </NavLink>
      <NavLink
        className={({ isActive }) =>
          `dashboard-navigation-link${isActive ? " active" : ""}`
        }
        to="/notifications"
      >
        <MailCheck size={17} />
        Notification Console
      </NavLink>
    </nav>
  );
}

export default DashboardNavigation;
