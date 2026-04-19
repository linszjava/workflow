<script setup lang="ts">
import { ref, inject } from 'vue'
import { submitPurchase } from '../api'

const currentUser = inject('currentUser') as any
const showToast = inject('showToast') as (type: string, message: string) => void

const form = ref({
  itemName: '',
  price: 0,
  quantity: 1,
  reason: ''
})

const submitLoading = ref(false)

async function handleSubmit() {
  if (!form.value.itemName || !form.value.reason) {
    showToast('error', '请完整填写购买物品和事由')
    return
  }
  if (form.value.price <= 0 || form.value.quantity <= 0) {
    showToast('error', '单价和数量必须大于0')
    return
  }
  
  submitLoading.value = true
  try {
    const payload = {
      ...form.value,
      userId: currentUser.value
    }
    await submitPurchase(payload)
    showToast('success', '采购申请提交成功！(并行网关+会签测试演示)')
    // 重置
    form.value.itemName = ''
    form.value.price = 0
    form.value.quantity = 1
    form.value.reason = ''
  } catch (e: any) {
    showToast('error', '提交失败: ' + e.message)
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <div class="apply-container">
    <div class="page-header">
      <h1 class="page-title">采购申请</h1>
      <p class="page-desc">发起新的采购请求。流程包含「部门经理和财务并行审批」以及「专家组多实例会签」。</p>
    </div>
    
    <a-card class="apply-card">
      <a-form layout="vertical">
        <a-form-item label="当前申请人">
          <a-tag color="blue">{{ currentUser }}</a-tag>
        </a-form-item>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="采购物品">
              <a-input v-model:value="form.itemName" placeholder="例如：MacBook Pro 16寸" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="单价(元)">
              <a-input-number v-model:value="form.price" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="数量">
              <a-input-number v-model:value="form.quantity" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="采购用途/事由">
          <a-textarea v-model:value="form.reason" rows="4" placeholder="请详细描述采购目的..." />
        </a-form-item>
        
        <a-form-item>
          <a-button type="primary" size="large" :loading="submitLoading" @click="handleSubmit" block>
            提交申请
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.apply-container {
  max-width: 800px;
  margin: 0 auto;
}
.apply-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
</style>
