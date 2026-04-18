<script setup lang="ts">
import { ref, computed, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DashboardOutlined,
  FormOutlined,
  ProfileOutlined,
  AuditOutlined,
  HistoryOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()

// 当前用户
const currentUser = ref('zhangsan')

// 用户列表
const users = [
  { id: 'zhangsan', label: '张三（员工）' },
  { id: 'manager', label: '李经理（部门经理组）' },
  { id: 'manager2', label: '赵副经理（部门经理组）' },
  { id: 'hr', label: '王HR（HR组）' }
]

// 菜单选中 key
const selectedKeys = computed(() => {
  const map: Record<string, string> = {
    '/': 'dashboard',
    '/apply': 'apply',
    '/my-leaves': 'my-leaves',
    '/tasks': 'tasks',
    '/history': 'history'
  }
  return [map[route.path] || 'dashboard']
})

function onMenuClick({ key }: { key: string }) {
  const map: Record<string, string> = {
    dashboard: '/',
    apply: '/apply',
    'my-leaves': '/my-leaves',
    tasks: '/tasks',
    history: '/history'
  }
  router.push(map[key] || '/')
}

// 提供给子组件
provide('currentUser', currentUser)

// Toast 通知
function showToast(type: string, msg: string) {
  if (type === 'success') message.success(msg)
  else if (type === 'error') message.error(msg)
  else message.info(msg)
}
provide('showToast', showToast)
</script>

<template>
  <a-layout class="app-layout" style="min-height: 100vh">
    <!-- 侧边栏 -->
    <a-layout-sider :width="220" theme="light" :trigger="null">
      <div class="sidebar-logo" @click="router.push('/')">
        <div class="logo-icon">F</div>
        <div>
          <div class="logo-text">请假管理系统</div>
          <div class="logo-version">Flowable v7.2.0</div>
        </div>
      </div>

      <a-menu
        mode="inline"
        :selected-keys="selectedKeys"
        class="sidebar-menu"
        style="margin-top: 8px"
        @click="onMenuClick"
      >
        <a-menu-item key="dashboard">
          <template #icon><DashboardOutlined /></template>
          工作台
        </a-menu-item>

        <a-menu-item-group title="请假管理">
          <a-menu-item key="apply">
            <template #icon><FormOutlined /></template>
            请假申请
          </a-menu-item>
          <a-menu-item key="my-leaves">
            <template #icon><ProfileOutlined /></template>
            我的申请
          </a-menu-item>
        </a-menu-item-group>

        <a-menu-item-group title="审批中心">
          <a-menu-item key="tasks">
            <template #icon><AuditOutlined /></template>
            待办审批
          </a-menu-item>
          <a-menu-item key="history">
            <template #icon><HistoryOutlined /></template>
            已办历史
          </a-menu-item>
        </a-menu-item-group>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <!-- 顶栏 -->
      <a-layout-header>
        <span style="font-size: 15px; font-weight: 600; color: #1a1a2e;">
          企业请假管理系统
        </span>
        <div class="user-select-wrapper">
          <UserOutlined />
          <span>当前用户：</span>
          <a-select
            v-model:value="currentUser"
            style="width: 180px"
            size="small"
            :options="users.map(u => ({ value: u.id, label: u.label }))"
          />
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="page-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
