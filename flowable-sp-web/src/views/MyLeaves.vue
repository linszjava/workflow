<script setup lang="ts">
import { ref, watch, inject } from 'vue'
import { ReloadOutlined, StopOutlined, NodeIndexOutlined } from '@ant-design/icons-vue'
import { listMyLeaves, cancelLeave, getProcessActivities } from '../api'

const currentUser = inject('currentUser') as any
const showToast = inject('showToast') as (type: string, message: string) => void

const leaves = ref<any[]>([])
const loading = ref(true)

// 轨迹弹窗
const traceModalVisible = ref(false)
const activities = ref<any[]>([])
const traceLoading = ref(false)

watch(currentUser, () => loadData(), { immediate: true })

async function loadData() {
  loading.value = true
  try {
    const res = await listMyLeaves(currentUser.value)
    leaves.value = res.data
  } catch (e: any) {
    showToast('error', '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel(record: any) {
  try {
    await cancelLeave(String(record.id))
    showToast('success', '已撤销')
    await loadData()
  } catch (e: any) {
    showToast('error', '撤销失败: ' + e.message)
  }
}

async function viewTrace(processInstanceId: string) {
  if (!processInstanceId) {
    showToast('error', '无流程实例')
    return
  }
  traceLoading.value = true
  traceModalVisible.value = true
  try {
    const res = await getProcessActivities(processInstanceId)
    activities.value = res.data
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

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '审批中', color: 'processing' },
  2: { text: '已通过', color: 'success' },
  3: { text: '已驳回', color: 'error' },
  4: { text: '已撤销', color: 'warning' }
}

const leaveTypeMap: Record<string, string> = {
  annual: '年假', sick: '病假', personal: '事假'
}

const columns = [
  { title: '请假类型', dataIndex: 'leaveType', key: 'leaveType', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 120 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 120 },
  { title: '天数', dataIndex: 'days', key: 'days', width: 70 },
  { title: '事由', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 140 }
]
</script>

<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">我的申请</h1>
      <p class="page-desc">
        查看 <a-tag color="blue">{{ currentUser }}</a-tag> 提交的所有请假申请及审批状态
      </p>
    </div>

    <a-card>
      <template #extra>
        <a-button size="small" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-table :columns="columns" :data-source="leaves" :loading="loading"
               :pagination="{ pageSize: 10 }" row-key="id" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'leaveType'">
            <a-tag>{{ leaveTypeMap[record.leaveType] || record.leaveType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-badge :status="statusMap[record.status]?.color || 'default'"
                     :text="statusMap[record.status]?.text || '未知'" />
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button v-if="record.processInstanceId" type="link" size="small"
                        @click="viewTrace(record.processInstanceId)">
                <template #icon><NodeIndexOutlined /></template>
                轨迹
              </a-button>
              <a-popconfirm v-if="record.status === 1"
                            title="确定撤销此请假申请？" @confirm="handleCancel(record)">
                <a-button type="link" danger size="small">
                  <template #icon><StopOutlined /></template>
                  撤销
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
        <template #emptyText>
          <a-empty description="暂无请假记录" />
        </template>
      </a-table>
    </a-card>

    <!-- 审批轨迹弹窗 -->
    <a-modal v-model:open="traceModalVisible" title="📜 审批轨迹" :footer="null" width="560px">
      <a-spin :spinning="traceLoading">
        <a-timeline class="trace-timeline" style="margin-top: 16px">
          <a-timeline-item v-for="(act, idx) in activities" :key="idx"
                           :color="act.endTime ? 'green' : 'blue'">
            <div class="trace-card">
              <div class="trace-title">
                {{ act.activityName || act.activityId }}
                <a-tag v-if="act.assignee" color="blue" style="margin-left: 8px">{{ act.assignee }}</a-tag>
              </div>
              <div class="trace-meta">
                {{ act.activityType }} · {{ formatDate(act.startTime) }}
                <template v-if="act.endTime"> → {{ formatDate(act.endTime) }}</template>
              </div>
              <div v-if="act.comment" class="trace-comment">💬 {{ act.comment }}</div>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-spin>
    </a-modal>
  </div>
</template>
