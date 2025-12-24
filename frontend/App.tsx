import React from 'react';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import DataCollection from './pages/DataCollection';
import Home from './pages/Home';
import StockList from './pages/StockList';
import UnifiedChartPage from './pages/UnifiedChartPage';

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/data-collection" element={<DataCollection />} />
        <Route path="/stock-list" element={<StockList />} />
        {/* Unified Chart Page for both Moving Average & Supply/Demand */}
        <Route path="/moving-chart" element={<UnifiedChartPage />} />
      </Routes>
    </Router>
  );
};

export default App;