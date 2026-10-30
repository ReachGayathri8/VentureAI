import React from 'react';
import RatingStars from './RatingStars';

export default function RatingRow({ label, values }) {
  return (
    <tr className="border-b border-slate-50 last:border-0">
      <td className="py-2.5 pr-4 text-sm font-medium text-slate-600">{label}</td>
      {values.map((v, idx) => (
        <td key={idx} className="py-2.5 pr-4">
          <RatingStars value={v} />
        </td>
      ))}
    </tr>
  );
}
