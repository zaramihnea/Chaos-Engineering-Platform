import { Routes, Route, useLocation } from "react-router-dom";

import HomeDashboard from "./components/HomeDashboard";
import WorkflowList from "./components/WorkflowList";
import ExperimentsPage from "./components/ExperimentsPage";
import WorkflowDetail from "./components/WorkflowDetail";
import Login from "./components/Login";
import CreateExperiment from "./components/CreateExperiment";
import MainLayout from "./layouts/MainLayout";


export default function App() {
  const location = useLocation();

  // Pages that SHOULD NOT use MainLayout
  const noLayoutRoutes = ["/login"];

  const isNoLayout = noLayoutRoutes.includes(location.pathname);

  return (
    <>
      {isNoLayout ? (
        /* Render pages WITHOUT MainLayout */
        <Routes>
          <Route path="/login" element={<Login />} />
        </Routes>
      ) : (
        /* Render pages WITH MainLayout */
        <MainLayout>
          <Routes>
            <Route path="/" element={<HomeDashboard />} />
            <Route path="/experiments" element={<ExperimentsPage />} />
            <Route path="/workflows" element={<WorkflowList />} />
            <Route path="/workflow/:id" element={<WorkflowDetail />} />
            <Route path="/new-experiment" element={<CreateExperiment />} />
          </Routes>
        </MainLayout>
      )}
    </>
  );
}