import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "./AuthContext";
import Brand from "../components/Brand";
import { Input, PrimaryButton, ErrorBanner, BackgroundGrid } from "./LoginPage";

const BANK_OPTIONS = ["PNB", "SBI", "HDFC", "ICICI", "AXIS", "BOI", "KOTAK"];
const ACCOUNT_TYPES = ["Savings", "Current", "Fixed"];

function generateAccountNo(bank) {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  const yy = pad(now.getFullYear() % 100);
  const mm = pad(now.getMonth() + 1);
  const dd = pad(now.getDate());
  const hh = pad(now.getHours());
  const mi = pad(now.getMinutes());
  const rand = pad(Math.floor(Math.random() * 100));
  return `${bank}${yy}${mm}${dd}${hh}${mi}${rand}`;
}

export default function SignupPage() {
  const navigate = useNavigate();
  const { signup } = useAuth();

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    bankName: "PNB",
    accountType: "Savings",
    balance: "",
  });
  const [status, setStatus] = useState(null);
  const [errorMsg, setErrorMsg] = useState("");

  const setField = (key) => (val) => setForm((f) => ({ ...f, [key]: val }));

  const isValid =
    form.name.trim() &&
    form.email.trim().includes("@") &&
    form.password.length > 0 &&
    form.bankName &&
    form.accountType &&
    Number(form.balance) >= 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isValid) return;
    setStatus("loading");
    setErrorMsg("");
    try {
      const accountNo = generateAccountNo(form.bankName);
      await signup({
        bankName: form.bankName,
        balance: Number(form.balance),
        accountType: form.accountType,
        name: form.name.trim(),
        email: form.email.trim(),
        accountNo,
        password: form.password,
      });
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setErrorMsg(err.message || "Signup failed.");
      setStatus("error");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-10 relative overflow-hidden">
      <BackgroundGrid />

      <div className="relative w-full max-w-xl bg-fade-up">
        <div className="flex justify-center mb-8">
          <Brand size="lg" />
        </div>

        <div className="relative bg-[#101627] border border-slate-800/80 rounded-2xl p-8 shadow-2xl shadow-black/30">
          <h1 className="text-2xl font-semibold text-slate-100">Create your account</h1>
          <p className="text-sm text-slate-400 mt-1.5">Set up your bank profile in less than a minute</p>

          <form onSubmit={handleSubmit} className="mt-7 flex flex-col gap-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input label="Full Name" placeholder="Raj Singh" value={form.name} onChange={setField("name")} icon={<UserIcon />} />
              <Input label="Email" type="email" placeholder="you@example.com" value={form.email} onChange={setField("email")} icon={<MailIcon />} />
            </div>

            <Input label="Password" type="password" placeholder="Choose a password" value={form.password} onChange={setField("password")} icon={<LockIcon />} />

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Select label="Bank" value={form.bankName} onChange={setField("bankName")} options={BANK_OPTIONS} />
              <Select label="Account Type" value={form.accountType} onChange={setField("accountType")} options={ACCOUNT_TYPES} />
            </div>

            <Input label="Opening Balance (₹)" type="number" placeholder="80000" value={form.balance} onChange={setField("balance")} icon={<RupeeIcon />} />

            <div className="flex items-center gap-3 bg-blue-500/5 border border-dashed border-blue-500/30 rounded-lg px-3.5 py-3">
              <div className="w-9 h-9 rounded-lg bg-blue-500/15 border border-blue-500/25 flex items-center justify-center shrink-0">
                <SparkleIcon />
              </div>
              <div className="flex flex-col">
                <span className="text-[10.5px] uppercase tracking-widest text-slate-500 font-medium">
                  Account number generated automatically
                </span>
                <span className="font-mono-display text-sm text-blue-300 mt-0.5">
                  {form.bankName}YYMMDDHHMM##
                </span>
              </div>
            </div>

            {status === "error" && errorMsg && <ErrorBanner message={errorMsg} />}

            <PrimaryButton type="submit" disabled={!isValid || status === "loading"} loading={status === "loading"}>
              {status === "loading" ? "Creating account…" : "Create Account"}
            </PrimaryButton>
          </form>

          <p className="text-sm text-slate-400 text-center mt-6">
            Already have an account?{" "}
            <Link to="/login" className="text-blue-400 font-semibold hover:text-blue-300 transition-colors">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

function Select({ label, value, onChange, options }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">{label}</label>
      <div className="relative flex items-center bg-[#0a0f1c] border border-slate-800 rounded-lg px-3 transition-colors focus-within:border-blue-500/60 focus-within:ring-2 focus-within:ring-blue-500/15">
        <span className="text-slate-500 mr-2.5"><BankIcon /></span>
        <select
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="flex-1 bg-transparent text-slate-100 text-sm py-2.5 outline-none appearance-none cursor-pointer pr-6"
        >
          {options.map((opt) => (
            <option key={opt} value={opt} className="bg-[#101627] text-slate-100">{opt}</option>
          ))}
        </select>
        <span className="text-slate-500 pointer-events-none absolute right-3">
          <ChevronDownIcon />
        </span>
      </div>
    </div>
  );
}

function UserIcon() {
  return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><circle cx="12" cy="8" r="4" /><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" /></svg>;
}
function MailIcon() {
  return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><rect x="3" y="5" width="18" height="14" rx="2" /><polyline points="3 7 12 13 21 7" /></svg>;
}
function LockIcon() {
  return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><rect x="4" y="11" width="16" height="10" rx="2" /><path d="M8 11V8a4 4 0 0 1 8 0v3" /></svg>;
}
function RupeeIcon() {
  return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><path d="M6 4h12M6 9h12M9 4c4 0 4 5 0 5M9 9l8 11" /></svg>;
}
function BankIcon() {
  return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><path d="M3 10l9-6 9 6" /><path d="M5 10v10M19 10v10M9 10v10M15 10v10" /><path d="M2 20h20" /></svg>;
}
function SparkleIcon() {
  return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#60a5fa" strokeWidth="2" strokeLinecap="round"><path d="M12 2v6M12 16v6M2 12h6M16 12h6" /><path d="M5 5l3 3M16 16l3 3M5 19l3-3M16 8l3-3" /></svg>;
}
function ChevronDownIcon() {
  return <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><polyline points="6 9 12 15 18 9" /></svg>;
}
