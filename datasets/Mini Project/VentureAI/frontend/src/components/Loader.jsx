import React from 'react';
import { Loader2 } from 'lucide-react';

export default function Loader({ label = 'Loading...' }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-24 text-slate-500">
      <Loader2 className="animate-spin text-navy-600" size={32} />
      <p className="text-sm font-medium">{label}</p>
    </div>
  );
}
