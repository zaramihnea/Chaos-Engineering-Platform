import React from "react";
import { NavLink } from "react-router-dom";
import "../styles/SidebarMenu.css";

export default function SidebarMenu() {
  return (
    <div className="sidebar-container">
      <div className="sidebar-top">
        <div className="sidebar-title">
          <span className="platform-accent">Chaos</span> Engineering Platform
        </div>

        <NavLink
          to="/"
          className={({ isActive }) =>
            isActive ? "sidebar-item active" : "sidebar-item"
          }
        >
          🏠 Overview
        </NavLink>

        <NavLink
          to="/experiments"
          className={({ isActive }) =>
            isActive ? "sidebar-item active" : "sidebar-item"
          }
        >
          🧪 Experiments
        </NavLink>
      </div>

      <div className="sidebar-bottom">
        <NavLink
          to="/profile"
          className={({ isActive }) =>
            isActive ? "sidebar-item active bottom" : "sidebar-item bottom"
          }
        >
          👤 Profile
        </NavLink>

        <NavLink
          to="/settings"
          className={({ isActive }) =>
            isActive ? "sidebar-item active bottom" : "sidebar-item bottom"
          }
        >
          ⚙️ Settings
        </NavLink>

        <NavLink
          to="/logout"
          className={({ isActive }) =>
            isActive ? "sidebar-item active bottom" : "sidebar-item bottom"
          }
        >
          ⬅️ Logout
        </NavLink>
      </div>
    </div>
  );
}
