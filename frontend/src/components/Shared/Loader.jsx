const Loader = ({ label = 'Loading…' }) => (
  <div className="flex items-center justify-center py-12 text-muted">
    <div className="animate-spin h-5 w-5 border-2 border-[var(--primary)] border-t-transparent rounded-full mr-3" />
    {label}
  </div>
);

export default Loader;
