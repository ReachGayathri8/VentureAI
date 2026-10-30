import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../auth/AuthContext';
import { extractErrorMessage } from '../api/client';
import { USER_TYPES } from '../utils/constants';
import { AuthShell } from './Login';

export default function Register() {
  const { register, loading } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: '', email: '', password: '', confirmPassword: '', userType: 'ENTREPRENEUR',
  });
  const [error, setError] = useState('');

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.password !== form.confirmPassword) {
      setError('Password and confirm password do not match.');
      return;
    }
    try {
      await register(form);
      toast.success('Account created! Welcome to VentureAI.');
      navigate('/dashboard');
    } catch (err) {
      setError(extractErrorMessage(err, 'Unable to create account.'));
    }
  };

  return (
    <AuthShell title="Create your account" subtitle="Start validating startup ideas with AI in minutes.">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label-field">Full Name</label>
          <input name="fullName" required className="input-field" value={form.fullName} onChange={handleChange} placeholder="Jane Doe" />
        </div>
        <div>
          <label className="label-field">Email</label>
          <input name="email" type="email" required className="input-field" value={form.email} onChange={handleChange} placeholder="you@example.com" />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label-field">Password</label>
            <input name="password" type="password" required minLength={6} className="input-field" value={form.password} onChange={handleChange} placeholder="••••••••" />
          </div>
          <div>
            <label className="label-field">Confirm Password</label>
            <input name="confirmPassword" type="password" required minLength={6} className="input-field" value={form.confirmPassword} onChange={handleChange} placeholder="••••••••" />
          </div>
        </div>
        <div>
          <label className="label-field">I am a...</label>
          <select name="userType" className="input-field" value={form.userType} onChange={handleChange}>
            {USER_TYPES.map((t) => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
        </div>
        {error && <p className="text-sm font-medium text-brand-red">{error}</p>}
        <button type="submit" disabled={loading} className="btn-primary w-full py-3">
          {loading ? 'Creating account...' : 'Create Account'}
        </button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        Already have an account? <Link to="/login" className="font-semibold text-brand-red">Log in</Link>
      </p>
    </AuthShell>
  );
}
