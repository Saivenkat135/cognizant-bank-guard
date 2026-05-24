import api from './api.config';

const wrap = async (promise) => {
  try {
    const res = await promise;
    return { success: true, data: res.data };
  } catch (err) {
    return { success: false, error: err.response?.data?.message || 'Request failed' };
  }
};

export const getAllCustomers = ()   => wrap(api.get('/api/customers'));
export const getCustomerById = (id) => wrap(api.get(`/api/customers/${id}`));
