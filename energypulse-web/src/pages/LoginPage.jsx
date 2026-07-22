import { useState } from "react";
import { Lock, Mail, Zap } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import "./AuthPage.css";

function LoginPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const [error, setError] = useState("");

  const handleChange = (event) => {
    setFormData({
      ...formData,
      [event.target.name]: event.target.value,
    });

    setError("");
  };

  const handleSubmit = (event) => {
  event.preventDefault();

  if (!formData.email || !formData.password) {
    setError("Please complete all fields.");
    return;
  }

  const savedUser = JSON.parse(
    localStorage.getItem("energyPulseRegisteredUser")
  );

  if (!savedUser) {
    setError("No registered account was found.");
    return;
  }

  if (
    formData.email !== savedUser.email ||
    formData.password !== savedUser.password
  ) {
    setError("Email address or password is incorrect.");
    return;
  }

  localStorage.setItem("energyPulseLoggedIn", "true");
  localStorage.setItem("energyPulseUserName", savedUser.fullName);

  navigate("/dashboard");
};

  return (
    <main className="auth-page">
      <section className="auth-brand-panel">
        <Link className="auth-logo" to="/">
          <span className="auth-logo-icon">
            <Zap size={22} />
          </span>
          EnergyPulse
        </Link>

        <div className="auth-brand-content">
          <h2>Your energy data, always within reach.</h2>
          <p>
            Sign in to monitor your homes, inspect device consumption and follow
            anomaly alerts from one dashboard.
          </p>
        </div>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-container">
          <h1>Welcome back</h1>
          <p className="auth-description">
            Enter your account details to open your dashboard.
          </p>

          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="auth-field">
              <label htmlFor="login-email">Email address</label>

              <div className="auth-input-wrapper">
                <Mail size={19} />
                <input
                  id="login-email"
                  name="email"
                  type="email"
                  placeholder="name@example.com"
                  value={formData.email}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="login-password">Password</label>

              <div className="auth-input-wrapper">
                <Lock size={19} />
                <input
                  id="login-password"
                  name="password"
                  type="password"
                  placeholder="Enter your password"
                  value={formData.password}
                  onChange={handleChange}
                />
              </div>
            </div>

            {error && <p className="auth-error">{error}</p>}

            <button className="auth-submit-button" type="submit">
              Sign In
            </button>
          </form>

          <p className="auth-switch-text">
            Don&apos;t have an account?{" "}
            <Link to="/register">Create an account</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

export default LoginPage;