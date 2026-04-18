<script setup lang="ts">
import { ref, watch, inject } from 'vue'
import { ReloadOutlined, NodeIndexOutlined } from '@ant-design/icons-vue'
import { listFinishedInstances, listFinishedTasks, getProcessActivities } from '../api'

const currentUser = inject('currentUser') as any
const showToast = inject('showToast') as (type: string, message: string) => void

const activeTab = ref('instances')

const finishedInstances = ref<any[]>([])
const finishedTasks = ref<any[]>([])
const loading = ref(true)

// 轨迹弹窗
const traceModalVisible = ref(false)
const activities = ref<any[]>([])
const traceLoading = ref(false)

watch(currentUser, () => loadData(), { immediate: true })

async function loadData() {
  loading.value = true
  try {
    const [instRes, taskRes] = await Promise.all([
      listFinishedInstances(),
      listFinishedTasks(currentUser.value)
    ])
    finishedInstances.value = instRes.data
    finishedTasks.value = taskRes.data
  } catch (e: any) {
    showToast('error', '加载历史记录失败')
  } finally {
    loading.value = false
  }
}

async function viewTrace(processInstanceId: string) {
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

function formatDuration(ms: number) {
  if (!ms) return '-'
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return `${seconds} 秒`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  return `${hours} 小时 ${minutes % 60} 分钟`
}

const instanceColumns = [
  { title: '流程名称', dataIndex: 'processDefinitionName', key: 'processDefinitionName' },
  { title: '发起人', dataIndex: 'initiator', key: 'initiator' },
  { title: '业务编号', dataIndex: 'businessKey', key: 'businessKey' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 190 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 190 },
  { title: '耗时', dataIndex: 'durationInMillis', key: 'duration', width: 120 },
  { title: '操作', key: 'action', width: 100 }
]

const taskColumns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName' },
  { title: '办理人', dataIndex: 'assignee', key: 'assignee' },
  { title: '审批意见', dataIndex: 'comment', key: 'comment', ellipsis: true },
  { title: '完成时间', dataIndex: 'endTime', key: 'endTime', width: 190 },
  { title: '耗时', dataIndex: 'durationInMillis', key: 'duration', width: 120 },
  { title: '操作', key: 'action', width: 100 }
]
</script>

<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">已办历史</h1>
      <p class="page-desc">查看已完结的流程和审批记录</p>
    </div>

    <a-card>
      <template #extra>
        <a-button size="small" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-tabs v-model:activeKey="activeTab">
        <!-- Tab 1: 已办流程 -->
        <a-tab-pane key="instances" :tab="`📁 已办流程 (${finishedInstances.length})`">
          <a-table
            :columns="instanceColumns"
            :data-source="finishedInstances"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            row-key="processInstanceId"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'processDefinitionName'">
                <span style="font-weight: 500">
                  {{ record.processDefinitionName || record.processDefinitionKey }}
                </span>
              </template>
              <template v-else-if="column.key === 'initiator'">
                {{ record.initiator || '-' }}
              </template>
              <template v-else-if="column.key === 'businessKey'">
                {{ record.businessKey || '-' }}
              </template>
              <template v-else-if="column.key === 'startTime'">
                {{ formatDate(record.startTime) }}
              </template>
              <template v-else-if="column.key === 'endTime'">
                {{ formatDate(record.endTime) }}
              </template>
              <template v-else-if="column.key === 'duration'">
                <a-tag color="green">{{ formatDuration(record.durationInMillis) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="viewTrace(record.processInstanceId)">
                  <template #icon><NodeIndexOutlined /></template>
                  轨迹
                </a-button>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无已完结的流程" />
            </template>
          </a-table>
        </a-tab-pane>

        <!-- Tab 2: 我的已办 -->
        <a-tab-pane key="tasks" :tab="`✅ 我的已办 (${finishedTasks.length})`">
          <div style="margin-bottom: 12px">
            <span style="font-size: 13px; color: #8c8c8c">
              当前用户：<a-tag color="blue">{{ currentUser }}</a-tag>
            </span>
          </div>
          <a-table
            :columns="taskColumns"
            :data-source="finishedTasks"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            row-key="taskId"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'taskName'">
                <a-tag color="blue">{{ record.taskName }}</a-tag>
              </template>
              <template v-else-if="column.key === 'assignee'">
                {{ record.assignee }}
              </template>
              <template v-else-if="column.key === 'comment'">
                {{ record.comment || '-' }}
              </template>
              <template v-else-if="column.key === 'endTime'">
                {{ formatDate(record.endTime) }}
              </template>
              <template v-else-if="column.key === 'duration'">
                <a-tag color="green">{{ formatDuration(record.durationInMillis) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="viewTrace(record.processInstanceId)">
                  <template #icon><NodeIndexOutlined /></template>
                  轨迹
                </a-button>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无已办任务" />
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 审批轨迹弹窗 -->
    <a-modal
      v-model:open="traceModalVisible"
      title="📜 审批轨迹"
      :footer="null"
      width="560px"
    >
      <a-spin :spinning="traceLoading">
        <a-timeline class="trace-timeline" style="margin-top: 16px">
          <a-timeline-item
            v-for="(act, idx) in activities"
            :key="idx"
            :color="act.endTime ? 'green' : 'blue'"
          >
            <div class="trace-card">
              <div class="trace-title">
                {{ act.activityName || act.activityId }}
                <a-tag v-if="act.assignee" color="blue" style="margin-left: 8px">{{ act.assignee }}</a-tag>
              </div>
              <div class="trace-meta">
                {{ act.activityType }}
                · 开始: {{ formatDate(act.startTime) }}
                <template v-if="act.endTime"> · 结束: {{ formatDate(act.endTime) }}</template>
              </div>
              <div v-if="act.comment" class="trace-comment">💬 {{ act.comment }}</div>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-spin>
    </a-modal>
  </div>
</template>
