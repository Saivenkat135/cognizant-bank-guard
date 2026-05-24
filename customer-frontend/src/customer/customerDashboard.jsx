import { useState } from "react";
import { useNavigate } from "react-router-dom";
import ProfilePage from "./ProfilePage";
import TransactionsPage from "./TransactionsPage";
import PayPage from "./PayPage";
import MessagesPage from "./MessagesPage";
import { useAuth } from "../auth/AuthContext";
import Brand from "../components/Brand";

const NAV_ITEMS = [
  { id: "profile",      label: "Profile",      icon: <ProfileIcon /> },
  { id: "transactions", label: "Transactions", icon: <ActivityIcon /> },
  { id: "pay",          label: "Send Money",   icon: <SendIcon /> },
  { id: "messages",     label: "Messages",     icon: <MessageIcon /> },
];

const PAGE_META = {
  profile:      { title: "Profile",      subtitle: "Your account overview" },
  transactions: { title: "Transactions", subtitle: "Your recent activity" },
  pay:          { title: "Send Money",   subtitle: "Transfer securely with fraud protection" },
  messages:     { title: "Messages",     subtitle: "Security alerts on your account" },
};

export default function CustomerDashboard() {
  const { email, customerId, logout } = useAuth();
  const navigate = useNavigate();
  const [activePage, setActivePage] = useState("profile");

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  const renderPage = () => {
    switch (activePage) {
      case "profile":      return <ProfilePage email={email} customerId={customerId} />;
      case "transactions": return <TransactionsPage email={email} customerId={customerId} />;
      case "pay":          return <PayPage customerId={customerId} />;
      case "messages":     return <MessagesPage customerId={customerId} />;
      default:             return <ProfilePage email={email} customerId={customerId} />;
    }
  };

  return (
    <div className="flex h-screen bg-[#0a0f1c] overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 shrink-0 bg-[#0d1322] border-r border-slate-800/70 flex flex-col">
        <div className="px-5 py-5 border-b border-slate-800/70">
          <Brand size="md" />
        </div>

        <nav className="flex-1 px-3 py-4 flex flex-col gap-1">
          {NAV_ITEMS.map((item) => {
            const active = activePage === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActivePage(item.id)}
                className={`flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all
                  ${active
                    ? "bg-blue-500/15 text-blue-300 border border-blue-500/25 shadow-sm shadow-blue-500/10"
                    : "text-slate-400 border border-transparent hover:bg-slate-800/40 hover:text-slate-200"}`}
              >
                <span className={active ? "text-blue-400" : "text-slate-500"}>{item.icon}</span>
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        <div className="p-3 border-t border-slate-800/70">
          <div className="flex items-center gap-3 px-2 py-2">
            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-500 to-blue-700 text-white flex items-center justify-center font-semibold text-sm shrink-0">
              {email ? email[0].toUpperCase() : "U"}
            </div>
            <div className="flex flex-col overflow-hidden flex-1">
              <span className="text-xs text-slate-200 font-medium truncate">{email || "user@bank.com"}</span>
              <span className="text-[10px] text-slate-500 uppercase tracking-widest">Customer</span>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="mt-2 w-full flex items-center justify-center gap-2 py-2 rounded-lg text-xs font-semibold text-slate-300 bg-slate-800/40 border border-slate-800 hover:bg-rose-500/10 hover:text-rose-400 hover:border-rose-500/30 transition-all"
          >
            <LogoutIcon />
            Sign out
          </button>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 flex flex-col overflow-hidden">
        <header className="flex items-center justify-between px-8 py-5 border-b border-slate-800/70 shrink-0">
          <div>
            <h1 className="text-xl font-semibold text-slate-100">{PAGE_META[activePage].title}</h1>
            <p className="text-xs text-slate-500 mt-0.5">{PAGE_META[activePage].subtitle}</p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/25">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.7)]" />
            <span className="text-[11px] font-medium text-emerald-300">Secure Session</span>
          </div>
        </header>

        <div className="flex-1 overflow-auto p-8">
          <div className="bg-fade-up">{renderPage()}</div>
        </div>
      </main>
    </div>
  );
}

function ProfileIcon() {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><circle cx="12" cy="8" r="4" /><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" /></svg>;
}
function ActivityIcon() {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12" /></svg>;
}
function SendIcon() {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" /></svg>;
}
function MessageIcon() {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>;
}
function LogoutIcon() {
  return <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" /></svg>;
}
