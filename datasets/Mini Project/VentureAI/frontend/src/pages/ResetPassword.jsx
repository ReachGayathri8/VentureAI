import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi } from '../api/auth';
import { extractErrorMessage } from '../api/client';
import { AuthShell } from './Login';

export default function ResetPassword() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ token: '', newPassword: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await authApi.resetPassword(form);
      toast.success('Password reset. Please log in.');
      navigate('/login');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title="Reset password" subtitle="Paste the reset token you received and choose a new password.">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label-field">Reset Token</label>
          <input name="token" required className="input-field" value={form.token} onChange={handleChange} placeholder="Paste your reset token" />
        </div>
        <div>
          <label className="label-field">New Password</label>
          <input name="newPassword" type="password" required minLength={6} className="input-field" value={form.newPassword} onChange={handleChange} placeholder="••••••••" />
        </div>
        {error && <p className="text-sm font-medium text-brand-red">{error}</p>}
        <button type="submit" disabled={loading} className="btn-primary w-full py-3">
          {loading ? 'Resetting...' : 'Reset Password'}
        </button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        <Link to="/login" className="font-semibold text-brand-red">Back to login</Link>
      </p>
    </AuthShell>
  );
}
