export function formatCurrency(value) {
  const num = Number(value ?? 0);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(num);
}

export function formatNumber(value) {
  return new Intl.NumberFormat('en-IN').format(Number(value ?? 0));
}

export function formatPercent(value, digits = 1) {
  return `${Number(value ?? 0).toFixed(digits)}%`;
}

export function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
}

export function classificationColor(classification = '') {
  const key = classification.toUpperCase().replace(/\s+/g, '_');
  switch (key) {
    case 'HIGH_POTENTIAL':
      return { bg: 'bg-emerald-50', text: 'text-emerald-700', dot: 'bg-emerald-500', ring: 'ring-emerald-200' };
    case 'MODERATE_POTENTIAL':
      return { bg: 'bg-amber-50', text: 'text-amber-700', dot: 'bg-amber-500', ring: 'ring-amber-200' };
    case 'NEEDS_IMPROVEMENT':
      return { bg: 'bg-orange-50', text: 'text-orange-700', dot: 'bg-orange-500', ring: 'ring-orange-200' };
    default:
      return { bg: 'bg-red-50', text: 'text-red-700', dot: 'bg-red-500', ring: 'ring-red-200' };
  }
}

export function riskColor(level = '') {
  switch (level.toUpperCase()) {
    case 'LOW':
      return { bg: 'bg-emerald-50', text: 'text-emerald-700' };
    case 'MEDIUM':
      return { bg: 'bg-amber-50', text: 'text-amber-700' };
    default:
      return { bg: 'bg-red-50', text: 'text-red-700' };
  }
}
