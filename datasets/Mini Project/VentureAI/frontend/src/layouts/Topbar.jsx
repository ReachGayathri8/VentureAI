import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Menu, ChevronDown, LogOut, User as UserIcon, Settings as SettingsIcon } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';

export default function Topbar({ onMenuClick }) {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200 bg-white/90 px-4 backdrop-blur sm:px-6">
      <div className="flex items-center gap-3">
        <button className="text-slate-500 lg:hidden" onClick={onMenuClick}>
          <Menu size={22} />
        </button>
      </div>

      <div className="relative">
        <button onClick={() => setOpen((o) => !o)} className="flex items-center gap-2 rounded-xl px-2 py-1.5 hover:bg-slate-50">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-navy-800 text-sm font-bold text-white">
            {user?.fullName?.charAt(0)?.toUpperCase() || 'U'}
          </div>
          <div className="hidden text-left sm:block">
            <p className="text-sm font-semibold text-navy-900">{user?.fullName}</p>
            <p className="text-xs text-slate-400">{user?.userType?.replace('_', ' ')}</p>
          </div>
          <ChevronDown size={16} className="text-slate-400" />
        </button>
        {open && (
          <div className="absolute right-0 mt-2 w-48 rounded-xl border border-slate-200 bg-white py-2 shadow-cardHover">
            <button
              onClick={() => { setOpen(false); navigate('/profile'); }}
              className="flex w-full items-center gap-2 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
            >
              <UserIcon size={16} /> Profile
            </button>
            <button
              onClick={() => { setOpen(false); navigate('/settings'); }}
              className="flex w-full items-center gap-2 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
            >
              <SettingsIcon size={16} /> Settings
            </button>
            <hr className="my-1 border-slate-100" />
            <button
              onClick={() => { setOpen(false); logout(); navigate('/login'); }}
              className="flex w-full items-center gap-2 px-4 py-2 text-sm text-brand-red hover:bg-red-50"
            >
              <LogOut size={16} /> Logout
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
