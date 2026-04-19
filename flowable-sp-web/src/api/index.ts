import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8081/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      console.error('API Error:', res.message)
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  (error) => {
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

export default api

// ==================== 请假申请 API ====================

/** 提交请假申请 */
export function submitLeave(data: {
  userId: string; leaveType: string; startDate: string
  endDate: string; days: number; reason: string
}) {
  return api.post('/leaves', data)
}

/** 查询我的请假单 */
export function listMyLeaves(userId: string) {
  return api.get('/leaves', { params: { userId } })
}

/** 请假单详情 */
export function getLeaveDetail(id: string) {
  return api.get(`/leaves/${id}`)
}

/** 撤销请假（强制终止流程） */
export function cancelLeave(id: string) {
  return api.post(`/leaves/${id}/cancel`)
}

// ==================== 统一任务 API (适配请假与采购) ====================

/** 查询待办任务（候选组待认领 + 已认领） */
export function listGlobalTasks(userId: string) {
  return api.get('/tasks', { params: { userId } })
}

/** 认领任务 */
export function claimAnyTask(taskId: string, userId: string) {
  return api.post(`/tasks/${taskId}/claim`, { userId })
}

/** 取消认领 */
export function unclaimAnyTask(taskId: string) {
  return api.post(`/tasks/${taskId}/unclaim`)
}

/** 审批通过 */
export function approveAnyTask(taskId: string, comment?: string) {
  return api.post(`/tasks/${taskId}/approve`, { comment })
}

/** 审批驳回 */
export function rejectAnyTask(taskId: string, comment?: string) {
  return api.post(`/tasks/${taskId}/reject`, { comment })
}

/** 任务转办（彻底转交） */
export function transferTask(taskId: string, targetUserId: string) {
  return api.post(`/tasks/${taskId}/transfer`, { targetUserId })
}

/** 任务委派（代为处理后打回） */
export function delegateTask(taskId: string, targetUserId: string) {
  return api.post(`/tasks/${taskId}/delegate`, { targetUserId })
}

/** 重新提交（由于采购暂未做打回逻辑，仅保留在此处兼容请假流程的遗留特定端点） */
export function resubmitTask(taskId: string, reason?: string) {
  return api.post(`/leaves/tasks/${taskId}/resubmit`, { reason })
}

/** 撤回（目前仅适配请假流程） */
export function withdrawTask(taskId: string) {
  return api.post(`/leaves/tasks/${taskId}/withdraw`)
}

// ==================== 采购申请 API ====================

/** 提交采购申请 */
export function submitPurchase(data: {
  userId: string; itemName: string; price: number; quantity: number; reason: string
}) {
  return api.post('/purchases/submit', data)
}

/** 查询我的采购申请单 */
export function listMyPurchases(userId: string) {
  return api.get('/purchases/my', { params: { userId } })
}

// ==================== 通用查询 API ====================

/** 获取系统全量用户（花名册） */
export function listSystemUsers() {
  return api.get('/users/list')
}

/** 获取系统状态 */
export function getStatus() {
  return api.get('/demo/status')
}

/** 查询审批轨迹 */
export function getProcessActivities(processInstanceId: string) {
  return api.get(`/history/process-instances/${processInstanceId}/activities`)
}

/** 查询已完成的流程实例 */
export function listFinishedInstances() {
  return api.get('/history/process-instances')
}

/** 查询已完成任务 */
export function listFinishedTasks(assignee: string) {
  return api.get('/history/tasks', { params: { assignee } })
}
