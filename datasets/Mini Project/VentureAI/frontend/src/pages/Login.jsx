import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../auth/AuthContext';
import { extractErrorMessage } from '../api/client';

export default function Login() {
  const { login, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await login(form);
      toast.success('Welcome back!');
      navigate(location.state?.from?.pathname || '/dashboard');
    } catch (err) {
      setError(extractErrorMessage(err, 'Invalid email or password.'));
    }
  };

  return (
    <AuthShell title="Welcome back" subtitle="Log in to continue validating your startup ideas.">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label-field">Email</label>
          <input name="email" type="email" required className="input-field" value={form.email} onChange={handleChange} placeholder="you@example.com" />
        </div>
        <div>
          <label className="label-field">Password</label>
          <input name="password" type="password" required className="input-field" value={form.password} onChange={handleChange} placeholder="••••••••" />
        </div>
        {error && <p className="text-sm font-medium text-brand-red">{error}</p>}
        <div className="flex items-center justify-between text-sm">
          <span />
          <Link to="/forgot-password" className="font-semibold text-navy-700 hover:text-brand-red">Forgot password?</Link>
        </div>
        <button type="submit" disabled={loading} className="btn-primary w-full py-3">
          {loading ? 'Logging in...' : 'Log In'}
        </button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        Don&apos;t have an account? <Link to="/register" className="font-semibold text-brand-red">Create one</Link>
      </p>
    </AuthShell>
  );
}

export function AuthShell({ title, subtitle, children }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-navy-950 px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 flex items-center justify-center gap-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-red text-lg font-black text-white">V</div>
          <span className="text-xl font-extrabold text-white">VentureAI</span>
        </div>
        <div className="rounded-2xl bg-white p-8 shadow-2xl">
          <h1 className="text-2xl font-extrabold text-navy-900">{title}</h1>
          {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
          <div className="mt-6">{children}</div>
        </div>
      </div>
    </div>
  );
}
