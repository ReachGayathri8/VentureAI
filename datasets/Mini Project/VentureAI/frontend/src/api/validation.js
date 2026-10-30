import apiClient from './client';

export const validationApi = {
  run: (startupId) => apiClient.post(`/validation/${startupId}`).then((r) => r.data),
  get: (id) => apiClient.get(`/validation/${id}`).then((r) => r.data),
  swot: (id) => apiClient.get(`/validation/${id}/swot`).then((r) => r.data),
  competitors: (id) => apiClient.get(`/validation/${id}/competitors`).then((r) => r.data),
  marketOpportunity: (id) => apiClient.get(`/validation/${id}/market-opportunity`).then((r) => r.data),
  recommendations: (id) => apiClient.get(`/validation/${id}/recommendations`).then((r) => r.data),
  risks: (id) => apiClient.get(`/validation/${id}/risks`).then((r) => r.data),
  verdict: (id) => apiClient.get(`/validation/${id}/verdict`).then((r) => r.data),
  history: () => apiClient.get('/validation-history').then((r) => r.data),
};
