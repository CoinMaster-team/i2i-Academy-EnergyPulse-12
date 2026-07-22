import { useState } from "react";
import { Lock, Mail, User, Zap } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import "./AuthPage.css";

function RegisterPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
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

    if (
      !formData.fullName ||
      !formData.email ||
      !formData.password ||
      !formData.confirmPassword
    ) {
      setError("Please complete all fields.");
      return;
    }

    if (formData.password.length < 6) {
      setError("Password must contain at least 6 characters.");
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    const registeredUser = {
    fullName: formData.fullName,
    email: formData.email,
    password: formData.password,
    };

    localStorage.setItem(
    "energyPulseRegisteredUser",
    JSON.stringify(registeredUser)
    );

    localStorage.setItem("energyPulseLoggedIn", "true");
    localStorage.setItem("energyPulseUserName", formData.fullName);

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
          <h2>Start monitoring your energy smarter.</h2>

          <p>
            Create your account to track electricity consumption, manage your
            devices and review anomaly indicators.
          </p>
        </div>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-container">
          <h1>Create account</h1>

          <p className="auth-description">
            Enter your details to start using EnergyPulse.
          </p>

          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="auth-field">
              <label htmlFor="register-name">Full name</label>

              <div className="auth-input-wrapper">
                <User size={19} />

                <input
                  id="register-name"
                  name="fullName"
                  type="text"
                  placeholder="Enter your full name"
                  value={formData.fullName}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="register-email">Email address</label>

              <div className="auth-input-wrapper">
                <Mail size={19} />

                <input
                  id="register-email"
                  name="email"
                  type="email"
                  placeholder="name@example.com"
                  value={formData.email}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="register-password">Password</label>

              <div className="auth-input-wrapper">
                <Lock size={19} />

                <input
                  id="register-password"
                  name="password"
                  type="password"
                  placeholder="At least 6 characters"
                  value={formData.password}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="register-confirm-password">
                Confirm password
              </label>

              <div className="auth-input-wrapper">
                <Lock size={19} />

                <input
                  id="register-confirm-password"
                  name="confirmPassword"
                  type="password"
                  placeholder="Enter your password again"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                />
              </div>
            </div>

            {error && <p className="auth-error">{error}</p>}

            <button className="auth-submit-button" type="submit">
              Create Account
            </button>
          </form>

          <p className="auth-switch-text">
            Already have an account? <Link to="/login">Sign in</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

export default RegisterPage;
