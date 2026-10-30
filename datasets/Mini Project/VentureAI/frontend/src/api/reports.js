import apiClient from './client';

export const reportsApi = {
  downloadUrl: (validationId) => `${apiClient.defaults.baseURL}/reports/${validationId}/download`,
  download: async (validationId) => {
    const response = await apiClient.get(`/reports/${validationId}/download`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `VentureAI-Validation-Report-${validationId}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};
