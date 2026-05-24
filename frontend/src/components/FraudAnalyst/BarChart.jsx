/**
 * Lightweight SVG bar chart. No charting library — just a list of
 * { label, value } points rendered as proportional bars.
 */
const BarChart = ({ data = [], height = 180 }) => {
  if (data.length === 0) return <div className="text-sm text-muted">No data.</div>;

  const max = Math.max(...data.map((d) => d.value), 1);
  const barW = 100 / data.length;

  return (
    <div>
      <svg width="100%" height={height} viewBox={`0 0 100 ${height}`} preserveAspectRatio="none">
        {data.map((d, i) => {
          const h = (d.value / max) * (height - 24);
          const x = i * barW + barW * 0.15;
          const w = barW * 0.7;
          const y = height - 18 - h;
          return (
            <g key={i}>
              <rect x={x} y={y} width={w} height={h} fill="#0b3d91" rx="0.4" />
              <text x={x + w / 2} y={y - 2} fontSize="3" fill="#475569" textAnchor="middle">
                {d.value}
              </text>
            </g>
          );
        })}
      </svg>
      <div className="flex text-[10px] text-muted">
        {data.map((d, i) => (
          <div key={i} style={{ width: `${barW}%` }} className="text-center truncate px-1">{d.label}</div>
        ))}
      </div>
    </div>
  );
};

export default BarChart;
