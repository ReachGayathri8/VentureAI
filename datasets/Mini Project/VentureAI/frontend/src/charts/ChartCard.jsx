import React from 'react';
import { ResponsiveContainer } from 'recharts';

export default function ChartCard({ title, subtitle, height = 280, children, actions }) {
  return (
    <div className="card">
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h3 className="text-base font-bold text-navy-900">{title}</h3>
          {subtitle && <p className="text-xs text-slate-400">{subtitle}</p>}
        </div>
        {actions}
      </div>
      <ResponsiveContainer width="100%" height={height}>
        {children}
      </ResponsiveContainer>
    </div>
  );
}
