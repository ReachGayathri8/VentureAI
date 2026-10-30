import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { LogOut } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import PageHeader from '../components/PageHeader';

export default function SettingsPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState(true);
  const [emailReports, setEmailReports] = useState(false);

  return (
    <div>
      <PageHeader title="Settings" subtitle="Manage your VentureAI preferences." />
      <div className="card max-w-xl space-y-6">
        <ToggleRow
          label="In-app notifications"
          description="Get notified when a validation finishes processing."
          checked={notifications}
          onChange={setNotifications}
        />
        <ToggleRow
          label="Email me validation reports"
          description="Requires an SMTP provider to be configured on the backend."
          checked={emailReports}
          onChange={setEmailReports}
        />
        <div className="border-t border-slate-100 pt-6">
          <button
            onClick={() => { logout(); navigate('/login'); toast.success('Logged out.'); }}
            className="btn-outline w-full text-brand-red hover:bg-red-50"
          >
            <LogOut size={16} /> Log Out
          </button>
        </div>
      </div>
    </div>
  );
}

function ToggleRow({ label, description, checked, onChange }) {
  return (
    <div className="flex items-center justify-between">
      <div>
        <p className="text-sm font-semibold text-navy-900">{label}</p>
        <p className="text-xs text-slate-400">{description}</p>
      </div>
      <button
        onClick={() => onChange(!checked)}
        className={`h-6 w-11 flex-shrink-0 rounded-full transition-colors ${checked ? 'bg-navy-800' : 'bg-slate-200'}`}
      >
        <span className={`block h-5 w-5 translate-y-0.5 rounded-full bg-white transition-transform ${checked ? 'translate-x-5' : 'translate-x-0.5'}`} />
      </button>
    </div>
  );
}
