import React from 'react';

export default function ScoreGauge({ score = 0, size = 200, label }) {
  const clamped = Math.max(0, Math.min(100, score));
  const radius = (size - 20) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (clamped / 100) * circumference;

  const color = clamped >= 80 ? '#059669' : clamped >= 60 ? '#d97706' : clamped >= 40 ? '#ea580c' : '#dc2626';

  return (
    <div className="relative flex flex-col items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={radius} stroke="#e2e8f0" strokeWidth={14} fill="none" />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={color}
          strokeWidth={14}
          fill="none"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          style={{ transition: 'stroke-dashoffset 1s ease-out' }}
        />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span className="text-4xl font-extrabold text-navy-900">{Math.round(clamped)}</span>
        <span className="text-xs font-medium text-slate-400">/ 100</span>
        {label && <span className="mt-1 text-sm font-semibold" style={{ color }}>{label}</span>}
      </div>
    </div>
  );
}
