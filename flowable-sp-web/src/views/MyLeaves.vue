<script setup lang="ts">
import { ref, watch, inject } from 'vue'
import { ReloadOutlined, StopOutlined, NodeIndexOutlined } from '@ant-design/icons-vue'
import { listMyLeaves, cancelLeave, getProcessActivities, listMyPurchases } from '../api'

const currentUser = inject('currentUser') as any
const showToast = inject('showToast') as (type: string, message: string) => void

const activeTab = ref('leave')
const leaves = ref<any[]>([])
const purchases = ref<any[]>([])
const loading = ref(true)

// 轨迹弹窗
const traceModalVisible = ref(false)
const activities = ref<any[]>([])
const traceLoading = ref(false)

watch(currentUser, () => loadData(), { immediate: true })

async function loadData() {
  loading.value = true
  try {
    if (activeTab.value === 'leave') {
      const res = await listMyLeaves(currentUser.value)
      leaves.value = res.data
    } else {
      const res = await listMyPurchases(currentUser.value)
      purchases.value = res.data
    }
  } catch (e: any) {
    showToast('error', '加载单据失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel(record: any) {
  try {
    await cancelLeave(String(record.id))
    showToast('success', '已撤销请假申请')
    await loadData()
  } catch (e: any) {
    showToast('error', '撤销失败: ' + e.message)
  }
}

async function viewTrace(processInstanceId: string, processType: string) {
  if (!processInstanceId) {
    showToast('error', '无流程实例')
    return
  }
  traceLoading.value = true
  traceModalVisible.value = true
  try {
    const res = await getProcessActivities(processInstanceId)
    // 1. 过滤：去掉线和网关
    let list = res.data.filter((a: any) => !['sequenceFlow', 'exclusiveGateway', 'parallelGateway'].includes(a.activityType))
    
    // 2. 预测未来节点
    const isFinished = list.some((a: any) => a.activityType === 'endEvent')
    if (!isFinished && list.length > 0) {
      if (processType === 'leave') {
        const lastAct = list[list.length - 1]
        if (lastAct.activityId === 'startEvent' || lastAct.activityId === 'modifyTask') {
          list.push({ activityName: '部门经理审批', activityType: 'userTask', isFuture: true })
          list.push({ activityName: 'HR审批', activityType: 'userTask', isFuture: true })
          list.push({ activityName: '结束', activityType: 'endEvent', isFuture: true })
        } else if (lastAct.activityId === 'managerTask') {
          list.push({ activityName: 'HR审批', activityType: 'userTask', isFuture: true })
          list.push({ activityName: '结束', activityType: 'endEvent', isFuture: true })
        } else if (lastAct.activityId === 'hrTask') {
          list.push({ activityName: '结束', activityType: 'endEvent', isFuture: true })
        }
      } else if (processType === 'purchase') {
        const hasManager = list.some((a:any) => a.activityId === 'managerTask')
        const hasFinance = list.some((a:any) => a.activityId === 'financeTask')
        const hasExpert = list.some((a:any) => a.activityId === 'expertTask')
        
        if (!hasManager || !hasFinance) {
          list.push({ activityName: '并行并发审批(经理/财务)', activityType: 'userTask', isFuture: true })
        }
        if (!hasExpert) {
          list.push({ activityName: '专家组会签(多实例)', activityType: 'userTask', isFuture: true })
        }
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

const leaveColumns = [
  { title: '请假类型', dataIndex: 'leaveType', key: 'leaveType', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 120 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 120 },
  { title: '天数', dataIndex: 'days', key: 'days', width: 70 },
  { title: '事由', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 140 }
]

const purchaseColumns = [
  { title: '采购物品', dataIndex: 'itemName', key: 'itemName', width: 140 },
  { title: '单价', dataIndex: 'price', key: 'price', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '资金分配事由', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '申请时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 100 }
]
</script>

<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">我的申请记录</h1>
      <p class="page-desc">
        在此处查阅 <a-tag color="blue">{{ currentUser }}</a-tag> 提交的所有类型申请与当前状态进度
      </p>
    </div>

    <a-card>
      <template #extra>
        <a-button size="small" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-tabs v-model:activeKey="activeTab" @change="loadData">
        <!-- 面板：请假单 -->
        <a-tab-pane key="leave" tab="请假申请记录">
          <a-table :columns="leaveColumns" :data-source="leaves" :loading="loading"
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
                            @click="viewTrace(record.processInstanceId, 'leave')">
                    <template #icon><NodeIndexOutlined /></template>
                    轨迹
                  </a-button>
                  <a-popconfirm v-if="record.status === 1"
                                title="确定无理由撤销此请假申请？" @confirm="handleCancel(record)">
                    <a-button type="link" danger size="small">
                      <template #icon><StopOutlined /></template>
                      撤销
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无请假单记录" />
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 面板：采购单 -->
        <a-tab-pane key="purchase" tab="采购申请记录">
          <a-table :columns="purchaseColumns" :data-source="purchases" :loading="loading"
                   :pagination="{ pageSize: 10 }" row-key="id" size="middle">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-badge :status="statusMap[record.status]?.color || 'default'"
                         :text="statusMap[record.status]?.text || '未知'" />
              </template>
              <template v-else-if="column.key === 'createTime'">
                {{ formatDate(record.createTime) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button v-if="record.processInstanceId" type="link" size="small"
                            @click="viewTrace(record.processInstanceId, 'purchase')">
                    <template #icon><NodeIndexOutlined /></template>
                    轨迹
                  </a-button>
                </a-space>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无采购单记录，快去申请测试一下吧！" />
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 审批轨迹弹窗 -->
    <a-modal v-model:open="traceModalVisible" title="📜 流程处理痕迹" :footer="null" width="560px">
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
                {{ act.activityType === 'startEvent' ? '发起节点' : (act.activityType === 'endEvent' ? '结束节点' : '审批流转节点') }}
                <span v-if="act.startTime"> · 到达于: {{ formatDate(act.startTime) }}</span>
                <template v-if="act.endTime"> → 过签于: {{ formatDate(act.endTime) }}</template>
              </div>
              <div class="trace-meta" v-else>
                (未达状态) 模型计算预测辅助线路...
              </div>
              <div v-if="act.comment" class="trace-comment">💬 {{ act.comment }}</div>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-spin>
    </a-modal>
  </div>
</template>
