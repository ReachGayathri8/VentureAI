import React from 'react';
import { classificationColor, riskColor } from '../utils/format';

export function ClassificationBadge({ classification }) {
  const c = classificationColor(classification);
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${c.bg} ${c.text}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${c.dot}`} />
      {classification}
    </span>
  );
}

export function RiskBadge({ level }) {
  const c = riskColor(level);
  return (
    <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${c.bg} ${c.text}`}>
      {level} Risk
    </span>
  );
}
