<template>
  <view class="privacy">
    <view class="privacy__header">
      <text class="privacy__title">隐私政策</text>
      <text class="privacy__updated">最后更新：2026-10-14</text>
    </view>
    <view v-for="(section, i) in sections" :key="i" class="privacy__section">
      <text class="privacy__h">{{ section.h }}</text>
      <text v-for="(line, j) in section.body" :key="j" class="privacy__p">
        {{ line }}
      </text>
    </view>
    <view class="privacy__contact">
      <text>联系邮箱：privacy@rocgit.example.com</text>
    </view>
  </view>
</template>

<script setup>
import { reactive } from 'vue';

const sections = reactive([
  { h: '一、我们收集的信息', body: ['账号信息：手机号、微信 OpenID、昵称/头像。', '业务信息：项目、文件、任务、研讨记录、版本历史、登录日志（30 天）。', '设备与日志：登录时间、IP（30 天后自动脱敏为 0.0.0.0）、User-Agent。'] },
  { h: '二、信息使用', body: ['仅用于提供平台核心功能、保障安全、配合法律法规调查。', '不会用于推送广告、不会出售或交换个人信息。'] },
  { h: '三、信息共享', body: ['您主动分享文件时向访客可见。', '法律法规要求时依法配合。', '与签订保密协议的服务商（如短信、对象存储）。'] },
  { h: '四、您的权利', body: ['查阅 → 我的 → 账号与安全 → 数据导出。', '更正 → 我的 → 个人信息。', '删除 → 我的 → 账号与安全 → 注销账号。'] },
  { h: '五、信息存储', body: ['数据库：自建 MySQL 8（主从热备）。', '文件：本地或阿里云 OSS（用户可配置）。', '备份：每日全量 + binlog，至少保留 30 天。', '加密：密码 bcrypt；JWT HS256；HTTPS 传输。'] },
  { h: '六、Cookie 与本地存储', body: ['仅使用 uni-app Storage 存储登录态。', '微信小程序使用系统级 Storage；不跨平台共享。'] },
  { h: '七、未成年人保护', body: ['本平台不面向 14 周岁以下未成年人。'] },
  { h: '八、政策更新', body: ['重大变更将通过站内通知告知。', '继续使用即视为接受变更。'] }
]);
</script>

<style scoped>
.privacy { padding: 32rpx 28rpx 60rpx; background: #f8fafc; min-height: 100vh; }
.privacy__header { text-align: center; padding: 24rpx 0; }
.privacy__title { display: block; font-size: 40rpx; font-weight: 700; color: #0f172a; }
.privacy__updated { display: block; font-size: 24rpx; color: #94a3b8; margin-top: 8rpx; }
.privacy__section {
  background: #fff; border-radius: 16rpx; padding: 24rpx 28rpx; margin-top: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(15, 23, 42, 0.04);
}
.privacy__h { display: block; font-size: 30rpx; font-weight: 600; color: #0f172a; margin-bottom: 12rpx; }
.privacy__p { display: block; font-size: 26rpx; color: #475569; line-height: 1.7; margin-bottom: 8rpx; }
.privacy__contact { text-align: center; color: #6366f1; font-size: 26rpx; margin-top: 40rpx; }
</style>
