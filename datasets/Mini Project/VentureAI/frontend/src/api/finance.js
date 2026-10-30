import apiClient from './client';

export const financeApi = {
  get: (startupId) => apiClient.get(`/finance/${startupId}`).then((r) => r.data),
  update: (startupId, payload) => apiClient.post(`/finance/${startupId}`, payload).then((r) => r.data),
  recalculate: (startupId, assumptions) =>
    apiClient.post(`/finance/${startupId}/recalculate`, assumptions).then((r) => r.data),
};
