import React from 'react';
import { NavLink } from 'react-router-dom';

const TABS = [
  { path: '', label: 'Overview' },
  { path: '/pnl', label: 'P&L' },
  { path: '/cash-flow', label: 'Cash Flow' },
  { path: '/balance-sheet', label: 'Balance Sheet' },
  { path: '/break-even', label: 'Break-even' },
  { path: '/revenue-projection', label: 'Revenue Projection' },
];

export default function FinancialTabs({ startupId }) {
  return (
    <div className="mb-6 flex flex-wrap gap-2 border-b border-slate-200 pb-3">
      {TABS.map((tab) => (
        <NavLink
          key={tab.label}
          to={`/financials/${startupId}${tab.path}`}
          end={tab.path === ''}
          className={({ isActive }) =>
            `rounded-lg px-3.5 py-2 text-sm font-semibold transition-colors ${
              isActive ? 'bg-navy-900 text-white' : 'text-slate-500 hover:bg-slate-100'
            }`
          }
        >
          {tab.label}
        </NavLink>
      ))}
    </div>
  );
}
