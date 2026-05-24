import { useEffect, useState } from 'react';
import { listRules, addRule, updateRule, deleteRule } from '../../services/rule.service';
import PageHeader from '../Shared/PageHeader';
import Loader from '../Shared/Loader';

/**
 * Rules table for the Risk Manager.
 * Each row is (id, text, riskScore). Rules are persisted in
 * decision_db.decision_rules via the Decision Engine, and are picked up
 * by Gemini when scoring transactions.
 */
const Rules = () => {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [text, setText] = useState('');
  const [riskScore, setRiskScore] = useState('');

  // Inline edit state: which row is being edited + the in-flight values.
  const [editingId, setEditingId] = useState(null);
  const [editText, setEditText] = useState('');
  const [editRiskScore, setEditRiskScore] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);

  const reload = async () => {
    setLoading(true);
    const r = await listRules();
    if (r.success) setRules(Array.isArray(r.data) ? r.data : []);
    else setError(r.error);
    setLoading(false);
  };

  useEffect(() => { reload(); }, []);

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!text.trim() || riskScore === '') return;
    const score = Number(riskScore);
    if (isNaN(score)) { setError('Risk score must be a number.'); return; }

    const r = await addRule(text.trim(), score);
    if (!r.success) { setError(r.error); return; }
    setText('');
    setRiskScore('');
    reload();
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this rule?')) return;
    const r = await deleteRule(id);
    if (r.success) reload();
    else setError(r.error);
  };

  const beginEdit = (rule) => {
    setEditingId(rule.id);
    setEditText(rule.text ?? '');
    setEditRiskScore(rule.riskScore ?? '');
    setError('');
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditText('');
    setEditRiskScore('');
  };

  const handleSaveEdit = async (id) => {
    if (!editText.trim() || editRiskScore === '') {
      setError('Rule text and risk score are required.');
      return;
    }
    const score = Number(editRiskScore);
    if (isNaN(score)) { setError('Risk score must be a number.'); return; }

    setSavingEdit(true);
    const r = await updateRule(id, editText.trim(), score);
    setSavingEdit(false);
    if (!r.success) { setError(r.error); return; }
    cancelEdit();
    reload();
  };

  if (loading) return <Loader label="Loading rules…" />;

  return (
    <>
      <PageHeader title="Rules" subtitle="Detection rules used by the risk pipeline" />

      <form onSubmit={handleAdd} className="card mb-4 flex flex-col md:flex-row md:items-center gap-3">
        <div className="flex-1 min-w-0">
          <input
            className="input"
            placeholder="Describe a new rule…"
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
        </div>
        <div className="md:w-40 shrink-0">
          <input
            className="input"
            type="number"
            step="0.1"
            min="0"
            max="100"
            placeholder="Risk score"
            value={riskScore}
            onChange={(e) => setRiskScore(e.target.value)}
          />
        </div>
        <button type="submit" className="btn-primary whitespace-nowrap">Add rule</button>
      </form>

      {error && (
        <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-md px-3 py-2 mb-3">
          {error}
        </div>
      )}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th style={{ width: 80 }}>#</th>
              <th>Rule</th>
              <th style={{ width: 140 }}>Risk Score</th>
              <th style={{ width: 180 }}></th>
            </tr>
          </thead>
          <tbody>
            {rules.length === 0 ? (
              <tr><td colSpan={4} className="text-center text-muted py-8">No rules yet.</td></tr>
            ) : rules.map((r) => {
              const isEditing = editingId === r.id;
              return (
                <tr key={r.id}>
                  <td className="font-mono text-xs">{r.id}</td>
                  <td>
                    {isEditing ? (
                      <input
                        className="input"
                        value={editText}
                        onChange={(e) => setEditText(e.target.value)}
                        autoFocus
                      />
                    ) : (
                      r.text
                    )}
                  </td>
                  <td>
                    {isEditing ? (
                      <input
                        className="input"
                        type="number"
                        step="0.1"
                        min="0"
                        max="100"
                        value={editRiskScore}
                        onChange={(e) => setEditRiskScore(e.target.value)}
                      />
                    ) : (
                      r.riskScore
                    )}
                  </td>
                  <td>
                    {isEditing ? (
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleSaveEdit(r.id)}
                          disabled={savingEdit}
                          className="btn-primary text-xs"
                        >
                          {savingEdit ? 'Saving…' : 'Save'}
                        </button>
                        <button
                          onClick={cancelEdit}
                          disabled={savingEdit}
                          className="btn-ghost text-xs"
                        >
                          Cancel
                        </button>
                      </div>
                    ) : (
                      <div className="flex gap-2">
                        <button
                          onClick={() => beginEdit(r)}
                          className="btn-ghost text-xs text-blue-700"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(r.id)}
                          className="btn-ghost text-xs text-red-700"
                        >
                          Delete
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </>
  );
};

export default Rules;
