import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { financeApi } from '../api/finance';

export default function useFinanceData(startupId) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = () => {
    if (!startupId) return;
    setLoading(true);
    financeApi
      .get(startupId)
      .then(setData)
      .catch((err) => {
        const message = err?.response?.data?.message || 'Financial inputs are incomplete for this startup.';
        setError(message);
        toast.error(message);
      })
      .finally(() => setLoading(false));
  };

  useEffect(reload, [startupId]);

  return { data, loading, error, reload };
}
