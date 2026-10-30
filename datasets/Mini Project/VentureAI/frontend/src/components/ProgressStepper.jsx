import React from 'react';
import { Check } from 'lucide-react';

export default function ProgressStepper({ steps, current }) {
  return (
    <div className="mb-8 flex items-center">
      {steps.map((step, idx) => {
        const isDone = idx < current;
        const isActive = idx === current;
        return (
          <React.Fragment key={step}>
            <div className="flex flex-col items-center">
              <div
                className={`flex h-9 w-9 items-center justify-center rounded-full text-sm font-bold ${
                  isDone ? 'bg-emerald-500 text-white' : isActive ? 'bg-navy-900 text-white' : 'bg-slate-200 text-slate-500'
                }`}
              >
                {isDone ? <Check size={16} /> : idx + 1}
              </div>
              <span className={`mt-1.5 hidden text-xs font-medium sm:block ${isActive ? 'text-navy-900' : 'text-slate-400'}`}>
                {step}
              </span>
            </div>
            {idx < steps.length - 1 && (
              <div className={`mx-2 h-0.5 flex-1 ${idx < current ? 'bg-emerald-500' : 'bg-slate-200'}`} />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}
