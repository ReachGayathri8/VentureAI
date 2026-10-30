import React from 'react';
import { useAuth } from '../auth/AuthContext';
import PageHeader from '../components/PageHeader';

export default function ProfilePage() {
  const { user } = useAuth();

  return (
    <div>
      <PageHeader title="Profile" subtitle="Your VentureAI account details." />
      <div className="card max-w-xl">
        <div className="mb-6 flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-navy-800 text-2xl font-bold text-white">
            {user?.fullName?.charAt(0)?.toUpperCase() || 'U'}
          </div>
          <div>
            <h3 className="text-lg font-bold text-navy-900">{user?.fullName}</h3>
            <p className="text-sm text-slate-500">{user?.userType?.replace('_', ' ')}</p>
          </div>
        </div>
        <dl className="space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <dt className="text-sm text-slate-500">Email</dt>
            <dd className="text-sm font-semibold text-navy-900">{user?.email}</dd>
          </div>
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <dt className="text-sm text-slate-500">Account Type</dt>
            <dd className="text-sm font-semibold text-navy-900">{user?.userType?.replace('_', ' ')}</dd>
          </div>
          <div className="flex items-center justify-between pb-1">
            <dt className="text-sm text-slate-500">User ID</dt>
            <dd className="text-sm font-semibold text-navy-900">#{user?.id}</dd>
          </div>
        </dl>
      </div>
    </div>
  );
}
