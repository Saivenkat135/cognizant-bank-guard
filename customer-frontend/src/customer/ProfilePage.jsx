import { useState, useEffect } from "react";

export default function ProfilePage({ customerId }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const effectiveId = customerId || 1;

  useEffect(() => {
    fetchProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [effectiveId]);

  const fetchProfile = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`http://localhost:8089/api/customers/${effectiveId}`);
      if (!res.ok) throw new Error(`Server error: ${res.status}`);
      const data = await res.json();
      setProfile(data);
    } catch (err) {
      setError(err.message || "Failed to load profile.");
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingState label="Loading your profile…" />;
  if (error)   return <ErrorState message={error} onRetry={fetchProfile} />;
  if (!profile) return null;

  // Risk score is already a 0–100 value stored on the customer row.
  const riskPct = Math.round(profile.riskscore || 0);
  const riskTone =
    riskPct < 30 ? { text: "text-emerald-400", bar: "bg-emerald-400", chip: "Low Risk" } :
    riskPct < 60 ? { text: "text-yellow-400",  bar: "bg-yellow-400",  chip: "Moderate" } :
    riskPct < 80 ? { text: "text-amber-400",   bar: "bg-amber-400",   chip: "Elevated" } :
                   { text: "text-rose-400",    bar: "bg-rose-400",    chip: "High Risk" };

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col gap-5">
      {/* Hero */}
      <div className="relative bg-gradient-to-br from-[#101627] to-[#0d1322] border border-slate-800/70 rounded-2xl p-7 overflow-hidden">
        <div className="absolute -top-24 -right-24 w-72 h-72 rounded-full pointer-events-none"
             style={{ background: "radial-gradient(circle, rgba(59,130,246,0.18) 0%, transparent 70%)" }} />

        <div className="relative flex flex-wrap items-center justify-between gap-6">
          <div className="flex items-center gap-5">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-blue-500 to-blue-700 text-white flex items-center justify-center text-2xl font-semibold shadow-lg shadow-blue-500/30">
              {profile.name ? profile.name[0].toUpperCase() : "U"}
            </div>
            <div className="flex flex-col gap-1">
              <h2 className="text-xl font-semibold text-slate-100">{profile.name}</h2>
              <p className="text-sm text-slate-400">{profile.email}</p>
              <div className="flex items-center gap-2 mt-1">
                <Pill tone="blue">{profile.bankName}</Pill>
                <Pill tone="slate">{profile.accountType}</Pill>
              </div>
            </div>
          </div>

          <div className="text-right">
            <p className="text-[11px] text-slate-500 uppercase tracking-widest mb-1">Available Balance</p>
            <p className="text-3xl font-bold text-slate-100">
              ₹{profile.balance?.toLocaleString("en-IN") || "0"}
            </p>
          </div>
        </div>
      </div>

      {/* Detail grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <DetailCard icon={<BankIcon />} label="Bank" value={profile.bankName} />
        <DetailCard icon={<AccountIcon />} label="Account No." value={profile.accountNo} mono />
        <DetailCard icon={<TypeIcon />} label="Type" value={profile.accountType} />
        <DetailCard icon={<MailIcon />} label="Email" value={profile.email} small />
      </div>

      
      
    </div>
  );
}

function DetailCard({ icon, label, value, mono, small }) {
  return (
    <div className="bg-[#101627] border border-slate-800/70 rounded-xl p-4 flex items-center gap-3 hover:border-blue-500/30 transition-colors">
      <div className="w-10 h-10 rounded-lg bg-blue-500/10 text-blue-400 flex items-center justify-center shrink-0">
        {icon}
      </div>
      <div className="flex flex-col overflow-hidden">
        <span className="text-[10px] text-slate-500 uppercase tracking-widest">{label}</span>
        <span className={`text-slate-200 font-medium truncate ${mono ? "font-mono-display text-[13px]" : small ? "text-[13px]" : "text-sm"}`}>
          {value}
        </span>
      </div>
    </div>
  );
}

function Pill({ children, tone = "blue" }) {
  const tones = {
    blue:  "bg-blue-500/10 text-blue-300 border-blue-500/30",
    slate: "bg-slate-800/60 text-slate-300 border-slate-700",
  };
  return (
    <span className={`text-[10.5px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full border ${tones[tone]}`}>
      {children}
    </span>
  );
}

export function LoadingState({ label = "Loading…" }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 min-h-[300px]">
      <div className="w-9 h-9 rounded-full border-2 border-slate-700 border-t-blue-400 animate-spin" />
      <p className="text-sm text-slate-400">{label}</p>
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 min-h-[300px]">
      <div className="w-12 h-12 rounded-full bg-rose-500/10 border border-rose-500/30 text-rose-400 flex items-center justify-center text-lg font-bold">!</div>
      <p className="text-sm text-rose-300">{message}</p>
      <button onClick={onRetry}
              className="px-5 py-2 rounded-lg text-sm font-semibold text-white bg-gradient-to-br from-blue-500 to-blue-700 hover:from-blue-400 hover:to-blue-600 transition-all shadow-md shadow-blue-500/20">
        Retry
      </button>
    </div>
  );
}

function BankIcon()    { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><path d="M3 10l9-6 9 6" /><path d="M5 10v10M19 10v10M9 10v10M15 10v10" /><path d="M2 20h20" /></svg>; }
function AccountIcon() { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><rect x="2" y="5" width="20" height="14" rx="2" /><line x1="2" y1="10" x2="22" y2="10" /></svg>; }
function TypeIcon()    { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><rect x="2" y="7" width="20" height="10" rx="2" /><circle cx="12" cy="12" r="2.5" /></svg>; }
function MailIcon()    { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><rect x="3" y="5" width="18" height="14" rx="2" /><polyline points="3 7 12 13 21 7" /></svg>; }
function ShieldIcon()  { return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /></svg>; }
