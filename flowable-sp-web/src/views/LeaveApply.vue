<script setup lang="ts">
import { ref, inject } from 'vue'
import { useRouter } from 'vue-router'
import { SendOutlined, UndoOutlined } from '@ant-design/icons-vue'
import { submitLeave } from '../api'

const router = useRouter()
const currentUser = inject('currentUser') as any
const showToast = inject('showToast') as (type: string, message: string) => void

const submitting = ref(false)

const formState = ref({
  leaveType: 'annual',
  startDate: '',
  endDate: '',
  days: 1,
  reason: ''
})

const leaveTypes = [
  { value: 'annual', label: '年假' },
  { value: 'sick', label: '病假' },
  { value: 'personal', label: '事假' }
]

async function handleSubmit() {
  if (!formState.value.startDate || !formState.value.endDate) {
    showToast('error', '请选择请假日期')
    return
  }
  if (!formState.value.reason.trim()) {
    showToast('error', '请填写请假事由')
    return
  }

  submitting.value = true
  try {
    await submitLeave({
      userId: currentUser.value,
      leaveType: formState.value.leaveType,
      startDate: formState.value.startDate,
      endDate: formState.value.endDate,
      days: formState.value.days,
      reason: formState.value.reason
    })
    showToast('success', '🎉 请假申请提交成功！已进入审批流程')
    router.push('/my-leaves')
  } catch (e: any) {
    showToast('error', '提交失败: ' + e.message)
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formState.value = {
    leaveType: 'annual',
    startDate: '',
    endDate: '',
    days: 1,
    reason: ''
  }
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">请假申请</h1>
      <p class="page-desc">填写请假信息，提交后将自动进入审批流程</p>
    </div>

    <a-card title="📝 请假单" style="max-width: 600px">
      <a-form :model="formState" layout="vertical" @finish="handleSubmit">
        <a-form-item label="申请人">
          <a-input :value="currentUser" disabled />
        </a-form-item>

        <a-form-item label="请假类型" required>
          <a-radio-group v-model:value="formState.leaveType">
            <a-radio-button v-for="t in leaveTypes" :key="t.value" :value="t.value">
              {{ t.label }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始日期" required>
              <a-input v-model:value="formState.startDate" type="date" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" required>
              <a-input v-model:value="formState.endDate" type="date" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="请假天数" required>
          <a-input-number v-model:value="formState.days" :min="1" :max="30" style="width: 100%" />
        </a-form-item>

        <a-form-item label="请假事由" required>
          <a-textarea v-model:value="formState.reason" :rows="4" placeholder="请详细描述请假事由..." />
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="submitting">
              <template #icon><SendOutlined /></template>
              提交申请
            </a-button>
            <a-button @click="handleReset">
              <template #icon><UndoOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>
