import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "./AuthContext";
import Brand from "../components/Brand";

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [status, setStatus] = useState(null);
  const [errorMsg, setErrorMsg] = useState("");

  const isValid = email.trim().includes("@") && password.length > 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isValid) return;
    setStatus("loading");
    setErrorMsg("");
    try {
      await login(email.trim(), password);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setErrorMsg(err.message || "Login failed.");
      setStatus("error");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-10 relative overflow-hidden">
      <BackgroundGrid />

      <div className="relative w-full max-w-md bg-fade-up">
        <div className="flex justify-center mb-8">
          <Brand size="lg" />
        </div>

        <div className="relative bg-[#101627] border border-slate-800/80 rounded-2xl p-8 shadow-2xl shadow-black/30">
          <h1 className="text-2xl font-semibold text-slate-100">Welcome back</h1>
          <p className="text-sm text-slate-400 mt-1.5">Sign in to your Bankguard Pay account</p>

          <form onSubmit={handleSubmit} className="mt-7 flex flex-col gap-4">
            <Input
              label="Email"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={setEmail}
              autoComplete="email"
              icon={<MailIcon />}
            />
            <Input
              label="Password"
              type={showPwd ? "text" : "password"}
              placeholder="Enter your password"
              value={password}
              onChange={setPassword}
              autoComplete="current-password"
              icon={<LockIcon />}
              suffix={
                <button
                  type="button"
                  onClick={() => setShowPwd((v) => !v)}
                  className="text-slate-500 hover:text-blue-400 transition-colors"
                  tabIndex={-1}
                >
                  {showPwd ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              }
            />

            {status === "error" && errorMsg && (
              <ErrorBanner message={errorMsg} />
            )}

            <PrimaryButton type="submit" disabled={!isValid || status === "loading"} loading={status === "loading"}>
              {status === "loading" ? "Signing in…" : "Sign In"}
            </PrimaryButton>
          </form>

          <p className="text-sm text-slate-400 text-center mt-6">
            New to Bankguard Pay?{" "}
            <Link to="/signup" className="text-blue-400 font-semibold hover:text-blue-300 transition-colors">
              Create an account
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

/* ─────────────── shared UI ─────────────── */

export function Input({ label, type = "text", placeholder, value, onChange, autoComplete, icon, suffix }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">{label}</label>
      <div className="relative flex items-center bg-[#0a0f1c] border border-slate-800 rounded-lg px-3 transition-colors focus-within:border-blue-500/60 focus-within:ring-2 focus-within:ring-blue-500/15">
        {icon && <span className="text-slate-500 mr-2.5">{icon}</span>}
        <input
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          autoComplete={autoComplete}
          className="flex-1 bg-transparent text-slate-100 placeholder:text-slate-600 text-sm py-2.5 outline-none"
        />
        {suffix && <span className="ml-2 flex items-center">{suffix}</span>}
      </div>
    </div>
  );
}

export function PrimaryButton({ children, disabled, loading, ...props }) {
  return (
    <button
      {...props}
      disabled={disabled}
      className={`relative w-full py-3 rounded-lg text-sm font-semibold tracking-wide text-white transition-all
        bg-gradient-to-br from-blue-500 to-blue-700 shadow-lg shadow-blue-500/20
        ${disabled ? "opacity-50 cursor-not-allowed" : "hover:from-blue-400 hover:to-blue-600 active:scale-[0.99]"}`}
    >
      {loading ? (
        <span className="flex items-center justify-center gap-2">
          <span className="w-4 h-4 rounded-full border-2 border-white/40 border-t-white animate-spin" />
          {children}
        </span>
      ) : children}
    </button>
  );
}

export function ErrorBanner({ message }) {
  return (
    <div className="flex items-start gap-2.5 bg-rose-500/10 border border-rose-500/30 rounded-lg px-3 py-2.5">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f43f5e" strokeWidth="2" strokeLinecap="round" className="shrink-0 mt-0.5">
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
      <span className="text-sm text-rose-300 leading-relaxed">{message}</span>
    </div>
  );
}

export function BackgroundGrid() {
  return (
    <>
      <div className="absolute inset-0 pointer-events-none"
        style={{
          backgroundImage: "linear-gradient(to right, rgba(96,165,250,0.06) 1px, transparent 1px), linear-gradient(to bottom, rgba(96,165,250,0.06) 1px, transparent 1px)",
          backgroundSize: "44px 44px",
          maskImage: "radial-gradient(ellipse at center, black 35%, transparent 75%)",
          WebkitMaskImage: "radial-gradient(ellipse at center, black 35%, transparent 75%)",
        }} />
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[520px] h-[520px] rounded-full pointer-events-none"
        style={{ background: "radial-gradient(circle, rgba(59,130,246,0.10) 0%, transparent 70%)" }} />
    </>
  );
}

function MailIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <polyline points="3 7 12 13 21 7" />
    </svg>
  );
}
function LockIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
      <rect x="4" y="11" width="16" height="10" rx="2" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </svg>
  );
}
function EyeIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}
function EyeOffIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
      <path d="M9.9 5.13A9.93 9.93 0 0 1 12 5c7 0 11 7 11 7a18.5 18.5 0 0 1-2.16 3.19" />
      <path d="M14.12 14.12A3 3 0 1 1 9.88 9.88" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  );
}
