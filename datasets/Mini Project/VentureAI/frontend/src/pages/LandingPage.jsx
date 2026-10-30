import React from 'react';
import { Link } from 'react-router-dom';
import {
  Brain, TrendingUp, Swords, Globe2, Wallet, Sparkles, LayoutDashboard, FileDown,
  ArrowRight, CheckCircle2, ChevronRight,
} from 'lucide-react';

const FEATURES = [
  { icon: Brain, title: 'AI Startup Validation', desc: 'NLP-driven analysis of your idea, problem and solution statements.' },
  { icon: TrendingUp, title: 'BART Prediction', desc: 'A real, trainable ML model scores viability from 0-100.' },
  { icon: Sparkles, title: 'SWOT Analysis', desc: 'AI-generated strengths, weaknesses, opportunities and threats.' },
  { icon: Swords, title: 'Competitor Intelligence', desc: 'Compare yourself against known or auto-discovered competitors.' },
  { icon: Globe2, title: 'Market Opportunity', desc: 'Demand, growth potential, segments and trends at a glance.' },
  { icon: Wallet, title: 'Financial Planning', desc: 'Revenue, P&L, cash flow, balance sheet & break-even, computed for you.' },
  { icon: LayoutDashboard, title: 'BI Dashboard', desc: 'One interactive dashboard for every metric that matters.' },
  { icon: FileDown, title: 'PDF Reports', desc: 'A polished, investor-ready validation report, one click away.' },
];

const STEPS = [
  { n: '01', title: 'Describe your idea', desc: 'Tell us the problem, solution, market and business model.' },
  { n: '02', title: 'NLP processing', desc: 'We extract keywords, industry and key business signals from your text.' },
  { n: '03', title: 'BART prediction', desc: 'A trained BART model scores your viability out of 100.' },
  { n: '04', title: 'AI business intelligence', desc: 'SWOT, competitor and market analysis generated from your data.' },
  { n: '05', title: 'Get your report', desc: 'A full BI dashboard and downloadable PDF validation report.' },
];

const PIPELINE = ['Startup Idea', 'NLP Analysis', 'BART Prediction', 'Market Intelligence', 'Financial Analysis', 'AI Recommendations', 'Validation Report'];

const STATS = [
  { value: '15+', label: 'Analysis Modules' },
  { value: '0-100', label: 'Viability Scoring' },
  { value: '3', label: 'Year Financial Projection' },
  { value: '100%', label: 'Data-Driven Reports' },
];

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white text-slate-800">
      <header className="sticky top-0 z-30 border-b border-slate-100 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-navy-900 font-black text-white">V</div>
            <span className="text-lg font-extrabold text-navy-900">VentureAI</span>
          </div>
          <nav className="hidden items-center gap-8 text-sm font-medium text-slate-600 md:flex">
            <a href="#features" className="hover:text-navy-900">Features</a>
            <a href="#how-it-works" className="hover:text-navy-900">How it Works</a>
            <a href="#stats" className="hover:text-navy-900">Why VentureAI</a>
          </nav>
          <div className="flex items-center gap-3">
            <Link to="/login" className="text-sm font-semibold text-navy-800 hover:text-brand-red">Log in</Link>
            <Link to="/register" className="btn-accent">Get Started</Link>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-b from-navy-950 via-navy-900 to-navy-800 text-white">
        <div className="absolute inset-0 opacity-20" style={{ backgroundImage: 'radial-gradient(circle at 20% 20%, #c81e2d 0, transparent 35%), radial-gradient(circle at 80% 0%, #3b5299 0, transparent 40%)' }} />
        <div className="relative mx-auto max-w-5xl px-6 py-24 text-center">
          <span className="mb-5 inline-block rounded-full border border-white/20 bg-white/10 px-4 py-1 text-xs font-semibold tracking-wide text-navy-100">
            AI-POWERED STARTUP VALIDATION &amp; BUSINESS INTELLIGENCE
          </span>
          <h1 className="text-4xl font-extrabold leading-tight sm:text-5xl lg:text-6xl">
            Validate Your Startup Idea<br /> with <span className="text-brand-red">AI</span>
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-lg text-navy-100">
            Turn your startup idea into data-driven business intelligence with AI-powered market, competitor and financial analysis.
          </p>
          <div className="mt-9 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <Link to="/register" className="btn-accent px-7 py-3 text-base">
              Validate My Idea <ArrowRight size={18} />
            </Link>
            <Link to="/login" className="btn-outline border-white/30 bg-transparent px-7 py-3 text-base text-white hover:bg-white/10">
              Explore Dashboard
            </Link>
          </div>

          <div className="mt-16 flex flex-wrap items-center justify-center gap-2 text-xs font-semibold text-navy-100 sm:text-sm">
            {PIPELINE.map((step, idx) => (
              <React.Fragment key={step}>
                <span className="rounded-full border border-white/15 bg-white/5 px-4 py-2">{step}</span>
                {idx < PIPELINE.length - 1 && <ChevronRight size={16} className="text-navy-300" />}
              </React.Fragment>
            ))}
          </div>
        </div>
      </section>

      {/* Stats */}
      <section id="stats" className="border-b border-slate-100 bg-navy-50/50">
        <div className="mx-auto grid max-w-6xl grid-cols-2 gap-6 px-6 py-12 sm:grid-cols-4">
          {STATS.map((s) => (
            <div key={s.label} className="text-center">
              <p className="text-3xl font-extrabold text-navy-900">{s.value}</p>
              <p className="mt-1 text-sm text-slate-500">{s.label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Features */}
      <section id="features" className="mx-auto max-w-7xl px-6 py-24">
        <div className="mx-auto mb-14 max-w-2xl text-center">
          <h2 className="text-3xl font-extrabold text-navy-900">Everything a validation platform should be</h2>
          <p className="mt-3 text-slate-500">Not a simple idea generator — a complete startup decision-support system.</p>
        </div>
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((f) => (
            <div key={f.title} className="card">
              <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-navy-50 text-navy-700">
                <f.icon size={22} />
              </div>
              <h3 className="font-bold text-navy-900">{f.title}</h3>
              <p className="mt-1.5 text-sm text-slate-500">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section id="how-it-works" className="bg-slate-50 py-24">
        <div className="mx-auto max-w-6xl px-6">
          <div className="mx-auto mb-14 max-w-2xl text-center">
            <h2 className="text-3xl font-extrabold text-navy-900">How it works</h2>
            <p className="mt-3 text-slate-500">From idea to investor-ready report in five steps.</p>
          </div>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-5">
            {STEPS.map((s) => (
              <div key={s.n} className="card text-center">
                <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-navy-900 text-sm font-bold text-white">
                  {s.n}
                </div>
                <h3 className="font-bold text-navy-900">{s.title}</h3>
                <p className="mt-1.5 text-sm text-slate-500">{s.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="bg-navy-900 py-20 text-center text-white">
        <h2 className="text-3xl font-extrabold">Ready to validate your idea?</h2>
        <p className="mx-auto mt-3 max-w-xl text-navy-200">
          Join entrepreneurs, students and founders using data — not guesswork — to build with confidence.
        </p>
        <Link to="/register" className="btn-accent mt-8 inline-flex px-8 py-3 text-base">
          Get Started Free <ArrowRight size={18} />
        </Link>
        <div className="mx-auto mt-8 flex max-w-xl flex-wrap justify-center gap-x-6 gap-y-2 text-sm text-navy-200">
          <span className="flex items-center gap-1.5"><CheckCircle2 size={16} /> No credit card required</span>
          <span className="flex items-center gap-1.5"><CheckCircle2 size={16} /> Full BI dashboard included</span>
          <span className="flex items-center gap-1.5"><CheckCircle2 size={16} /> Downloadable PDF reports</span>
        </div>
      </section>

      <footer className="border-t border-slate-100 bg-white py-10">
        <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 px-6 sm:flex-row">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-navy-900 text-sm font-black text-white">V</div>
            <span className="font-extrabold text-navy-900">VentureAI</span>
          </div>
          <p className="text-sm text-slate-400">Validate your idea. Understand your market. Build with confidence.</p>
          <p className="text-xs text-slate-400">&copy; {new Date().getFullYear()} VentureAI. Academic mini project.</p>
        </div>
      </footer>
    </div>
  );
}
