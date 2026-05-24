export default function Brand({ size = "md", subtitle = true }) {
  const sizes = {
    sm: { wrap: "gap-2",   box: "w-8 h-8 rounded-lg",   icon: 16, title: "text-sm",   sub: "text-[9px]"  },
    md: { wrap: "gap-2.5", box: "w-10 h-10 rounded-xl", icon: 20, title: "text-lg",   sub: "text-[10px]" },
    lg: { wrap: "gap-3",   box: "w-12 h-12 rounded-xl", icon: 24, title: "text-xl",   sub: "text-[11px]" },
  };
  const s = sizes[size] || sizes.md;

  return (
    <div className={`flex items-center ${s.wrap}`}>
      <div
        className={`${s.box} bg-gradient-to-br from-blue-500 to-blue-700 flex items-center justify-center shadow-md shadow-blue-500/30`}
      >
        <svg width={s.icon} height={s.icon} viewBox="0 0 24 24" fill="none"
             stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 2 L4 6 v6 c0 5 3.5 9.5 8 10 4.5 -0.5 8 -5 8 -10 V6 Z" />
          <path d="M9 12 l2 2 l4-4" />
        </svg>
      </div>
      <div className="flex flex-col leading-none">
        <span className={`${s.title} font-bold text-slate-100 tracking-tight`}>
          Bankguard <span className="text-blue-400">Pay</span>
        </span>
        {subtitle && (
          <span className={`${s.sub} text-slate-500 uppercase tracking-[0.18em] mt-1`}>
            Secure Banking
          </span>
        )}
      </div>
    </div>
  );
}
