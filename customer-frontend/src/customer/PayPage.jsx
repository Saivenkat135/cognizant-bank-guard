import { useState, useEffect } from "react";
import { useAuth } from "../auth/AuthContext";

async function getPublicIP() {
  try {
    const res = await fetch("https://api.ipify.org?format=json");
    const data = await res.json();
    return data.ip;
  } catch {
    return "0.0.0.0";
  }
}

async function getLocationFromIP(ip) {
  try {
    const res = await fetch(`https://ipapi.co/${ip}/json/`);
    const data = await res.json();
    return { city: data.city || "Unknown", state: data.region || "Unknown" };
  } catch {
    return { city: "Unknown", state: "Unknown" };
  }
}

export default function PayPage() {
  const { customerId } = useAuth();

  const [receiverAccount, setReceiverAccount] = useState("");
  const [amount, setAmount] = useState("");
  const [status, setStatus] = useState(null);
  const [errorMsg, setErrorMsg] = useState("");
  const [result, setResult] = useState(null);
  const [meta, setMeta] = useState({ ip: null, city: null, state: null, loading: true });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const ip = await getPublicIP();
      const loc = await getLocationFromIP(ip);
      if (!cancelled) setMeta({ ip, city: loc.city, state: loc.state, loading: false });
    })();
    return () => { cancelled = true; };
  }, []);

  const isValid = receiverAccount.trim().length > 4 && Number(amount) > 0;

  const handlePay = async () => {
    if (!isValid) return;
    setStatus("loading");
    setErrorMsg("");
    setResult(null);

    try {
      let { ip, city, state } = meta;
      if (!ip) {
        ip = await getPublicIP();
        const loc = await getLocationFromIP(ip);
        city = loc.city; state = loc.state;
        setMeta({ ip, city, state, loading: false });
      }

      const payload = {
        amount: Number(amount),
        city, state,
        ipAddress: ip,
        receiverAccountNumber: receiverAccount.trim(),
        customerId: customerId || 1,
      };

      const res = await fetch("http://localhost:8089/api/transactions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const data = await res.json();

      if (data.status === "ERROR") {
        const msg = data.errorCode === "RECEIVER_NOT_FOUND"
          ? `Receiver account not found: ${data.receiverAccountNumber}`
          : data.message || "Transaction failed.";
        setErrorMsg(msg); setStatus("error");
        return;
      }

      const txnStatus = data.transactionStatus || data.transaction?.status;
      if (txnStatus && txnStatus.toLowerCase() === "terminated") {
        setErrorMsg("Transaction terminated. Check your email for details.");
        setStatus("error");
        return;
      }

      if (!res.ok) {
        setErrorMsg(`Server error: ${res.status}`); setStatus("error");
        return;
      }

      setResult(data); setStatus("success");
      setReceiverAccount(""); setAmount("");
    } catch (err) {
      setErrorMsg(err.message || "Something went wrong. Please try again.");
      setStatus("error");
    }
  };

  return (
    <div className="w-full max-w-2xl mx-auto">
      <div className="relative bg-[#101627] border border-slate-800/70 rounded-2xl p-7 overflow-hidden">
        <div className="absolute -top-20 -right-20 w-60 h-60 rounded-full pointer-events-none"
             style={{ background: "radial-gradient(circle, rgba(59,130,246,0.16) 0%, transparent 70%)" }} />

        {/* Header */}
        <div className="relative flex items-center gap-4 mb-7">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-blue-700 text-white flex items-center justify-center shadow-lg shadow-blue-500/30">
            <SendIcon />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-slate-100">Send Money</h2>
            <p className="text-xs text-slate-500 mt-0.5">Transfer funds securely with automatic fraud check</p>
          </div>
        </div>

        {/* Fields */}
        <div className="relative flex flex-col gap-4">
          <Field
            label="Receiver Account Number"
            placeholder="e.g. BOI2605131430-47"
            value={receiverAccount}
            onChange={setReceiverAccount}
            icon={<AccountIcon />}
            mono
          />
          <Field
            label="Amount (₹)"
            placeholder="0.00"
            value={amount}
            onChange={setAmount}
            type="number"
            icon={<RupeeIcon />}
          />

          {/* Auto-detected info */}
          <div className="grid grid-cols-2 gap-3 bg-blue-500/[0.04] border border-blue-500/20 rounded-xl p-3.5">
            <DetectChip icon={<PinIcon />} label="Location"
              value={meta.loading ? "Detecting…" : `${meta.city || "Unknown"}, ${meta.state || "Unknown"}`}
              loading={meta.loading} />
            <DetectChip icon={<GlobeIcon />} label="IP Address"
              value={meta.loading ? "Detecting…" : (meta.ip || "—")}
              loading={meta.loading} mono />
          </div>

          <div className="text-[11px] text-slate-500 flex items-center gap-1.5">
            <span className="text-slate-600">●</span>
            Sending as Customer ID <span className="text-blue-300 font-semibold font-mono-display">#{customerId || 1}</span>
          </div>

          {/* Submit */}
          <button
            onClick={handlePay}
            disabled={!isValid || status === "loading"}
            className={`mt-1 w-full py-3.5 rounded-xl text-sm font-semibold text-white transition-all
              bg-gradient-to-br from-blue-500 to-blue-700 shadow-lg shadow-blue-500/25
              ${!isValid || status === "loading" ? "opacity-50 cursor-not-allowed" : "hover:from-blue-400 hover:to-blue-600 active:scale-[0.99]"}`}
          >
            {status === "loading" ? (
              <span className="flex items-center justify-center gap-2">
                <span className="w-4 h-4 rounded-full border-2 border-white/40 border-t-white animate-spin" />
                Processing…
              </span>
            ) : (
              <span className="flex items-center justify-center gap-2">
                <SendIcon size={15} /> Send Money
              </span>
            )}
          </button>

          {/* Error */}
          {status === "error" && errorMsg && (
            <div className="bg-rose-500/10 border border-rose-500/30 rounded-xl px-4 py-3 flex items-start gap-3">
              <div className="w-7 h-7 rounded-full bg-rose-500/15 border border-rose-500/30 text-rose-400 flex items-center justify-center text-sm font-bold shrink-0">!</div>
              <p className="text-sm text-rose-300 leading-relaxed">{errorMsg}</p>
            </div>
          )}

          {/* Success */}
          {status === "success" && result && (
            <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-5 flex flex-col gap-4">
              <div className="flex items-start gap-3">
                <div className="w-9 h-9 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 flex items-center justify-center font-bold shrink-0">✓</div>
                <div className="flex-1">
                  <p className="text-sm font-semibold text-emerald-300">Transaction Successful</p>
                  <p className="text-xs text-slate-300 mt-1">
                    ₹{result.transaction?.amount?.toLocaleString("en-IN")} sent to{" "}
                    <span className="font-mono-display text-blue-300">{result.transaction?.receiverAccountNumber}</span>
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 pt-3 border-t border-emerald-500/20">
                <ResultItem label="New Balance" value={`₹${result.senderNewBalance?.toLocaleString("en-IN")}`} accent="text-blue-300" />
                <ResultItem label="Status"      value={result.transactionStatus} accent="text-emerald-300" />
                <ResultItem label="Transaction ID" value={`#${result.transactionId}`} accent="text-slate-200" mono />
                <ResultItem label="Risk Score"  value={`${Math.round(result.riskScore || 0)}%`} accent="text-amber-300" />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Field({ label, placeholder, value, onChange, type = "text", icon, mono }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">{label}</label>
      <div className="relative flex items-center bg-[#0a0f1c] border border-slate-800 rounded-lg px-3 transition-colors focus-within:border-blue-500/60 focus-within:ring-2 focus-within:ring-blue-500/15">
        <span className="text-slate-500 mr-2.5">{icon}</span>
        <input
          type={type}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className={`flex-1 bg-transparent text-slate-100 placeholder:text-slate-600 text-sm py-2.5 outline-none ${mono ? "font-mono-display" : ""}`}
        />
      </div>
    </div>
  );
}

function DetectChip({ icon, label, value, loading, mono }) {
  return (
    <div className="flex items-center gap-2.5">
      <div className="w-8 h-8 rounded-lg bg-blue-500/15 border border-blue-500/25 text-blue-300 flex items-center justify-center shrink-0">
        {icon}
      </div>
      <div className="flex flex-col overflow-hidden">
        <span className="text-[10px] uppercase tracking-widest text-slate-500">{label}</span>
        <span className={`text-xs ${loading ? "text-slate-500" : "text-slate-200"} truncate ${mono ? "font-mono-display" : ""}`}>
          {value}
        </span>
      </div>
    </div>
  );
}

function ResultItem({ label, value, accent = "text-slate-200", mono }) {
  return (
    <div className="flex flex-col">
      <span className="text-[10px] uppercase tracking-widest text-slate-500">{label}</span>
      <span className={`text-sm font-semibold ${accent} ${mono ? "font-mono-display" : ""}`}>{value}</span>
    </div>
  );
}

function SendIcon({ size = 18 }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" /></svg>;
}
function AccountIcon() { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><rect x="2" y="5" width="20" height="14" rx="2" /><line x1="2" y1="10" x2="22" y2="10" /></svg>; }
function RupeeIcon()   { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><path d="M6 4h12M6 9h12M9 4c4 0 4 5 0 5M9 9l8 11" /></svg>; }
function PinIcon()     { return <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" /><circle cx="12" cy="9" r="2.5" /></svg>; }
function GlobeIcon()   { return <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="12" cy="12" r="10" /><path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20" /></svg>; }
