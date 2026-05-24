import api from './api.config';

const wrap = async (promise) => {
  try {
    const res = await promise;
    return { success: true, data: res.data };
  } catch (err) {
    return { success: false, error: err.response?.data?.message || 'Request failed' };
  }
};

// Rules live in the Decision Engine (decision_db.decision_rules) and are
// reached through the gateway's existing /api/gemini/** route.
const PATH = '/api/gemini/rules';

export const listRules   = ()                       => wrap(api.get(PATH));
export const addRule     = (text, riskScore)        => wrap(api.post(PATH, { text, riskScore }));
export const updateRule  = (id, text, riskScore)    => wrap(api.put(`${PATH}/${id}`, { text, riskScore }));
export const deleteRule  = (id)                     => wrap(api.delete(`${PATH}/${id}`));
