/**
 * SVG pie chart for status breakdown. Each slice is one { label, value }.
 * Slice color is chosen by label (genuine/flagged/etc).
 */
const COLORS = {
  genuine:    '#10b981',
  flagged:    '#ef4444',
  pending:    '#eab308',
  terminated: '#64748b',
  unknown:    '#94a3b8'
};

const StatusPie = ({ data = [], size = 180 }) => {
  const total = data.reduce((acc, d) => acc + d.value, 0);
  if (total === 0) return <div className="text-sm text-muted">No data.</div>;

  // Build SVG arc paths
  let cumulative = 0;
  const cx = size / 2;
  const cy = size / 2;
  const r = size / 2 - 4;

  const slices = data.map((d) => {
    const startAngle = (cumulative / total) * 2 * Math.PI;
    cumulative += d.value;
    const endAngle = (cumulative / total) * 2 * Math.PI;
    const largeArc = endAngle - startAngle > Math.PI ? 1 : 0;
    const x1 = cx + r * Math.sin(startAngle);
    const y1 = cy - r * Math.cos(startAngle);
    const x2 = cx + r * Math.sin(endAngle);
    const y2 = cy - r * Math.cos(endAngle);
    const path = `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${largeArc} 1 ${x2} ${y2} Z`;
    return { path, color: COLORS[d.label] || '#3b82f6', d };
  });

  return (
    <div className="flex items-center gap-6">
      <svg width={size} height={size}>
        {slices.map((s, i) => <path key={i} d={s.path} fill={s.color} />)}
      </svg>
      <ul className="text-sm space-y-1">
        {data.map((d) => (
          <li key={d.label} className="flex items-center gap-2">
            <span
              className="inline-block w-3 h-3 rounded-sm"
              style={{ background: COLORS[d.label] || '#3b82f6' }}
            />
            <span className="text-slate-700 capitalize">{d.label}</span>
            <span className="text-muted">{d.value}</span>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default StatusPie;
