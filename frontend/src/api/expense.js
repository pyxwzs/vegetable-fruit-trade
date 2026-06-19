import request from './request'

export const getExpenses = (params) => request.get('/expenses', { params })
export const getExpenseYearTotal = (params) => request.get('/expenses/year-total', { params })
export const createExpense = (data) => request.post('/expenses', data)
export const updateExpense = (id, data) => request.put(`/expenses/${id}`, data)
export const deleteExpense = (id) => request.delete(`/expenses/${id}`)
