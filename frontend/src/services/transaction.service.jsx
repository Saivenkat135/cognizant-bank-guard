import api from './api.config';

const wrap = async (promise) => {
  try {
    const res = await promise;
    return { success: true, data: res.data };
  } catch (err) {
    return { success: false, error: err.response?.data?.message || 'Request failed' };
  }
};

export const getAllTransactions       = ()          => wrap(api.get('/api/transactions'));
export const getTransactionById       = (id)        => wrap(api.get(`/api/transactions/${id}`));
export const getTransactionsByCustomer = (customerId) =>
  wrap(api.get(`/api/transactions/customer/${customerId}`));
