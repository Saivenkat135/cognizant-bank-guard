import { useState, useEffect } from "react";
import { LoadingState, ErrorState } from "./ProfilePage";

const STATUS_TONES = {
  completed:  { pill: "bg-emerald-500/10 text-emerald-300 border-emerald-500/30", label: "Completed" },
  pending:    { pill: "bg-yellow-500/10 text-yellow-300 border-yellow-500/30",   label: "Pending" },
  flagged:    { pill: "bg-amber-500/10 text-amber-300 border-amber-500/30",      label: "Flagged" },
  terminated: { pill: "bg-rose-500/10 text-rose-300 border-rose-500/30",         label: "Terminated" },
};

export default function TransactionsPage({ customerId }) {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchTransactions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [customerId]);

  const fetchTransactions = async () => {
    setLoading(true);
    setError(null);
    try {
      const url = customerId
        ? `http://localhost:8089/api/transactions/customer/${customerId}`
        : "http://localhost:8089/api/transactions";
      const res = await fetch(url);
      if (!res.ok) throw new Error(`Server error: ${res.status}`);
      const data = await res.json();
      setTransactions(Array.isArray(data) ? data : data.transactions || []);
    } catch (err) {
      setError(err.message || "Failed to load transactions.");
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingState label="Fetching your transactions…" />;
  if (error)   return <ErrorState message={error} onRetry={fetchTransactions} />;

  const totalCount = transactions.length;
  const totalVolume = transactions.reduce((s, t) => s + (t.amount || 0), 0);
  const avgAmount = totalCount ? totalVolume / totalCount : 0;
  // Risk scores are already stored on a 0–100 scale, no extra *100 needed.
  const avgRiskPct = totalCount
    ? Math.round(transactions.reduce((s, t) => s + (t.riskScore || 0), 0) / totalCount)
    : 0;
  const riskTone =
    avgRiskPct < 30 ? "emerald" :
    avgRiskPct < 60 ? "amber"   :
                      "rose";
  const fmt = (v) => `₹${Math.round(v).toLocaleString("en-IN")}`;

  return (
    <div className="w-full max-w-6xl mx-auto flex flex-col gap-5">
      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={<CountIcon />}  tone="blue"    label="Total Transactions" value={totalCount} />
        <StatCard icon={<VolumeIcon />} tone="emerald" label="Total Volume"       value={fmt(totalVolume)} />
        <StatCard icon={<AvgIcon />}    tone="amber"   label="Avg. Amount"        value={fmt(avgAmount)} />
        <StatCard icon={<RiskIcon />}   tone={riskTone} label="Avg. Risk Score"   value={`${avgRiskPct}%`} />
      </div>

      {/* Table */}
      <div className="bg-[#101627] border border-slate-800/70 rounded-2xl overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-800/70 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold text-slate-200">Transaction History</h3>
            <p className="text-xs text-slate-500 mt-0.5">{transactions.length} total transactions</p>
          </div>
          <button onClick={fetchTransactions} className="flex items-center gap-1.5 text-xs text-blue-400 hover:text-blue-300 transition-colors font-medium">
            <RefreshIcon /> Refresh
          </button>
        </div>

        {transactions.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 gap-2">
            <div className="w-14 h-14 rounded-full bg-slate-800/60 flex items-center justify-center text-2xl">📭</div>
            <p className="text-sm text-slate-400 mt-2">No transactions yet</p>
            <p className="text-xs text-slate-600">Start by sending money from the Pay tab</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-[#0a0f1c] text-left">
                  <Th>#ID</Th>
                  <Th>Receiver</Th>
                  <Th>Amount</Th>
                  <Th>Location</Th>
                  <Th>IP</Th>
                  <Th>Date</Th>
                  <Th>Status</Th>
                  <Th>Risk</Th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((t, i) => (
                  <Row key={t.transactionId ?? i} txn={t} index={i} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function StatCard({ icon, label, value, tone }) {
  const tones = {
    blue:    "from-blue-500/10 border-blue-500/25 text-blue-300",
    emerald: "from-emerald-500/10 border-emerald-500/25 text-emerald-300",
    amber:   "from-amber-500/10 border-amber-500/25 text-amber-300",
    rose:    "from-rose-500/10 border-rose-500/25 text-rose-300",
  };
  return (
    <div className={`bg-gradient-to-br ${tones[tone]} to-[#101627] border rounded-xl p-4 flex items-center gap-3.5`}>
      <div className="w-10 h-10 rounded-lg bg-slate-900/40 flex items-center justify-center shrink-0">{icon}</div>
      <div className="flex flex-col overflow-hidden">
        <span className="text-[10px] uppercase tracking-widest text-slate-500">{label}</span>
        <span className="text-lg font-bold truncate">{value}</span>
      </div>
    </div>
  );
}

function CountIcon()  { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="8" y1="13" x2="16" y2="13" /><line x1="8" y1="17" x2="13" y2="17" /></svg>; }
function VolumeIcon() { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M3 17l6-6 4 4 8-8" /><polyline points="14 7 21 7 21 14" /></svg>; }
function AvgIcon()    { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M4 19h16M4 5h16M6 12h12" /></svg>; }
function RiskIcon()   { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /></svg>; }

function Th({ children }) {
  return (
    <th className="px-5 py-3 text-[10.5px] uppercase tracking-widest font-semibold text-slate-500 whitespace-nowrap">
      {children}
    </th>
  );
}

function Row({ txn, index }) {
  const tone = STATUS_TONES[txn.status] || {
    pill: "bg-slate-700/40 text-slate-300 border-slate-600/30",
    label: txn.status || "—",
  };
  // Risk score is already a 0–100 value coming back from the gateway.
  const riskPct = Math.round(txn.riskScore || 0);
  const riskColor =
    riskPct < 30 ? "text-emerald-400 bg-emerald-400" :
    riskPct < 60 ? "text-yellow-400 bg-yellow-400" :
    riskPct < 80 ? "text-amber-400 bg-amber-400" :
                   "text-rose-400 bg-rose-400";
  const [riskText, riskBar] = riskColor.split(" ");

  return (
    <tr
      className="border-t border-slate-800/60 hover:bg-blue-500/[0.03] transition-colors"
      style={{ animation: `bg-fade-up 0.3s ease both ${index * 0.03}s`, opacity: 0 }}
    >
      <Td><span className="font-mono-display text-xs text-slate-500">#{txn.transactionId}</span></Td>
      <Td><span className="font-mono-display text-xs text-slate-200">{txn.receiverAccountNumber || "—"}</span></Td>
      <Td><span className="text-sm font-semibold text-rose-300">₹{txn.amount?.toLocaleString("en-IN") || "0"}</span></Td>
      <Td><span className="text-xs text-slate-400">{txn.city && txn.state ? `${txn.city}, ${txn.state}` : "—"}</span></Td>
      <Td><span className="font-mono-display text-[11px] text-slate-500">{txn.ipAddress || "—"}</span></Td>
      <Td><span className="text-xs text-slate-400 whitespace-nowrap">{formatDate(txn.time)}</span></Td>
      <Td>
        <span className={`text-[10.5px] font-semibold px-2 py-0.5 rounded-full border whitespace-nowrap ${tone.pill}`}>
          {tone.label}
        </span>
      </Td>
      <Td>
        <div className="flex items-center gap-2">
          <div className="w-10 h-1.5 bg-slate-800 rounded-full overflow-hidden">
            <div className={`h-full ${riskBar}`} style={{ width: `${riskPct}%` }} />
          </div>
          <span className={`text-[11px] font-semibold ${riskText}`}>{riskPct}%</span>
        </div>
      </Td>
    </tr>
  );
}

function Td({ children }) {
  return <td className="px-5 py-3.5 whitespace-nowrap">{children}</td>;
}

function formatDate(iso) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("en-IN", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
  } catch { return iso; }
}

function RefreshIcon() {
  return <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="23 4 23 10 17 10" /><polyline points="1 20 1 14 7 14" /><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" /></svg>;
}
