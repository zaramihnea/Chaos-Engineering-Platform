import { Routes, Route, useLocation } from "react-router-dom";

import HomeDashboard from "./components/HomeDashboard";
import WorkflowList from "./components/WorkflowList";
import ExperimentsPage from "./components/ExperimentsPage";
import WorkflowDetail from "./components/WorkflowDetail";
import Login from "./components/Login";
import CreateExperiment from "./components/CreateExperiment";
import ExperimentDetail from "./components/ExperimentDetail";
import MainLayout from "./layouts/MainLayout";


export default function App() {
  const location = useLocation();

  const noLayoutRoutes = ["/login"];

  const isNoLayout = noLayoutRoutes.includes(location.pathname);

  return (
    <>
      {isNoLayout ? (
        <Routes>
          <Route path="/login" element={<Login />} />
        </Routes>
      ) : (
        <MainLayout>
          <Routes>
            <Route path="/" element={<HomeDashboard />} />
            <Route path="/experiments" element={<ExperimentsPage />} />
            {/* <Route path="/workflows" element={<WorkflowList />} /> */}
            <Route path="/new-experiment" element={<CreateExperiment />} />
            <Route path="/experiment/:id" element={<ExperimentDetail />} />
            {/* <Route path="/workflow/:id" element={<WorkflowDetail />} /> */}
          </Routes>
        </MainLayout>
      )}
    </>
  );
}