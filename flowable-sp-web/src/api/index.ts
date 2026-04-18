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

// ==================== 任务 API ====================

/** 查询待办任务（候选组待认领 + 已认领） */
export function listLeaveTasks(userId: string) {
  return api.get('/leaves/tasks', { params: { userId } })
}

/** 认领任务 */
export function claimTask(taskId: string, userId: string) {
  return api.post(`/leaves/tasks/${taskId}/claim`, null, { params: { userId } })
}

/** 取消认领 */
export function unclaimTask(taskId: string) {
  return api.post(`/leaves/tasks/${taskId}/unclaim`)
}

/** 审批通过 */
export function approveTask(taskId: string, comment?: string) {
  return api.post(`/leaves/tasks/${taskId}/approve`, { comment })
}

/** 审批驳回 */
export function rejectTask(taskId: string, comment?: string) {
  return api.post(`/leaves/tasks/${taskId}/reject`, { comment })
}

/** 重新提交（申请人修改后） */
export function resubmitTask(taskId: string, reason?: string) {
  return api.post(`/leaves/tasks/${taskId}/resubmit`, { reason })
}

/** 撤回（在修改节点放弃） */
export function withdrawTask(taskId: string) {
  return api.post(`/leaves/tasks/${taskId}/withdraw`)
}

// ==================== 通用查询 API ====================

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
