<template>
  <div class="mobile-list-card" @click="$emit('click', $event)">
    <div class="mlc-header">
      <span class="mlc-title">{{ title }}</span>
      <div class="mlc-tags"><slot name="tag" /></div>
    </div>
    <div v-if="$slots.default" class="mlc-body"><slot /></div>
    <div v-if="$slots.actions" class="mlc-actions" @click.stop>
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({ title: { type: String, default: '' } })
defineEmits(['click'])
</script>
