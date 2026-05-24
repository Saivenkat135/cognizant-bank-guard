/**
 * Premium KPI card. Tone picks the colored leading bar + dot so totals,
 * flagged, genuine etc. read at a glance.
 */
const TONES = {
  blue:    { bar: 'bg-blue-500',    dot: 'bg-blue-500',    soft: 'bg-blue-50' },
  red:     { bar: 'bg-red-500',     dot: 'bg-red-500',     soft: 'bg-red-50' },
  emerald: { bar: 'bg-emerald-500', dot: 'bg-emerald-500', soft: 'bg-emerald-50' },
  amber:   { bar: 'bg-amber-500',   dot: 'bg-amber-500',   soft: 'bg-amber-50' },
  slate:   { bar: 'bg-slate-400',   dot: 'bg-slate-400',   soft: 'bg-slate-50' }
};

const StatCard = ({ label, value, tone = 'blue' }) => {
  const t = TONES[tone] || TONES.blue;
  return (
    <div className="card !p-0 overflow-hidden">
      <div className="flex">
        <div className={`w-1 ${t.bar}`} aria-hidden="true" />
        <div className="flex-1 p-4 md:p-5">
          <div className="flex items-center gap-2">
            <span className={`inline-block w-1.5 h-1.5 rounded-full ${t.dot}`} aria-hidden="true" />
            <span className="text-[11px] font-semibold text-muted uppercase tracking-wider">{label}</span>
          </div>
          <div className="text-2xl md:text-3xl font-bold mt-2 text-slate-900 tabular-nums tracking-tight">
            {value}
          </div>
        </div>
      </div>
    </div>
  );
};

export default StatCard;
