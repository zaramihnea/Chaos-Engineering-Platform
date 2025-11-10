import { Routes, Route } from "react-router-dom";

import HomeDashboard from "./components/HomeDashboard";
import WorkflowList from "./components/WorkflowList";
import ExperimentsPage from "./components/ExperimentsPage";
import WorkflowDetail from "./components/WorkflowDetail";


export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeDashboard />} />
      <Route path="/experiments" element={<ExperimentsPage />}/>
      <Route path="/workflows" element={<WorkflowList />}/>
      <Route path="/workflow/:id" element={<WorkflowDetail />} />

    </Routes>
  );
}
