import React, { useState } from "react";
import { FcGoogle } from "react-icons/fc";
import { FaGithub } from "react-icons/fa";
import useFormMonitor from "../mop/useFormMonitor";
import "../styles/Login.css";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  useFormMonitor(
  {
    email,
    password
  },
  {
    email: {
      check: (v) => v.trim().length > 0,
      message: "Email is required",
      onViolation: () => console.log("Login blocked until email is valid"),
      onValidation: () => console.log("Email looks good")
    },
    password: {
      check: (v) => v.trim().length > 0,
      message: "Password is required",
      onViolation: () => console.log("Login blocked until password is valid"),
    }
  }
);
  return (
    <div className="login-page">

      <div className="login-card">
        <div className="login-left">
          <h1 className="left-title">
            Get started with <span>Chaos Platform</span>
          </h1>

          <p className="left-subtitle">
            Break things safely. Improve reliability.
          </p>

          <div className="left-graphic">🚀</div>
        </div>

        <div className="login-right">
          <h2 className="form-title">Sign In</h2>

          <label>Email</label>
          <input
            type="email"
            placeholder="Enter email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <label>Password</label>
          <input
            type="password"
            placeholder="Enter password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <button className="btn-login">Login</button>

          <div className="divider">
            <span>or</span>
          </div>

          <button className="btn-oauth google">
            <FcGoogle size={22} style={{ marginRight: "10px" }} />
            Sign in with Google
          </button>
          <button className="btn-oauth github">
            <FaGithub size={22} style={{ marginRight: "10px" }} />
            Sign in with GitHub
          </button>
        </div>
      </div>
    </div>
  );
}
