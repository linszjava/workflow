<script setup lang="ts">
import { ref, watch, inject } from 'vue'
import {
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  NodeIndexOutlined,
  UserSwitchOutlined,
  UndoOutlined,
  SendOutlined,
  StopOutlined
} from '@ant-design/icons-vue'
import {
  listGlobalTasks, claimAnyTask, unclaimAnyTask,
  approveAnyTask, rejectAnyTask, resubmitTask, withdrawTask,
  getProcessActivities, transferTask, delegateTask, listSystemUsers
} from '../api'

const currentUser = inject('currentUser') as any
const showToast = inject('showToast') as (type: string, message: string) => void

const tasks = ref<any[]>([])
const loading = ref(true)

// 审批弹窗
const approvalModalVisible = ref(false)
const currentTask = ref<any>(null)
const approvalComment = ref('')
const approving = ref(false)

// 修改重新提交弹窗
const modifyModalVisible = ref(false)
const modifyReason = ref('')
const submitting = ref(false)

// 轨迹弹窗
const traceModalVisible = ref(false)
const activities = ref<any[]>([])
const traceLoading = ref(false)

// 指派转交弹窗
const assignModalVisible = ref(false)
const assignAction = ref('') // 'transfer' or 'delegate'
const assignTarget = ref('')
const assignLoading = ref(false)
const systemUsers = ref<any[]>([])

async function loadSystemUsers() {
  try {
    const res = await listSystemUsers()
    systemUsers.value = res.data
  } catch (e: any) {
    showToast('error', '加载人员列表失败')
  }
}

function openAssign(action: string, record: any) {
  currentTask.value = record
  assignAction.value = action
  assignTarget.value = undefined // 重置为未选状态
  assignModalVisible.value = true
  if (systemUsers.value.length === 0) {
    loadSystemUsers()
  }
}

async function handleAssignSubmit() {
  if (!assignTarget.value) {
    showToast('error', '请输入受托人ID')
    return
  }
  assignLoading.value = true
  try {
    if (assignAction.value === 'transfer') {
      await transferTask(currentTask.value.taskId, assignTarget.value)
      showToast('success', '已永久转交给 ' + assignTarget.value)
    } else {
      await delegateTask(currentTask.value.taskId, assignTarget.value)
      showToast('success', '已委派，被委派人完成后会自动返回给您')
    }
    assignModalVisible.value = false
    await loadTasks()
  } catch(e: any) {
    showToast('error', '指派失败: ' + e.message)
  } finally {
    assignLoading.value = false
  }
}

watch(currentUser, () => loadTasks(), { immediate: true })

async function loadTasks() {
  loading.value = true
  try {
    const res = await listGlobalTasks(currentUser.value)
    tasks.value = res.data
  } catch (e: any) {
    showToast('error', '加载待办失败')
  } finally {
    loading.value = false
  }
}

// ---- 认领 ----
async function handleClaim(record: any) {
  try {
    await claimAnyTask(record.taskId, currentUser.value)
    showToast('success', '认领成功，任务已分配给你')
    await loadTasks()
  } catch (e: any) {
    showToast('error', '认领失败: ' + e.message)
  }
}

async function handleUnclaim(record: any) {
  try {
    await unclaimAnyTask(record.taskId)
    showToast('success', '已退回到候选组')
    await loadTasks()
  } catch (e: any) {
    showToast('error', '取消认领失败: ' + e.message)
  }
}

// ---- 审批 ----
function openApproval(record: any) {
  currentTask.value = record
  approvalComment.value = ''
  approvalModalVisible.value = true
}

async function handleApprove() {
  if (!currentTask.value) return
  approving.value = true
  try {
    await approveAnyTask(currentTask.value.taskId, approvalComment.value)
    showToast('success', '审批通过 ✅')
    approvalModalVisible.value = false
    await loadTasks()
  } catch (e: any) {
    showToast('error', '操作失败: ' + e.message)
  } finally {
    approving.value = false
  }
}

async function handleReject() {
  if (!currentTask.value) return
  approving.value = true
  try {
    await rejectAnyTask(currentTask.value.taskId, approvalComment.value)
    showToast('success', '已驳回，退回给申请人修改 🔙')
    approvalModalVisible.value = false
    await loadTasks()
  } catch (e: any) {
    showToast('error', '操作失败: ' + e.message)
  } finally {
    approving.value = false
  }
}

// ---- 申请人修改后操作 ----
function openModify(record: any) {
  currentTask.value = record
  modifyReason.value = record.reason || ''
  modifyModalVisible.value = true
}

async function handleResubmit() {
  if (!currentTask.value) return
  submitting.value = true
  try {
    await resubmitTask(currentTask.value.taskId, modifyReason.value)
    showToast('success', '已重新提交，等待审批 🔄')
    modifyModalVisible.value = false
    await loadTasks()
  } catch (e: any) {
    showToast('error', '提交失败: ' + e.message)
  } finally {
    submitting.value = false
  }
}

async function handleWithdraw() {
  if (!currentTask.value) return
  submitting.value = true
  try {
    await withdrawTask(currentTask.value.taskId)
    showToast('success', '已撤回申请')
    modifyModalVisible.value = false
    await loadTasks()
  } catch (e: any) {
    showToast('error', '撤回失败: ' + e.message)
  } finally {
    submitting.value = false
  }
}

// ---- 轨迹 ----
async function viewTrace(processInstanceId: string) {
  if (!processInstanceId) {
    showToast('error', '无流程实例')
    return
  }
  traceLoading.value = true
  traceModalVisible.value = true
  try {
    const res = await getProcessActivities(processInstanceId)
    // 1. 过滤：去掉线和网关
    let list = res.data.filter((a: any) => !['sequenceFlow', 'exclusiveGateway'].includes(a.activityType))
    
    // 2. 预测未来节点
    const isFinished = list.some((a: any) => a.activityType === 'endEvent')
    if (!isFinished && list.length > 0) {
      const lastAct = list[list.length - 1]
      if (lastAct.activityId === 'startEvent') {
        list.push({ activityName: '部门经理审批', activityType: 'userTask', isFuture: true })
        list.push({ activityName: 'HR审批', activityType: 'userTask', isFuture: true })
        list.push({ activityName: '结束', activityType: 'endEvent', isFuture: true })
      } else if (lastAct.activityId === 'managerTask') {
        list.push({ activityName: 'HR审批', activityType: 'userTask', isFuture: true })
        list.push({ activityName: '结束', activityType: 'endEvent', isFuture: true })
      } else if (lastAct.activityId === 'hrTask') {
        list.push({ activityName: '结束', activityType: 'endEvent', isFuture: true })
      } else if (lastAct.activityId === 'modifyTask') {
        list.push({ activityName: '部门经理审批', activityType: 'userTask', isFuture: true })
        list.push({ activityName: 'HR审批', activityType: 'userTask', isFuture: true })
        list.push({ activityName: '结束', activityType: 'endEvent', isFuture: true })
      }
    }
    
    activities.value = list
  } catch (e: any) {
    showToast('error', '获取轨迹失败')
  } finally {
    traceLoading.value = false
  }
}

function formatDate(date: string) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const leaveTypeMap: Record<string, string> = {
  annual: '年假', sick: '病假', personal: '事假'
}

const columns = [
  { title: '状态', dataIndex: 'taskStatus', key: 'taskStatus', width: 90 },
  { title: '任务节点', dataIndex: 'taskName', key: 'taskName', width: 140 },
  { title: '业务单号', dataIndex: 'bizTitle', key: 'bizTitle', width: 160 },
  { title: '详情', dataIndex: 'bizDetail', key: 'bizDetail', ellipsis: true },
  { title: '申请人', dataIndex: 'applicant', key: 'applicant', width: 90 },
  { title: '操作', key: 'action', width: 220 }
]
</script>

<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">待办审批</h1>
      <p class="page-desc">
        当前用户 <a-tag color="blue">{{ currentUser }}</a-tag> 的待处理任务
      </p>
    </div>

    <a-card>
      <template #extra>
        <a-button size="small" @click="loadTasks">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-table :columns="columns" :data-source="tasks" :loading="loading"
               :pagination="false" row-key="taskId" size="middle">
        <template #bodyCell="{ column, record }">
          <!-- 认领状态 -->
          <template v-if="column.key === 'taskStatus'">
            <a-tag v-if="record.taskStatus === 'candidate'" color="orange">待认领</a-tag>
            <a-tag v-else color="green">已认领</a-tag>
          </template>

          <template v-else-if="column.key === 'taskName'">
            <a-tag :color="record.taskDefinitionKey === 'modifyTask' ? 'red' : 'blue'">
              {{ record.taskName }}
            </a-tag>
            <a-tag v-if="record.delegationState === 'PENDING'" color="purple">代人受托</a-tag>
            <a-tag v-if="record.delegationState === 'RESOLVED'" color="green">委派回归</a-tag>
          </template>

          <template v-else-if="column.key === 'applicant'">
            <span style="font-weight: 500">{{ record.applicant || record.initiator || '-' }}</span>
          </template>

          <template v-else-if="column.key === 'bizTitle'">
            <span style="font-weight: 500">{{ record.bizTitle || '-' }}</span>
          </template>

          <template v-else-if="column.key === 'bizDetail'">
            {{ record.bizDetail || '-' }}
          </template>

          <!-- 操作按钮（根据任务类型和状态不同展示不同按钮） -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <!-- 申请人修改节点：显示 重新提交 / 撤回 -->
              <template v-if="record.taskDefinitionKey === 'modifyTask'">
                <a-button type="primary" size="small" @click="openModify(record)">
                  <template #icon><SendOutlined /></template>
                  处理
                </a-button>
              </template>

              <!-- 审批节点 -->
              <template v-else>
                <!-- 待认领：显示认领按钮 -->
                <template v-if="record.taskStatus === 'candidate'">
                  <a-button type="primary" size="small" @click="handleClaim(record)">
                    <template #icon><UserSwitchOutlined /></template>
                    认领
                  </a-button>
                </template>

                <!-- 已认领：显示审批 + 指派能力 + 退回 -->
                <template v-else>
                  <a-button type="primary" size="small" @click="openApproval(record)">
                    {{ record.delegationState === 'PENDING' ? '代拟并返回' : '去办理' }}
                  </a-button>
                  <a-dropdown trigger="click">
                    <a-button size="small">
                      更多 <NodeIndexOutlined />
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item key="transfer" @click="openAssign('transfer', record)">
                          彻底转交他人(Transfer)
                        </a-menu-item>
                        <a-menu-item key="delegate" @click="openAssign('delegate', record)">
                          委派代拟意见(Delegate)
                        </a-menu-item>
                        <a-menu-divider />
                        <a-menu-item key="unclaim" @click="handleUnclaim(record)">
                          丢回公海池(Unclaim)
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </template>
              </template>

              <a-button size="small" @click="viewTrace(record.processInstanceId)">
                <template #icon><NodeIndexOutlined /></template>
              </a-button>
            </a-space>
          </template>
        </template>

        <template #emptyText>
          <a-empty description="暂无待办任务 ☕" />
        </template>
      </a-table>
    </a-card>

    <!-- ========== 审批弹窗 ========== -->
    <a-modal v-model:open="approvalModalVisible"
             :title="`审批 — ${currentTask?.taskName || ''}`" :footer="null" width="500px">
      <a-descriptions :column="2" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="单据">{{ currentTask?.bizTitle || '-' }}</a-descriptions-item>
        <a-descriptions-item label="申请人">{{ currentTask?.applicant || '-' }}</a-descriptions-item>
        <a-descriptions-item label="详情" :span="2">{{ currentTask?.bizDetail || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-form layout="vertical">
        <a-form-item label="审批意见">
          <a-textarea v-model:value="approvalComment" :rows="3" placeholder="请输入审批意见..." />
        </a-form-item>
      </a-form>

      <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px">
        <a-button @click="approvalModalVisible = false" :disabled="approving">取消</a-button>
        <a-button danger :loading="approving" @click="handleReject">
          <template #icon><CloseCircleOutlined /></template>
          驳回退回
        </a-button>
        <a-button type="primary" :loading="approving" @click="handleApprove">
          <template #icon><CheckCircleOutlined /></template>
          通过
        </a-button>
      </div>
    </a-modal>

    <!-- ========== 申请人修改弹窗 ========== -->
    <a-modal v-model:open="modifyModalVisible"
             title="📝 申请被驳回 — 修改后重新提交" :footer="null" width="500px">
      <a-alert message="您的请假申请已被驳回，请修改后重新提交，或选择撤回申请。"
               type="warning" show-icon style="margin-bottom: 16px" />

      <a-form layout="vertical">
        <a-form-item label="修改请假事由">
          <a-textarea v-model:value="modifyReason" :rows="4" placeholder="修改请假事由后重新提交..." />
        </a-form-item>
      </a-form>

      <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px">
        <a-button @click="modifyModalVisible = false" :disabled="submitting">取消</a-button>
        <a-button danger :loading="submitting" @click="handleWithdraw">
          <template #icon><StopOutlined /></template>
          撤回申请
        </a-button>
        <a-button type="primary" :loading="submitting" @click="handleResubmit">
          <template #icon><SendOutlined /></template>
          重新提交
        </a-button>
      </div>
    </a-modal>

    <!-- ========== 审批轨迹弹窗 ========== -->
    <a-modal v-model:open="traceModalVisible" title="📜 审批轨迹" :footer="null" width="560px">
      <a-spin :spinning="traceLoading">
        <a-timeline class="trace-timeline" style="margin-top: 16px">
          <a-timeline-item v-for="(act, idx) in activities" :key="idx"
                           :color="act.isFuture ? 'gray' : (act.endTime ? 'green' : 'blue')">
            <div class="trace-card" :style="{ opacity: act.isFuture ? 0.6 : 1 }">
              <div class="trace-title">
                {{ act.activityName || act.activityId }}
                <a-tag v-if="act.assignee" color="blue" style="margin-left: 8px">{{ act.assignee }}</a-tag>
                <a-tag v-if="act.isFuture" color="default" style="margin-left: 8px">📍 待处理</a-tag>
              </div>
              <div class="trace-meta" v-if="!act.isFuture">
                {{ act.activityType === 'startEvent' ? '发起节点' : (act.activityType === 'endEvent' ? '结束节点' : '审批节点') }}
                <span v-if="act.startTime"> · {{ formatDate(act.startTime) }}</span>
                <template v-if="act.endTime"> → {{ formatDate(act.endTime) }}</template>
              </div>
              <div class="trace-meta" v-else>
                系统预测步骤
              </div>
              <div v-if="act.comment" class="trace-comment">💬 {{ act.comment }}</div>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-spin>
    </a-modal>

    <!-- ========== 转交/委派 弹窗 ========== -->
    <a-modal v-model:open="assignModalVisible"
             :title="assignAction === 'transfer' ? '转交审批权' : '委派代拟意见'" :footer="null" width="400px">
      <a-alert :message="assignAction === 'transfer' ? '将任务执行权永久转给另一个人。' : '借由别人代拟意见，对方点完成(代拟)后，任务将强制回传进您的收件箱。'"
               :type="assignAction === 'transfer' ? 'info' : 'warning'" show-icon style="margin-bottom: 16px" />
      <a-form layout="vertical">
        <a-form-item label="选择受理人">
          <a-select v-model:value="assignTarget" placeholder="请下拉选取接防人员" style="width: 100%" show-search option-filter-prop="label">
            <a-select-option v-for="u in systemUsers" :key="u.userId" :value="u.userId" :label="u.userName">
               {{ u.userName || u.userId }}
               <span style="color: gray; font-size: 12px; margin-left: 8px">[{{ u.roleName || '无岗位' }}]</span>
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px">
        <a-button @click="assignModalVisible = false" :disabled="assignLoading">取消</a-button>
        <a-button type="primary" :loading="assignLoading" @click="handleAssignSubmit">
          确认
        </a-button>
      </div>
    </a-modal>
  </div>
</template>
