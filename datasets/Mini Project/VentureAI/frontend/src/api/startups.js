import apiClient from './client';

export const startupsApi = {
  create: (payload) => apiClient.post('/startups', payload).then((r) => r.data),
  list: () => apiClient.get('/startups').then((r) => r.data),
  get: (id) => apiClient.get(`/startups/${id}`).then((r) => r.data),
  update: (id, payload) => apiClient.put(`/startups/${id}`, payload).then((r) => r.data),
  remove: (id) => apiClient.delete(`/startups/${id}`).then((r) => r.data),
};

export const competitorsApi = {
  list: (startupId) => apiClient.get(`/startups/${startupId}/competitors`).then((r) => r.data),
  add: (startupId, payload) => apiClient.post(`/startups/${startupId}/competitors`, payload).then((r) => r.data),
  discover: (startupId) => apiClient.post(`/startups/${startupId}/competitors/discover`).then((r) => r.data),
  remove: (startupId, competitorId) =>
    apiClient.delete(`/startups/${startupId}/competitors/${competitorId}`).then((r) => r.data),
};
