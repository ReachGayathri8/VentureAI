import React, { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Plus,
  Trash2,
  Wand2,
  ArrowLeft,
  ArrowRight,
  Sparkles,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

import { startupsApi } from '../api/startups';
import { extractErrorMessage } from '../api/client';
import ProgressStepper from '../components/ProgressStepper';
import PageHeader from '../components/PageHeader';
import { INDUSTRIES } from '../utils/constants';

const STEPS = [
  'Basic Info',
  'Business Details',
  'Financials',
  'Competitors',
  'Review',
];

/*
 * Business Model is different from customer type.
 * B2B/B2C/B2B2C are better treated as customer models,
 * but the current backend stores this as a single businessModel string.
 *
 * Therefore, these values remain backend-compatible.
 */
const BUSINESS_MODELS = [
  'SaaS / Subscription',
  'Marketplace',
  'E-commerce',
  'Direct Sales',
  'Transaction / Commission',
  'Usage-Based / Pay-As-You-Go',
  'Freemium',
  'Advertising',
  'Licensing',
  'Franchise',
  'Wholesale',
  'Retail',
  'Manufacturing',
  'Service-Based',
  'Consulting',
  'Agency',
  'Affiliate',
  'Brokerage',
  'Aggregator',
  'On-Demand',
  'Rental / Leasing',
  'Membership',
  'Digital Products',
  'Enterprise Contracts',
  'Revenue Share',
  'Outcome-Based',
  'Dropshipping',
  'D2C',
  'B2B',
  'B2C',
  'B2B2C',
  'Hybrid',
  'Other',
];

const initialForm = {
  // Step 1
  startupName: '',
  startupIdea: '',
  industry: 'Technology',
  businessCategory: '',
  targetMarket: '',
  location: '',
  businessModel: 'B2C',

  // Step 2
  problem: '',
  solution: '',
  targetCustomers: '',
  uniqueValueProposition: '',
  revenueModel: '',
  marketingStrategy: '',
  businessDescription: '',

  // Step 3
  initialInvestment: '',
  founderInvestment: '',
  externalFunding: '',

  monthlyOperatingCost: '',
  employeeCost: '',
  marketingCost: '',
  infrastructureCost: '',
  productDevelopmentCost: '',

  expectedMonthlyCustomers: '',
  averageCustomerRevenue: '',
  expectedGrowthRate: '',
  projectionPeriodYears: 3,

  fixedCosts: '',
  variableCostPerUnit: '',
  sellingPricePerUnit: '',

  // Step 4
  competitors: [],
  autoDiscoverCompetitors: false,
};

export default function NewValidation() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const editId = searchParams.get('edit');
  const isEditMode = Boolean(editId);

  const [step, setStep] = useState(0);
  const [form, setForm] = useState(initialForm);
  const [submitting, setSubmitting] = useState(false);
  const [loadingStartup, setLoadingStartup] = useState(Boolean(editId));

  React.useEffect(() => {
    let cancelled = false;

    if (!editId) {
      setLoadingStartup(false);
      return undefined;
    }

    const loadStartup = async () => {
      setLoadingStartup(true);

      try {
        const startup = await startupsApi.get(editId);

        if (cancelled) return;

        const competitors = Array.isArray(startup.competitors)
          ? startup.competitors.map((competitor) => ({
              name: competitor.name || '',
              website: competitor.website || '',
              description: competitor.description || '',
            }))
          : [];

        setForm((current) => ({
          ...current,
          startupName: startup.name || '',
          startupIdea: startup.startupIdea || '',
          industry: startup.industry || 'Technology',
          businessCategory: startup.businessCategory || '',
          targetMarket: startup.targetMarket || '',
          location: startup.location || '',
          businessModel: startup.businessModel || 'B2C',
          problem: startup.problem || '',
          solution: startup.solution || '',
          targetCustomers: startup.targetCustomers || '',
          uniqueValueProposition: startup.uniqueValueProposition || '',
          revenueModel: startup.revenueModel || '',
          marketingStrategy: startup.marketingStrategy || '',
          businessDescription: startup.description || '',

          // STEP 3 — Restore saved financial data
          initialInvestment: startup.initialInvestment ?? '',
          founderInvestment: startup.founderInvestment ?? '',
          externalFunding: startup.externalFunding ?? '',
          monthlyOperatingCost: startup.monthlyOperatingCost ?? '',
          employeeCost: startup.employeeCost ?? '',
          marketingCost: startup.marketingCost ?? '',
          infrastructureCost: startup.infrastructureCost ?? '',
          productDevelopmentCost: startup.productDevelopmentCost ?? '',
          expectedMonthlyCustomers: startup.expectedMonthlyCustomers ?? '',
          averageCustomerRevenue: startup.averageCustomerRevenue ?? '',
          expectedGrowthRate: startup.expectedGrowthRate ?? '',
          projectionPeriodYears: startup.projectionPeriodYears ?? '',
          fixedCosts: startup.fixedCosts ?? '',
          variableCostPerUnit: startup.variableCostPerUnit ?? '',
          sellingPricePerUnit: startup.sellingPricePerUnit ?? '',

          competitors,
          autoDiscoverCompetitors:
            startup.autoDiscoverCompetitors === true ||
            startup.autoDiscoverCompetitors === 'true',
        }));
      } catch (err) {
        if (!cancelled) {
          toast.error(
            extractErrorMessage(err, 'Unable to load startup for editing.')
          );
          navigate('/startups');
        }
      } finally {
        if (!cancelled) {
          setLoadingStartup(false);
        }
      }
    };

    loadStartup();

    return () => {
      cancelled = true;
    };
  }, [editId, navigate]);

  const update = (field, value) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const next = () => {
    const error = validateStep(step);

    if (error) {
      toast.error(error);
      return;
    }

    setStep((current) =>
      Math.min(current + 1, STEPS.length - 1)
    );
  };

  const back = () => {
    setStep((current) => Math.max(current - 1, 0));
  };

  const validateStep = (currentStep) => {
    if (currentStep === 0) {
      if (!form.startupName.trim()) {
        return 'Startup name is required.';
      }

      if (!form.startupIdea.trim()) {
        return 'Startup idea is required.';
      }

      if (!form.industry) {
        return 'Please select an industry.';
      }

      if (!form.businessModel) {
        return 'Please select a business model.';
      }

      return null;
    }

    if (currentStep === 1) {
      if (!form.problem.trim()) {
        return 'Please describe the problem being solved.';
      }

      if (!form.solution.trim()) {
        return 'Please describe the proposed solution.';
      }

      if (!form.targetCustomers.trim()) {
        return 'Please specify your target customers.';
      }

      if (!form.revenueModel.trim()) {
        return 'Please specify your revenue model.';
      }

      return null;
    }

    if (currentStep === 2) {
      const numericChecks = [
        ['initialInvestment', 'Initial investment'],
        ['founderInvestment', 'Founder investment'],
        ['externalFunding', 'External funding'],
        ['monthlyOperatingCost', 'Monthly operating cost'],
        ['employeeCost', 'Employee cost'],
        ['marketingCost', 'Marketing cost'],
        ['infrastructureCost', 'Infrastructure cost'],
        ['productDevelopmentCost', 'Product development cost'],
        ['expectedMonthlyCustomers', 'Expected monthly customers'],
        ['averageCustomerRevenue', 'Average customer revenue'],
        ['expectedGrowthRate', 'Expected growth rate'],
        ['fixedCosts', 'Fixed costs'],
        ['variableCostPerUnit', 'Variable cost per unit'],
        ['sellingPricePerUnit', 'Selling price per unit'],
      ];

      for (const [field, label] of numericChecks) {
        const value = form[field];

        if (value !== '' && value !== null) {
          const number = Number(value);

          if (Number.isNaN(number)) {
            return `${label} must be a valid number.`;
          }

          if (number < 0) {
            return `${label} cannot be negative.`;
          }
        }
      }

      if (
        form.expectedGrowthRate !== '' &&
        Number(form.expectedGrowthRate) > 1000
      ) {
        return 'Expected growth rate cannot exceed 1000%.';
      }

      if (
        form.projectionPeriodYears &&
        Number(form.projectionPeriodYears) < 1
      ) {
        return 'Projection period must be at least 1 year.';
      }

      return null;
    }

    if (currentStep === 3) {
      const incompleteCompetitor = form.competitors.some(
        (competitor) =>
          !competitor.name?.trim()
      );

      if (incompleteCompetitor) {
        return 'Please enter a competitor name or remove the empty competitor.';
      }

      return null;
    }

    return null;
  };

  const addCompetitor = () => {
    update('competitors', [
      ...form.competitors,
      {
        name: '',
        website: '',
        description: '',
      },
    ]);
  };

  const updateCompetitor = (index, field, value) => {
    const competitors = [...form.competitors];

    competitors[index] = {
      ...competitors[index],
      [field]: value,
    };

    update('competitors', competitors);
  };

  const removeCompetitor = (index) => {
    update(
      'competitors',
      form.competitors.filter((_, i) => i !== index)
    );
  };

  const handleSubmit = async () => {
    const error =
      validateStep(0) ||
      validateStep(1) ||
      validateStep(2) ||
      validateStep(3);

    if (error) {
      toast.error(error);
      return;
    }

    setSubmitting(true);

    try {
      const numericFields = [
        'initialInvestment',
        'founderInvestment',
        'externalFunding',
        'monthlyOperatingCost',
        'employeeCost',
        'marketingCost',
        'infrastructureCost',
        'productDevelopmentCost',
        'expectedMonthlyCustomers',
        'averageCustomerRevenue',
        'expectedGrowthRate',
        'projectionPeriodYears',
        'fixedCosts',
        'variableCostPerUnit',
        'sellingPricePerUnit',
      ];

      const payload = {
        ...form,
      };

      numericFields.forEach((field) => {
        if (
          payload[field] === '' ||
          payload[field] === null ||
          payload[field] === undefined
        ) {
          payload[field] = null;
        } else {
          payload[field] = Number(payload[field]);
        }
      });

      payload.competitors = form.competitors
        .filter((competitor) => competitor.name?.trim())
        .map((competitor) => ({
          name: competitor.name.trim(),
          website: competitor.website?.trim() || '',
          description: competitor.description?.trim() || '',
        }));

      let startup;

      if (isEditMode) {
        startup = await startupsApi.update(editId, payload);

        toast.success(
          'Startup updated! Starting AI re-validation...'
        );
      } else {
        startup = await startupsApi.create(payload);

        toast.success(
          'Startup created! Starting AI validation...'
        );
      }

      navigate(`/validate/${startup.id}/processing`);
    } catch (err) {
      toast.error(
        extractErrorMessage(
          err,
          isEditMode ? 'Unable to update startup.' : 'Unable to create startup.'
        )
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        title={isEditMode ? 'Edit Startup Validation' : 'New Startup Validation'}
        subtitle={
          isEditMode
            ? 'Update your startup details and run the VentureAI validation again.'
            : 'Provide the key details about your startup. VentureAI will analyze the idea, market, competition and financial potential.'
        }
      />

      <ProgressStepper
        steps={STEPS}
        current={step}
      />

      {loadingStartup && (
        <div className="mb-4 rounded-xl border border-navy-100 bg-navy-50 p-4 text-sm text-navy-800">
          Loading your startup details...
        </div>
      )}

      <div className="card">
        {step === 0 && (
          <StepBasicInfo
            form={form}
            update={update}
          />
        )}

        {step === 1 && (
          <StepBusinessDetails
            form={form}
            update={update}
          />
        )}

        {step === 2 && (
          <StepFinancials
            form={form}
            update={update}
          />
        )}

        {step === 3 && (
          <StepCompetitors
            form={form}
            update={update}
            addCompetitor={addCompetitor}
            updateCompetitor={updateCompetitor}
            removeCompetitor={removeCompetitor}
          />
        )}

        {step === 4 && (
          <StepReview
            form={form}
            onEdit={setStep}
          />
        )}

        <div className="mt-8 flex items-center justify-between border-t border-slate-100 pt-6">
          <button
            type="button"
            onClick={back}
            disabled={step === 0 || submitting || loadingStartup}
            className="btn-outline disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ArrowLeft size={16} />
            Back
          </button>

          {step < STEPS.length - 1 ? (
            <button
              type="button"
              onClick={next}
              disabled={submitting || loadingStartup}
              className="btn-primary"
            >
              Next
              <ArrowRight size={16} />
            </button>
          ) : (
            <button
              type="button"
              onClick={handleSubmit}
              disabled={submitting || loadingStartup}
              className="btn-accent px-6 disabled:opacity-60"
            >
              <Sparkles size={16} />

              {loadingStartup
                ? 'Loading Startup...'
                : submitting
                  ? (isEditMode
                      ? 'Starting AI Re-validation...'
                      : 'Starting AI Validation...')
                  : (isEditMode
                    ? 'Update & Re-validate'
                    : 'Start AI Validation')}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   COMMON FIELD
========================================================= */

function Field({
  label,
  children,
  required = false,
  hint,
}) {
  return (
    <div>
      <label className="label-field">
        {label}

        {required && (
          <span className="ml-1 text-brand-red">
            *
          </span>
        )}
      </label>

      {children}

      {hint && (
        <p className="mt-1 text-xs text-slate-400">
          {hint}
        </p>
      )}
    </div>
  );
}

/* =========================================================
   STEP 1
========================================================= */

function StepBasicInfo({ form, update }) {
  return (
    <div>
      <div className="mb-6">
        <h2 className="section-title">
          Step 1 — Basic Information
        </h2>

        <p className="mt-1 text-sm text-slate-500">
          Tell us what your startup is and who it is built for.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
        <Field
          label="Startup Name"
          required
        >
          <input
            className="input-field"
            value={form.startupName}
            onChange={(e) =>
              update(
                'startupName',
                e.target.value
              )
            }
            placeholder="e.g. SmartFood AI"
            maxLength={200}
          />
        </Field>

        <Field
          label="Industry"
          required
        >
          <select
            className="input-field"
            value={form.industry}
            onChange={(e) =>
              update(
                'industry',
                e.target.value
              )
            }
          >
            {INDUSTRIES.map((industry) => (
              <option
                key={industry}
                value={industry}
              >
                {industry}
              </option>
            ))}
          </select>
        </Field>

        <Field
          label="Business Category"
          hint="A more specific category within your industry."
        >
          <input
            className="input-field"
            value={form.businessCategory}
            onChange={(e) =>
              update(
                'businessCategory',
                e.target.value
              )
            }
            placeholder="e.g. Food Delivery Platform"
            maxLength={150}
          />
        </Field>

        <Field
          label="Business Model"
          required
        >
          <select
            className="input-field"
            value={form.businessModel}
            onChange={(e) =>
              update(
                'businessModel',
                e.target.value
              )
            }
          >
            {BUSINESS_MODELS.map((model) => (
              <option
                key={model}
                value={model}
              >
                {model}
              </option>
            ))}
          </select>
        </Field>

        <Field
          label="Target Market"
          required
          hint="Describe the market you want to serve."
        >
          <input
            className="input-field"
            value={form.targetMarket}
            onChange={(e) =>
              update(
                'targetMarket',
                e.target.value
              )
            }
            placeholder="e.g. Small restaurants in urban India"
            maxLength={200}
          />
        </Field>

        <Field
          label="Location"
          required
          hint="Primary city, region or country where you plan to operate."
        >
          <input
            className="input-field"
            value={form.location}
            onChange={(e) =>
              update(
                'location',
                e.target.value
              )
            }
            placeholder="e.g. Hyderabad, Telangana, India"
            maxLength={150}
          />
        </Field>
      </div>

      <div className="mt-5">
        <Field
          label="Startup Idea"
          required
          hint="Clearly explain what the startup does and how it creates value."
        >
          <textarea
            rows={4}
            className="input-field"
            value={form.startupIdea}
            onChange={(e) =>
              update(
                'startupIdea',
                e.target.value
              )
            }
            placeholder="Describe your startup idea in a few clear sentences..."
            maxLength={3000}
          />
        </Field>
      </div>
    </div>
  );
}

/* =========================================================
   STEP 2
========================================================= */

function StepBusinessDetails({ form, update }) {
  return (
    <div>
      <div className="mb-6">
        <h2 className="section-title">
          Step 2 — Business Details
        </h2>

        <p className="mt-1 text-sm text-slate-500">
          Explain the problem, solution, customers and
          business strategy.
        </p>
      </div>

      <div className="space-y-5">
        <Field
          label="Problem Being Solved"
          required
          hint="What important problem or unmet need exists?"
        >
          <textarea
            rows={3}
            className="input-field"
            value={form.problem}
            onChange={(e) =>
              update(
                'problem',
                e.target.value
              )
            }
            placeholder="What problem are customers facing today?"
            maxLength={3000}
          />
        </Field>

        <Field
          label="Proposed Solution"
          required
          hint="How exactly does your startup solve the problem?"
        >
          <textarea
            rows={3}
            className="input-field"
            value={form.solution}
            onChange={(e) =>
              update(
                'solution',
                e.target.value
              )
            }
            placeholder="Explain your product or service..."
            maxLength={3000}
          />
        </Field>

        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <Field
            label="Target Customers"
            required
            hint="Who will actually pay for or use your solution?"
          >
            <input
              className="input-field"
              value={form.targetCustomers}
              onChange={(e) =>
                update(
                  'targetCustomers',
                  e.target.value
                )
              }
              placeholder="e.g. Small restaurants and cloud kitchens"
              maxLength={500}
            />
          </Field>

          <Field
            label="Revenue Model"
            required
            hint="Explain how the startup will make money."
          >
            <input
              className="input-field"
              value={form.revenueModel}
              onChange={(e) =>
                update(
                  'revenueModel',
                  e.target.value
                )
              }
              placeholder="e.g. Monthly subscription per restaurant"
              maxLength={500}
            />
          </Field>
        </div>

        <Field
          label="Unique Value Proposition"
          hint="Why should customers choose you instead of alternatives?"
        >
          <textarea
            rows={3}
            className="input-field"
            value={form.uniqueValueProposition}
            onChange={(e) =>
              update(
                'uniqueValueProposition',
                e.target.value
              )
            }
            placeholder="What makes your startup different or better?"
            maxLength={2000}
          />
        </Field>

        <Field
          label="Marketing Strategy"
          hint="How will customers discover and buy your product?"
        >
          <textarea
            rows={3}
            className="input-field"
            value={form.marketingStrategy}
            onChange={(e) =>
              update(
                'marketingStrategy',
                e.target.value
              )
            }
            placeholder="e.g. Instagram, Google Ads, partnerships, referrals..."
            maxLength={2000}
          />
        </Field>

        <Field
          label="Business Description"
          hint="Provide additional context about how the business will operate."
        >
          <textarea
            rows={4}
            className="input-field"
            value={form.businessDescription}
            onChange={(e) =>
              update(
                'businessDescription',
                e.target.value
              )
            }
            placeholder="Describe the business, product/service, operations and future plans..."
            maxLength={4000}
          />
        </Field>
      </div>
    </div>
  );
}

/* =========================================================
   NUMBER FIELD
========================================================= */

function NumberField({
  label,
  value,
  onChange,
  prefix,
  suffix,
  hint,
}) {
  return (
    <Field
      label={label}
      hint={hint}
    >
      <div className="relative">
        {prefix && (
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-slate-400">
            {prefix}
          </span>
        )}

        <input
          type="number"
          className={`input-field ${
            prefix ? 'pl-10' : ''
          } ${suffix ? 'pr-12' : ''}`}
          value={value}
          onChange={(e) =>
            onChange(e.target.value)
          }
          min="0"
          step="any"
        />

        {suffix && (
          <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-slate-400">
            {suffix}
          </span>
        )}
      </div>
    </Field>
  );
}

/* =========================================================
   STEP 3
========================================================= */

function StepFinancials({ form, update }) {
  return (
    <div>
      <div className="mb-6">
        <h2 className="section-title">
          Step 3 — Financial Inputs
        </h2>

        <p className="mt-1 text-sm text-slate-500">
          These assumptions are used by VentureAI to
          estimate revenue, profitability, cash flow and
          financial viability.
        </p>
      </div>

      {/* FUNDING */}
      <FinancialSection
        title="Startup Funding"
        description="Enter the capital available to start and fund the business."
      >
        <NumberField
          label="Initial Investment"
          prefix="₹"
          value={form.initialInvestment}
          onChange={(value) =>
            update(
              'initialInvestment',
              value
            )
          }
          hint="Total capital required to start the business."
        />

        <NumberField
          label="Founder Investment"
          prefix="₹"
          value={form.founderInvestment}
          onChange={(value) =>
            update(
              'founderInvestment',
              value
            )
          }
          hint="Amount invested by the founder(s)."
        />

        <NumberField
          label="External Funding"
          prefix="₹"
          value={form.externalFunding}
          onChange={(value) =>
            update(
              'externalFunding',
              value
            )
          }
          hint="Funding received or expected from outside sources."
        />
      </FinancialSection>

      {/* MONTHLY COSTS */}
      <FinancialSection
        title="Monthly Operating Costs"
        description="Enter the recurring costs required to operate the business."
      >
        <NumberField
          label="Monthly Operating Cost"
          prefix="₹"
          value={form.monthlyOperatingCost}
          onChange={(value) =>
            update(
              'monthlyOperatingCost',
              value
            )
          }
          hint="Total monthly operating cost. Avoid double-counting the itemized costs below."
        />

        <NumberField
          label="Employee Cost"
          prefix="₹"
          value={form.employeeCost}
          onChange={(value) =>
            update(
              'employeeCost',
              value
            )
          }
          hint="Monthly salaries, wages and employee-related costs."
        />

        <NumberField
          label="Marketing Cost"
          prefix="₹"
          value={form.marketingCost}
          onChange={(value) =>
            update(
              'marketingCost',
              value
            )
          }
          hint="Expected monthly marketing and customer acquisition spend."
        />

        <NumberField
          label="Infrastructure Cost"
          prefix="₹"
          value={form.infrastructureCost}
          onChange={(value) =>
            update(
              'infrastructureCost',
              value
            )
          }
          hint="Monthly rent, cloud, utilities, software and infrastructure."
        />

        <NumberField
          label="Product Development Cost"
          prefix="₹"
          value={form.productDevelopmentCost}
          onChange={(value) =>
            update(
              'productDevelopmentCost',
              value
            )
          }
          hint="Expected monthly product development expense."
        />
      </FinancialSection>

      {/* REVENUE */}
      <FinancialSection
        title="Revenue Assumptions"
        description="Estimate your initial customer base, pricing and growth."
      >
        <NumberField
          label="Expected Monthly Customers"
          value={form.expectedMonthlyCustomers}
          onChange={(value) =>
            update(
              'expectedMonthlyCustomers',
              value
            )
          }
          hint="Expected active customers per month at the starting point."
        />

        <NumberField
          label="Average Customer Revenue"
          prefix="₹"
          value={form.averageCustomerRevenue}
          onChange={(value) =>
            update(
              'averageCustomerRevenue',
              value
            )
          }
          hint="Average revenue generated per customer."
        />

        <NumberField
          label="Expected Annual Growth Rate"
          value={form.expectedGrowthRate}
          suffix="%"
          onChange={(value) =>
            update(
              'expectedGrowthRate',
              value
            )
          }
          hint="Expected annual business/revenue growth assumption."
        />

        <Field
          label="Projection Period"
        >
          <select
            className="input-field"
            value={form.projectionPeriodYears}
            onChange={(e) =>
              update(
                'projectionPeriodYears',
                e.target.value
              )
            }
          >
            <option value={1}>
              1 Year
            </option>
            <option value={2}>
              2 Years
            </option>
            <option value={3}>
              3 Years
            </option>
          </select>
        </Field>
      </FinancialSection>

      {/* UNIT ECONOMICS */}
      <FinancialSection
        title="Unit Economics & Break-even"
        description="Optional, but strongly recommended when you know your pricing and variable costs."
      >
        <NumberField
          label="Fixed Costs"
          prefix="₹"
          value={form.fixedCosts}
          onChange={(value) =>
            update(
              'fixedCosts',
              value
            )
          }
          hint="Monthly fixed costs used for break-even analysis."
        />

        <NumberField
          label="Variable Cost / Unit"
          prefix="₹"
          value={form.variableCostPerUnit}
          onChange={(value) =>
            update(
              'variableCostPerUnit',
              value
            )
          }
          hint="Cost directly associated with producing or delivering one unit."
        />

        <NumberField
          label="Selling Price / Unit"
          prefix="₹"
          value={form.sellingPricePerUnit}
          onChange={(value) =>
            update(
              'sellingPricePerUnit',
              value
            )
          }
          hint="Selling price for one unit."
        />
      </FinancialSection>

      <div className="mt-5 flex gap-3 rounded-xl border border-amber-100 bg-amber-50 p-4">
        <AlertCircle
          size={19}
          className="mt-0.5 shrink-0 text-amber-600"
        />

        <div>
          <p className="text-sm font-semibold text-amber-900">
            Financial assumptions
          </p>

          <p className="mt-1 text-xs leading-5 text-amber-800">
            Enter realistic estimates rather than optimistic
            numbers. Better assumptions produce more useful
            viability and financial analysis.
          </p>
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   FINANCIAL SECTION
========================================================= */

function FinancialSection({
  title,
  description,
  children,
}) {
  return (
    <section className="mb-7">
      <div className="mb-4">
        <h3 className="text-sm font-bold uppercase tracking-wide text-navy-900">
          {title}
        </h3>

        <p className="mt-1 text-xs text-slate-400">
          {description}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {children}
      </div>
    </section>
  );
}

/* =========================================================
   STEP 4
========================================================= */

function StepCompetitors({
  form,
  update,
  addCompetitor,
  updateCompetitor,
  removeCompetitor,
}) {
  return (
    <div>
      <div className="mb-6">
        <h2 className="section-title">
          Step 4 — Competitor Information
        </h2>

        <p className="mt-1 text-sm text-slate-500">
          Add known competitors or let VentureAI discover
          competitors automatically.
        </p>
      </div>

      <div className="mb-6 rounded-xl border border-navy-100 bg-navy-50 p-4">
        <div className="flex items-start gap-3">
          <input
            type="checkbox"
            id="autoDiscover"
            checked={form.autoDiscoverCompetitors}
            onChange={(e) =>
              update(
                'autoDiscoverCompetitors',
                e.target.checked
              )
            }
            className="mt-1 h-4 w-4 rounded border-slate-300"
          />

          <div>
            <label
              htmlFor="autoDiscover"
              className="flex cursor-pointer items-center gap-2 text-sm font-semibold text-navy-900"
            >
              <Wand2 size={16} />
              Discover competitors automatically
            </label>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              VentureAI will use your startup idea, industry,
              target market and business information to
              identify potential competitors.
            </p>
          </div>
        </div>
      </div>

      {form.competitors.length === 0 && (
        <div className="rounded-xl border border-dashed border-slate-200 p-8 text-center">
          <p className="text-sm font-semibold text-slate-600">
            No competitors added
          </p>

          <p className="mt-1 text-xs text-slate-400">
            You can add known competitors or use AI
            discovery.
          </p>
        </div>
      )}

      <div className="space-y-4">
        {form.competitors.map(
          (competitor, index) => (
            <div
              key={index}
              className="rounded-xl border border-slate-200 bg-white p-4"
            >
              <div className="mb-4 flex items-center justify-between">
                <span className="text-sm font-semibold text-navy-900">
                  Competitor {index + 1}
                </span>

                <button
                  type="button"
                  onClick={() =>
                    removeCompetitor(index)
                  }
                  className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-brand-red"
                  title="Remove competitor"
                >
                  <Trash2 size={16} />
                </button>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Field
                  label="Competitor Name"
                  required
                >
                  <input
                    className="input-field"
                    placeholder="e.g. Competitor Inc."
                    value={competitor.name}
                    onChange={(e) =>
                      updateCompetitor(
                        index,
                        'name',
                        e.target.value
                      )
                    }
                  />
                </Field>

                <Field label="Website">
                  <input
                    className="input-field"
                    placeholder="https://example.com"
                    value={competitor.website}
                    onChange={(e) =>
                      updateCompetitor(
                        index,
                        'website',
                        e.target.value
                      )
                    }
                  />
                </Field>
              </div>

              <div className="mt-4">
                <Field
                  label="Description"
                  hint="What does this competitor offer?"
                >
                  <textarea
                    className="input-field"
                    rows={2}
                    placeholder="Short description of the competitor..."
                    value={competitor.description}
                    onChange={(e) =>
                      updateCompetitor(
                        index,
                        'description',
                        e.target.value
                      )
                    }
                  />
                </Field>
              </div>
            </div>
          )
        )}
      </div>

      <button
        type="button"
        onClick={addCompetitor}
        className="btn-outline mt-5"
      >
        <Plus size={16} />
        Add Competitor
      </button>
    </div>
  );
}

/* =========================================================
   STEP 5
========================================================= */

function StepReview({ form, onEdit }) {
  const financialComplete =
    form.initialInvestment !== '' ||
    form.expectedMonthlyCustomers !== '' ||
    form.averageCustomerRevenue !== '';

  const businessComplete =
    form.problem &&
    form.solution &&
    form.targetCustomers &&
    form.revenueModel;

  return (
    <div>
      <div className="mb-6">
        <h2 className="section-title">
          Step 5 — Review & Start Validation
        </h2>

        <p className="mt-1 text-sm text-slate-500">
          Review your information before starting the
          VentureAI validation pipeline.
        </p>
      </div>

      <div className="mb-6 rounded-xl border border-emerald-100 bg-emerald-50 p-4">
        <div className="flex items-start gap-3">
          <CheckCircle2
            size={20}
            className="mt-0.5 shrink-0 text-emerald-600"
          />

          <div>
            <p className="text-sm font-semibold text-emerald-900">
              Ready for AI validation
            </p>

            <p className="mt-1 text-xs leading-5 text-emerald-800">
              VentureAI will analyze your startup idea,
              business model, financial assumptions and
              competitive landscape.
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <ReviewBlock
          title="Basic Information"
          onEdit={() => onEdit(0)}
          rows={[
            ['Startup Name', form.startupName],
            ['Industry', form.industry],
            [
              'Business Category',
              form.businessCategory,
            ],
            [
              'Business Model',
              form.businessModel,
            ],
            [
              'Target Market',
              form.targetMarket,
            ],
            ['Location', form.location],
          ]}
        />

        <ReviewBlock
          title="Business Details"
          onEdit={() => onEdit(1)}
          rows={[
            ['Problem', form.problem],
            ['Solution', form.solution],
            [
              'Target Customers',
              form.targetCustomers,
            ],
            [
              'Revenue Model',
              form.revenueModel,
            ],
            [
              'Unique Value Proposition',
              form.uniqueValueProposition,
            ],
            [
              'Marketing Strategy',
              form.marketingStrategy,
            ],
          ]}
        />

        <ReviewBlock
          title="Financial Assumptions"
          onEdit={() => onEdit(2)}
          rows={[
            [
              'Initial Investment',
              formatCurrency(
                form.initialInvestment
              ),
            ],
            [
              'Founder Investment',
              formatCurrency(
                form.founderInvestment
              ),
            ],
            [
              'External Funding',
              formatCurrency(
                form.externalFunding
              ),
            ],
            [
              'Monthly Operating Cost',
              formatCurrency(
                form.monthlyOperatingCost
              ),
            ],
            [
              'Monthly Customers',
              form.expectedMonthlyCustomers,
            ],
            [
              'Average Customer Revenue',
              formatCurrency(
                form.averageCustomerRevenue
              ),
            ],
            [
              'Expected Growth',
              form.expectedGrowthRate
                ? `${form.expectedGrowthRate}%`
                : '-',
            ],
            [
              'Projection Period',
              `${form.projectionPeriodYears} year${
                Number(
                  form.projectionPeriodYears
                ) === 1
                  ? ''
                  : 's'
              }`,
            ],
          ]}
        />

        <ReviewBlock
          title="Competitors"
          onEdit={() => onEdit(3)}
          rows={[
            [
              'Known Competitors',
              form.competitors.length
                ? `${form.competitors.length} added`
                : 'None added',
            ],
            [
              'AI Discovery',
              form.autoDiscoverCompetitors
                ? 'Enabled'
                : 'Disabled',
            ],
          ]}
        />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2">
        <ReadinessItem
          complete={Boolean(
            form.startupName &&
              form.startupIdea &&
              form.industry &&
              form.businessModel
          )}
          text="Basic startup information"
        />

        <ReadinessItem
          complete={Boolean(businessComplete)}
          text="Core business details"
        />

        <ReadinessItem
          complete={Boolean(financialComplete)}
          text="Financial assumptions"
        />

        <ReadinessItem
          complete={
            form.competitors.length > 0 ||
            form.autoDiscoverCompetitors
          }
          text="Competitive analysis setup"
        />
      </div>

      <div className="mt-6 rounded-xl bg-slate-50 p-4">
        <p className="text-xs leading-5 text-slate-500">
          <strong className="text-slate-700">
            Important:
          </strong>{' '}
          VentureAI's financial results depend on the
          assumptions you provide. Use realistic estimates
          for customers, pricing, costs and growth.
        </p>
      </div>
    </div>
  );
}

/* =========================================================
   REVIEW BLOCK
========================================================= */

function ReviewBlock({
  title,
  rows,
  onEdit,
}) {
  return (
    <div className="rounded-xl border border-slate-100 bg-white p-5">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-sm font-bold text-navy-900">
          {title}
        </h3>

        <button
          type="button"
          onClick={onEdit}
          className="text-xs font-semibold text-navy-700 hover:text-brand-red"
        >
          Edit
        </button>
      </div>

      <dl className="space-y-3">
        {rows.map(([label, value]) => (
          <div key={label}>
            <dt className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
              {label}
            </dt>

            <dd className="mt-0.5 break-words text-sm text-slate-700">
              {value || '-'}
            </dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

/* =========================================================
   READINESS ITEM
========================================================= */

function ReadinessItem({
  complete,
  text,
}) {
  return (
    <div
      className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm ${
        complete
          ? 'border-emerald-100 bg-emerald-50 text-emerald-800'
          : 'border-slate-200 bg-slate-50 text-slate-500'
      }`}
    >
      <CheckCircle2
        size={16}
        className={
          complete
            ? 'text-emerald-600'
            : 'text-slate-300'
        }
      />

      {text}
    </div>
  );
}

/* =========================================================
   CURRENCY FORMATTER
========================================================= */

function formatCurrency(value) {
  if (
    value === '' ||
    value === null ||
    value === undefined
  ) {
    return '-';
  }

  const number = Number(value);

  if (Number.isNaN(number)) {
    return '-';
  }

  return `₹${number.toLocaleString('en-IN')}`;
}
