import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';

import {
  LayoutDashboard,
  Sparkles,
  Rocket,
  History,
  Globe2,
  Swords,
  LineChart,
  FileText,
  User,
  Settings,
  X,
  CircleHelp,
  ArrowRight,
} from 'lucide-react';

import { SIDEBAR_LINKS } from '../utils/constants';


const ICONS = {
  LayoutDashboard,
  Sparkles,
  Rocket,
  History,
  Globe2,
  Swords,
  LineChart,
  FileText,
  User,
  Settings,
};


export default function Sidebar({
  open,
  onClose,
}) {

  const [
    helpOpen,
    setHelpOpen,
  ] = useState(false);

  const navigate =
    useNavigate();


  const closeHelp = () => {

    setHelpOpen(false);
  };


  const startValidation = () => {

    closeHelp();
    onClose?.();

    navigate(
      '/validate/new'
    );
  };


  return (

    <>

      {/* Mobile overlay */}

      {open && (

        <div
          className="fixed inset-0 z-30 bg-black/30 lg:hidden"
          onClick={onClose}
        />

      )}


      <aside
        className={`
          fixed inset-y-0 left-0 z-40 w-64
          transform bg-navy-900 text-white
          transition-transform duration-200
          lg:static lg:translate-x-0
          ${
            open
              ? 'translate-x-0'
              : '-translate-x-full'
          }
        `}
      >

        {/* ====================================================
            LOGO
        ==================================================== */}

        <div className="flex items-center justify-between px-5 py-5">

          <div className="flex items-center gap-2">

            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-red font-black">
              V
            </div>

            <div>

              <p className="text-sm font-extrabold leading-none">
                VentureAI
              </p>

              <p className="text-[10px] text-navy-300">
                Business Intelligence
              </p>

            </div>

          </div>


          <button
            type="button"
            className="text-navy-300 lg:hidden"
            onClick={onClose}
            aria-label="Close menu"
          >
            <X size={20} />
          </button>

        </div>


        {/* ====================================================
            NAVIGATION
        ==================================================== */}

        <nav className="mt-2 space-y-1 px-3">

          {SIDEBAR_LINKS.map(
            (link) => {

              const Icon =
                ICONS[
                  link.icon
                ];

              return (

                <NavLink
                  key={link.to}
                  to={link.to}
                  onClick={onClose}
                  className={({ isActive }) =>
                    `
                    flex items-center gap-3
                    rounded-xl px-3 py-2.5
                    text-sm font-medium
                    transition-colors
                    ${
                      isActive
                        ? 'bg-brand-red text-white'
                        : 'text-navy-200 hover:bg-navy-800 hover:text-white'
                    }
                    `
                  }
                >

                  <Icon size={18} />

                  {link.label}

                </NavLink>

              );

            }
          )}

        </nav>


        {/* ====================================================
            HELP
        ==================================================== */}

        <div className="absolute bottom-0 w-full p-4">

          <div className="rounded-xl bg-navy-800 p-4">

            <div className="flex items-start gap-3">

              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-navy-700 text-navy-200">

                <CircleHelp
                  size={18}
                />

              </div>


              <div className="min-w-0">

                <p className="text-sm font-semibold text-white">
                  Need help?
                </p>

                <p className="mt-1 text-xs leading-5 text-navy-300">
                  Learn how VentureAI validates and compares startup ideas.
                </p>


                <button
                  type="button"
                  onClick={() =>
                    setHelpOpen(true)
                  }
                  className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-white hover:text-red-300"
                >
                  How it works
                  <ArrowRight
                    size={13}
                  />
                </button>

              </div>

            </div>

          </div>

        </div>

      </aside>


      {/* ======================================================
          HELP MODAL
      ====================================================== */}

      {helpOpen && (

        <div
          className="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 p-4"
          onClick={closeHelp}
        >

          <div
            className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl"
            onClick={(event) =>
              event.stopPropagation()
            }
          >

            {/* Header */}

            <div className="flex items-start justify-between">

              <div className="flex items-center gap-3">

                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-50 text-brand-red">

                  <CircleHelp
                    size={20}
                  />

                </div>

                <div>

                  <h2 className="font-bold text-navy-900">
                    How VentureAI Works
                  </h2>

                  <p className="text-xs text-slate-400">
                    From startup idea to business intelligence
                  </p>

                </div>

              </div>


              <button
                type="button"
                onClick={closeHelp}
                className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                aria-label="Close help"
              >
                <X size={18} />
              </button>

            </div>


            {/* Steps */}

            <div className="mt-6 space-y-4">

              <HelpStep
                number="1"
                title="Describe your startup"
                text="Enter the idea, problem, solution, customers, business model and financial assumptions."
              />

              <HelpStep
                number="2"
                title="Input quality check"
                text="VentureAI checks whether the submitted information is meaningful and sufficient before running the analysis."
              />

              <HelpStep
                number="3"
                title="AI & ML analysis"
                text="NLP extracts business features and the BART model produces the viability prediction."
              />

              <HelpStep
                number="4"
                title="Business intelligence"
                text="The platform analyzes market opportunity, competitors, SWOT, risks and recommendations."
              />

              <HelpStep
                number="5"
                title="Financial analysis"
                text="Revenue, expenses, profitability, cash flow, break-even and other financial indicators are calculated."
              />

            </div>


            {/* Score explanation */}

            <div className="mt-6 rounded-xl bg-slate-50 p-4">

              <p className="text-sm font-semibold text-navy-900">
                Viability Score
              </p>

              <div className="mt-3 grid grid-cols-2 gap-2 text-xs">

                <ScoreRange
                  range="80–100"
                  label="High Potential"
                />

                <ScoreRange
                  range="60–79"
                  label="Moderate Potential"
                />

                <ScoreRange
                  range="40–59"
                  label="Needs Improvement"
                />

                <ScoreRange
                  range="0–39"
                  label="High Risk"
                />

              </div>

            </div>


            {/* CTA */}

            <button
              type="button"
              onClick={startValidation}
              className="btn-accent mt-6 w-full justify-center"
            >
              <Sparkles size={16} />
              Start New Validation
            </button>

          </div>

        </div>

      )}

    </>

  );
}


/* ============================================================
   HELP STEP
============================================================ */

function HelpStep({
  number,
  title,
  text,
}) {

  return (

    <div className="flex gap-3">

      <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-navy-900 text-xs font-bold text-white">

        {number}

      </div>

      <div>

        <p className="text-sm font-semibold text-navy-900">
          {title}
        </p>

        <p className="mt-0.5 text-xs leading-5 text-slate-500">
          {text}
        </p>

      </div>

    </div>

  );
}


/* ============================================================
   SCORE RANGE
============================================================ */

function ScoreRange({
  range,
  label,
}) {

  return (

    <div className="rounded-lg border border-slate-200 bg-white p-2">

      <p className="font-bold text-navy-900">
        {range}
      </p>

      <p className="mt-0.5 text-slate-500">
        {label}
      </p>

    </div>

  );

}
