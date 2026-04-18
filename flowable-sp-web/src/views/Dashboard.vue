<script setup lang="ts">
import { ref, onMounted, inject, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  FundProjectionScreenOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  RightOutlined,
  FormOutlined
} from '@ant-design/icons-vue'
import { getStatus, listMyLeaves, listLeaveTasks } from '../api'

const router = useRouter()
const currentUser = inject('currentUser') as any

const stats = ref({
  totalProcessDefinitions: 0,
  runningProcessInstances: 0,
  pendingTasks: 0,
  flowableVersion: '7.2.0'
})

const myLeaves = ref<any[]>([])
const myTasks = ref<any[]>([])
const loading = ref(true)

watch(currentUser, () => loadData(), { immediate: true })

async function loadData() {
  loading.value = true
  try {
    const [statusRes, leavesRes, tasksRes] = await Promise.all([
      getStatus(),
      listMyLeaves(currentUser.value),
      listLeaveTasks(currentUser.value)
    ])
    stats.value = statusRes.data
    myLeaves.value = leavesRes.data.slice(0, 5)
    myTasks.value = tasksRes.data.slice(0, 5)
  } catch (e) {
    console.error('加载数据失败:', e)
  } finally {
    loading.value = false
  }
}

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '审批中', color: 'processing' },
  2: { text: '已通过', color: 'success' },
  3: { text: '已驳回', color: 'error' },
  4: { text: '已撤销', color: 'warning' }
}

const leaveColumns = [
  { title: '请假类型', dataIndex: 'leaveType', key: 'leaveType', width: 100 },
  { title: '天数', dataIndex: 'days', key: 'days', width: 70 },
  { title: '事由', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 }
]

const taskColumns = [
  { title: '任务', dataIndex: 'taskName', key: 'taskName' },
  { title: '申请人', dataIndex: 'applicant', key: 'applicant' },
  { title: '天数', dataIndex: 'days', key: 'days', width: 70 }
]

const leaveTypeMap: Record<string, string> = {
  annual: '年假', sick: '病假', personal: '事假'
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">工作台</h1>
      <p class="page-desc">欢迎回来，{{ currentUser }} 👋</p>
    </div>

    <a-spin :spinning="loading">
      <!-- 统计卡片 -->
      <a-row :gutter="16" style="margin-bottom: 24px">
        <a-col :span="6">
          <a-card class="stat-card" hoverable @click="router.push('/my-leaves')">
            <a-statistic title="我的申请" :value="myLeaves.length">
              <template #prefix><FundProjectionScreenOutlined style="color: #1677ff" /></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card" hoverable @click="router.push('/tasks')">
            <a-statistic title="待我审批" :value="myTasks.length">
              <template #prefix><ClockCircleOutlined style="color: #fa8c16" /></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card" hoverable>
            <a-statistic title="运行中流程" :value="stats.runningProcessInstances">
              <template #prefix><PlayCircleOutlined style="color: #52c41a" /></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card" hoverable @click="router.push('/apply')">
            <a-statistic title="快速申请" value="请假">
              <template #prefix><FormOutlined style="color: #722ed1" /></template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <!-- 两栏 -->
      <a-row :gutter="16">
        <a-col :span="12">
          <a-card title="📋 我的最近申请" size="small">
            <template #extra>
              <a @click="router.push('/my-leaves')">全部 <RightOutlined /></a>
            </template>
            <a-table :columns="leaveColumns" :data-source="myLeaves" :pagination="false" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'leaveType'">
                  <a-tag>{{ leaveTypeMap[record.leaveType] || record.leaveType }}</a-tag>
                </template>
                <template v-else-if="column.key === 'status'">
                  <a-badge :status="statusMap[record.status]?.color || 'default'" :text="statusMap[record.status]?.text || '未知'" />
                </template>
              </template>
              <template #emptyText><a-empty description="暂无申请记录" :image-style="{ height: '40px' }" /></template>
            </a-table>
          </a-card>
        </a-col>

        <a-col :span="12">
          <a-card title="⏳ 待我审批" size="small">
            <template #extra>
              <a @click="router.push('/tasks')">全部 <RightOutlined /></a>
            </template>
            <a-table :columns="taskColumns" :data-source="myTasks" :pagination="false" size="small" row-key="taskId">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'taskName'">
                  <a-tag color="orange">{{ record.taskName }}</a-tag>
                </template>
                <template v-else-if="column.key === 'applicant'">
                  <span style="font-weight: 500">{{ record.applicant || record.initiator || '-' }}</span>
                </template>
              </template>
              <template #emptyText><a-empty description="暂无待办" :image-style="{ height: '40px' }" /></template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>
