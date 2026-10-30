import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './auth/ProtectedRoute';
import DashboardLayout from './layouts/DashboardLayout';

import LandingPage from './pages/LandingPage';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

import Dashboard from './pages/Dashboard';
import MyStartups from './pages/MyStartups';
import StartupDetails from './pages/StartupDetails';
import NewValidation from './pages/NewValidation';
import ValidationProcessing from './pages/ValidationProcessing';
import ValidationResult from './pages/ValidationResult';
import SwotAnalysisPage from './pages/SwotAnalysisPage';
import CompetitorAnalysisPage from './pages/CompetitorAnalysisPage';
import MarketIntelligencePage from './pages/MarketIntelligencePage';
import FinancialDashboardPage from './pages/FinancialDashboardPage';
import PnLPage from './pages/PnLPage';
import CashFlowPage from './pages/CashFlowPage';
import BalanceSheetPage from './pages/BalanceSheetPage';
import BreakEvenPage from './pages/BreakEvenPage';
import RevenueProjectionPage from './pages/RevenueProjectionPage';
import RecommendationsPage from './pages/RecommendationsPage';
import RiskAnalysisPage from './pages/RiskAnalysisPage';
import ValidationHistoryPage from './pages/ValidationHistoryPage';
import CompareStartupsPage from './pages/CompareStartupsPage';
import ReportsPage from './pages/ReportsPage';
import ProfilePage from './pages/ProfilePage';
import SettingsPage from './pages/SettingsPage';
import NotFound from './pages/NotFound';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/startups" element={<MyStartups />} />
          <Route path="/startups/:id" element={<StartupDetails />} />
          <Route path="/validate/new" element={<NewValidation />} />
          <Route path="/validate/:startupId/processing" element={<ValidationProcessing />} />
          <Route path="/validations/:id/result" element={<ValidationResult />} />
          <Route path="/validations/:id/swot" element={<SwotAnalysisPage />} />
          <Route path="/validations/:id/competitors" element={<CompetitorAnalysisPage />} />
          <Route path="/validations/:id/market-intelligence" element={<MarketIntelligencePage />} />
          <Route path="/validations/:id/recommendations" element={<RecommendationsPage />} />
          <Route path="/validations/:id/risk" element={<RiskAnalysisPage />} />
          <Route path="/market-intelligence" element={<MarketIntelligencePage />} />
          <Route path="/competitors" element={<CompetitorAnalysisPage />} />
          <Route path="/financials" element={<FinancialDashboardPage />} />
          <Route path="/financials/:startupId" element={<FinancialDashboardPage />} />
          <Route path="/financials/:startupId/pnl" element={<PnLPage />} />
          <Route path="/financials/:startupId/cash-flow" element={<CashFlowPage />} />
          <Route path="/financials/:startupId/balance-sheet" element={<BalanceSheetPage />} />
          <Route path="/financials/:startupId/break-even" element={<BreakEvenPage />} />
          <Route path="/financials/:startupId/revenue-projection" element={<RevenueProjectionPage />} />
          <Route path="/history" element={<ValidationHistoryPage />} />
          <Route path="/compare" element={<CompareStartupsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
