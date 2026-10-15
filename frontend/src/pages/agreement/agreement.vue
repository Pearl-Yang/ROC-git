<template>
  <view class="agreement">
    <view class="agreement__header">
      <text class="agreement__title">用户协议</text>
      <text class="agreement__updated">最后更新：2026-10-14</text>
    </view>
    <view v-for="(section, i) in sections" :key="i" class="agreement__section">
      <text class="agreement__h">{{ section.h }}</text>
      <text v-for="(line, j) in section.body" :key="j" class="agreement__p">
        {{ line }}
      </text>
    </view>
    <view class="agreement__footer">
      <button class="agreement__btn" @click="onAccept">我已阅读并同意</button>
    </view>
  </view>
</template>

<script setup>
import { reactive } from 'vue';

const sections = reactive([
  { h: '一、协议接受', body: ['使用 ROC-git 平台即视为您已阅读并同意本协议全部条款。'] },
  { h: '二、账号与认证', body: ['支持微信登录与手机号+验证码登录两种方式。', '您应妥善保管凭证；同一手机号仅可绑定一个账号。'] },
  { h: '三、用户行为规范', body: ['不得上传违反国家法律法规的内容。', '不得上传侵犯他人隐私、版权或商业机密的内容。'] },
  { h: '四、知识产权', body: ['您上传内容的知识产权归您或您的合法授权方所有。', '平台对您上传内容不承担审查责任，但有权依法律法规协助调查。'] },
  { h: '五、隐私保护', body: ['我们仅收集为您提供服务所必需的信息（详见《隐私政策》）。', '您有权随时查看、更正、删除您的个人信息。'] },
  { h: '六、服务变更与终止', body: ['我们保留随时修改或终止本平台服务的权利。', '注销账号：我的 → 账号与安全 → 注销账号；30 天后数据将被永久删除。'] },
  { h: '七、免责', body: ['因不可抗力导致的服务中断，平台不承担责任。'] },
  { h: '八、争议解决', body: ['本协议适用中华人民共和国法律。', '争议提交平台运营方所在地有管辖权的人民法院解决。'] }
]);

const onAccept = () => {
  uni.setStorageSync('agreement_accepted_v1', Date.now());
  uni.showToast({ title: '已同意协议', icon: 'success' });
  setTimeout(() => uni.navigateBack(), 600);
};
</script>

<style scoped>
.agreement {
  padding: 32rpx 28rpx 120rpx;
  background: #f8fafc;
  min-height: 100vh;
}
.agreement__header { text-align: center; padding: 24rpx 0; }
.agreement__title { display: block; font-size: 40rpx; font-weight: 700; color: #0f172a; }
.agreement__updated { display: block; font-size: 24rpx; color: #94a3b8; margin-top: 8rpx; }
.agreement__section {
  background: #fff; border-radius: 16rpx; padding: 24rpx 28rpx; margin-top: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(15, 23, 42, 0.04);
}
.agreement__h { display: block; font-size: 30rpx; font-weight: 600; color: #0f172a; margin-bottom: 12rpx; }
.agreement__p { display: block; font-size: 26rpx; color: #475569; line-height: 1.7; margin-bottom: 8rpx; }
.agreement__footer { position: fixed; left: 0; right: 0; bottom: 0; padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -4rpx 16rpx rgba(15, 23, 42, 0.06); }
.agreement__btn { background: #6366f1; color: #fff; border-radius: 999rpx; font-size: 30rpx; }
</style>
