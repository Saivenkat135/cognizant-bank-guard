import { useEffect, useState } from "react";
import { LoadingState, ErrorState } from "./ProfilePage";

const SEVERITY_TONES = {
  HIGH:   { pill: "bg-rose-500/10 text-rose-300 border-rose-500/30",     dot: "bg-rose-400",   label: "High" },
  MEDIUM: { pill: "bg-amber-500/10 text-amber-300 border-amber-500/30",  dot: "bg-amber-400",  label: "Medium" },
  LOW:    { pill: "bg-yellow-500/10 text-yellow-300 border-yellow-500/30", dot: "bg-yellow-400", label: "Low" },
};

const DECISION_TONES = {
  flagged:    { pill: "bg-amber-500/10 text-amber-300 border-amber-500/30", label: "Flagged" },
  terminated: { pill: "bg-rose-500/10 text-rose-300 border-rose-500/30",    label: "Terminated" },
};

export default function MessagesPage({ customerId }) {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchAlerts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [customerId]);

  const fetchAlerts = async () => {
    if (!customerId) {
      setAlerts([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`http://localhost:8085/api/investigation/alerts/customer/${customerId}`);
      if (!res.ok) throw new Error(`Server error: ${res.status}`);
      const data = await res.json();
      setAlerts(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load messages.");
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingState label="Fetching your messages…" />;
  if (error)   return <ErrorState message={error} onRetry={fetchAlerts} />;

  return (
    <div className="w-full max-w-4xl mx-auto flex flex-col gap-5">
      <div className="bg-[#101627] border border-slate-800/70 rounded-2xl overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-800/70 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold text-slate-200">Security Alerts</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              {alerts.length} alert{alerts.length === 1 ? "" : "s"} on your account
            </p>
          </div>
          <button
            onClick={fetchAlerts}
            className="flex items-center gap-1.5 text-xs text-blue-400 hover:text-blue-300 transition-colors font-medium"
          >
            <RefreshIcon /> Refresh
          </button>
        </div>

        {alerts.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 gap-2">
            <div className="w-14 h-14 rounded-full bg-slate-800/60 flex items-center justify-center text-2xl">📭</div>
            <p className="text-sm text-slate-400 mt-2">No security alerts</p>
            <p className="text-xs text-slate-600">Your account looks clean — nothing to worry about.</p>
          </div>
        ) : (
          <ul className="divide-y divide-slate-800/60">
            {alerts.map((a, i) => (
              <AlertRow key={a.alertId ?? i} alert={a} index={i} />
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function AlertRow({ alert, index }) {
  const sev = SEVERITY_TONES[alert.severity] || {
    pill: "bg-slate-700/40 text-slate-300 border-slate-600/30",
    dot:  "bg-slate-400",
    label: alert.severity || "Info",
  };
  const dec = DECISION_TONES[alert.decisionStatus] || {
    pill: "bg-slate-700/40 text-slate-300 border-slate-600/30",
    label: alert.decisionStatus || "—",
  };
  const riskPct = Math.round(alert.riskScore || 0);

  return (
    <li
      className="px-5 py-4 hover:bg-blue-500/[0.03] transition-colors"
      style={{ animation: `bg-fade-up 0.3s ease both ${index * 0.04}s`, opacity: 0 }}
    >
      <div className="flex items-start gap-4">
        <div className={`mt-1 w-2.5 h-2.5 rounded-full shrink-0 ${sev.dot} shadow-[0_0_8px_currentColor]`} />
        <div className="flex-1 min-w-0">
          <div className="flex items-center flex-wrap gap-2 mb-1">
            <span className={`text-[10.5px] font-semibold px-2 py-0.5 rounded-full border whitespace-nowrap ${sev.pill}`}>
              {sev.label} severity
            </span>
            <span className={`text-[10.5px] font-semibold px-2 py-0.5 rounded-full border whitespace-nowrap ${dec.pill}`}>
              {dec.label}
            </span>
            <span className="text-[10.5px] text-slate-500 font-mono-display">#{alert.alertId}</span>
            <span className="text-[10.5px] text-slate-500 ml-auto whitespace-nowrap">
              {formatDate(alert.createdAt)}
            </span>
          </div>
          <p className="text-sm text-slate-200 leading-relaxed">
            {alert.reason || "A transaction on your account triggered our fraud-detection system."}
          </p>
          <div className="flex items-center gap-2 mt-2">
            <div className="w-24 h-1.5 bg-slate-800 rounded-full overflow-hidden">
              <div
                className={
                  riskPct < 30 ? "h-full bg-emerald-400" :
                  riskPct < 60 ? "h-full bg-yellow-400" :
                  riskPct < 80 ? "h-full bg-amber-400" :
                                 "h-full bg-rose-400"
                }
                style={{ width: `${riskPct}%` }}
              />
            </div>
            <span className="text-[11px] font-semibold text-slate-400">Risk {riskPct}%</span>
          </div>
        </div>
      </div>
    </li>
  );
}

function formatDate(iso) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("en-IN", {
      day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit",
    });
  } catch { return iso; }
}

function RefreshIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="23 4 23 10 17 10" />
      <polyline points="1 20 1 14 7 14" />
      <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
    </svg>
  );
}
