import { Activity, AlertTriangle, ArrowRight, House, Zap } from "lucide-react";
import { Link } from "react-router-dom";
import "./HomePage.css";

function HomePage() {
  return (
    <main className="landing-page">
      <nav className="landing-navbar">
        <Link className="landing-logo" to="/">
          <span className="logo-icon">
            <Zap size={22} />
          </span>
          EnergyPulse
        </Link>

        <div className="landing-nav-actions">
          <Link className="login-link" to="/login">
            Login
          </Link>

          <Link className="nav-register-button" to="/register">
            Get Started
          </Link>
        </div>
      </nav>

      <section className="hero-section">
        <div className="hero-content">
          <span className="hero-badge">
            <Activity size={16} />
            Smart Energy Monitoring
          </span>

          <h1>
            Understand and control your
            <span> home energy usage.</span>
          </h1>

          <p>
            Track electricity consumption, detect unusual activity and manage
            every device in your home from one simple dashboard.
          </p>

          <div className="hero-buttons">
            <Link className="primary-hero-button" to="/register">
              Start Monitoring
              <ArrowRight size={18} />
            </Link>

            <Link className="secondary-hero-button" to="/login">
              Open Dashboard
            </Link>
          </div>
        </div>

        <div className="hero-preview">
          <div className="preview-header">
            <div>
              <span>Live consumption</span>
              <strong>2,840 W</strong>
            </div>

            <span className="live-status">LIVE</span>
          </div>

          <div className="energy-graph">
            <div style={{ height: "35%" }} />
            <div style={{ height: "54%" }} />
            <div style={{ height: "44%" }} />
            <div style={{ height: "72%" }} />
            <div style={{ height: "61%" }} />
            <div style={{ height: "88%" }} />
            <div style={{ height: "68%" }} />
          </div>

          <div className="preview-device">
            <div className="device-icon">
              <House size={20} />
            </div>

            <div>
              <strong>Home A</strong>
              <span>5 active devices</span>
            </div>

            <span className="normal-label">NORMAL</span>
          </div>
        </div>
      </section>

      <section className="features-section">
        <div className="section-heading">
          <span>ENERGYPULSE FEATURES</span>
          <h2>Everything you need to monitor your home</h2>
        </div>

        <div className="feature-grid">
          <article className="feature-card">
            <div className="feature-icon blue">
              <Activity size={25} />
            </div>

            <h3>Live Monitoring</h3>
            <p>
              Follow the current electricity usage of your homes and devices
              in real time.
            </p>
          </article>

          <article className="feature-card">
            <div className="feature-icon orange">
              <AlertTriangle size={25} />
            </div>

            <h3>Anomaly Insights</h3>
            <p>
              Review unusual energy consumption patterns and the devices that
              require attention.
            </p>
          </article>

          <article className="feature-card">
            <div className="feature-icon green">
              <Zap size={25} />
            </div>

            <h3>Device Management</h3>
            <p>
              View registered devices, compare their consumption and manage
              them from one place.
            </p>
          </article>
        </div>
      </section>

      <footer className="landing-footer">
        <Link className="landing-logo footer-logo" to="/">
          <Zap size={20} />
          EnergyPulse
        </Link>

        <p>Smart energy monitoring for modern homes.</p>
      </footer>
    </main>
  );
}

export default HomePage;
