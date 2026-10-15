<template>
  <view v-if="visible" class="sheet-mask" @tap="onMaskClick" :style="{ zIndex: zIndex }">
    <view class="sheet-card" :class="[roundedTop ? 'sheet-card-top' : 'sheet-card-bottom']" :style="cardStyle" @tap.stop="noop">
      <!-- drag handle for bottom sheets -->
      <view v-if="roundedTop && showHandle" class="sheet-handle"></view>
      <slot />
    </view>
  </view>
</template>

<script>
/**
 * Sheet - 统一底部弹窗容器
 *  - 默认从底部 slide-up（roundedTop: false）
 *  - 也支持顶部弹出（roundedTop: true）
 *  - 黑色 40% 遮罩
 *  - 点击遮罩关闭
 */
export default {
  name: 'RocSheet',
  props: {
    visible: { type: Boolean, default: false },
    roundedTop: { type: Boolean, default: false },
    showHandle: { type: Boolean, default: true },
    maskClosable: { type: Boolean, default: true },
    maxHeight: { type: String, default: '85vh' },
    zIndex: { type: Number, default: 50 }
  },
  emits: ['update:visible', 'close'],
  computed: {
    cardStyle() {
      return `max-height:${this.maxHeight};`
    }
  },
  methods: {
    onMaskClick() {
      if (this.maskClosable) {
        this.$emit('update:visible', false)
        this.$emit('close')
      }
    },
    noop() {}
  }
}
</script>

<style scoped>
.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  animation: sheetFadeIn 0.2s ease;
}
@keyframes sheetFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.sheet-card {
  width: 100%;
  max-width: 480px;
  background: #FFFFFF;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: sheetSlideUp 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.sheet-card-bottom {
  border-top-left-radius: 24px;
  border-top-right-radius: 24px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom));
}
.sheet-card-top {
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
  margin-top: 24px;
  align-self: flex-start;
  width: 100%;
}
@keyframes sheetSlideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.sheet-handle {
  width: 40px;
  height: 4px;
  border-radius: 999px;
  background: #E2E8F0;
  margin: 8px auto 4px;
}
</style>
