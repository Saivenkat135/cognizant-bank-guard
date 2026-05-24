import { STATUS_BADGE } from '../../utils/constants.jsx';

const StatusBadge = ({ status }) => {
  const key = (status || '').toLowerCase();
  const cls = STATUS_BADGE[key] || 'bg-slate-100 text-slate-700 border-slate-300';
  return <span className={`badge ${cls}`}>{key || '—'}</span>;
};

export default StatusBadge;
