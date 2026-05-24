/**
 * Standard page header.
 *   - Thin royal-blue accent bar on the left of the title for visual rhythm.
 *   - Optional `children` slot for action buttons.
 */
const PageHeader = ({ title, subtitle, children }) => (
  <div className="flex items-start justify-between gap-4 mb-6">
    <div className="flex gap-3">
      <span className="block w-1 rounded-full bg-[var(--primary)] shrink-0" aria-hidden="true" />
      <div>
        <h1 className="text-xl md:text-2xl font-semibold text-slate-900 tracking-tight">{title}</h1>
        {subtitle && <p className="text-sm text-muted mt-1">{subtitle}</p>}
      </div>
    </div>
    {children && <div className="flex gap-2 shrink-0">{children}</div>}
  </div>
);

export default PageHeader;
