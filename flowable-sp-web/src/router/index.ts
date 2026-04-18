import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('../views/Dashboard.vue'),
      meta: { title: '仪表盘', icon: '📊' }
    },
    {
      path: '/apply',
      name: 'LeaveApply',
      component: () => import('../views/LeaveApply.vue'),
      meta: { title: '请假申请', icon: '📝' }
    },
    {
      path: '/my-leaves',
      name: 'MyLeaves',
      component: () => import('../views/MyLeaves.vue'),
      meta: { title: '我的申请', icon: '📋' }
    },
    {
      path: '/tasks',
      name: 'MyTasks',
      component: () => import('../views/MyTasks.vue'),
      meta: { title: '待办审批', icon: '✅' }
    },
    {
      path: '/history',
      name: 'History',
      component: () => import('../views/History.vue'),
      meta: { title: '已办历史', icon: '📚' }
    }
  ]
})

export default router
