<template>
  <div class="outline-node">
    <div 
      :class="[
        'flex items-center cursor-pointer rounded-lg py-1.5 px-2 transition-colors',
        { 
          'text-blue-600 bg-blue-50': item.isActive,
          'hover:bg-gray-50': !item.isActive
        }
      ]"
      :style="{ paddingLeft: `${(item.level * 12) + 8}px` }"
    >
      <span 
        v-if="hasChildren" 
        class="mr-2 w-4 h-4 flex items-center justify-center text-gray-400 hover:text-gray-600 cursor-pointer"
        @click.stop="$emit('toggle', item.id)"
      >
        <template v-if="isCollapsed">▶</template>
        <template v-else>▼</template>
      </span>
      <!-- Add a spacer for items without children to maintain alignment -->
      <span v-else class="mr-2 w-4 h-4"></span>
      
      <span class="flex-1 truncate text-sm" @click="$emit('navigate', item.id)">
        {{ item.textContent }}
      </span>
    </div>
    <div v-if="hasChildren && !isCollapsed" class="children-container">
      <OutlineNode 
        v-for="child in item.children" 
        :key="child.id"
        :item="child"
        :collapsed-sections="collapsedSections"
        @toggle="$emit('toggle', $event)"
        @navigate="$emit('navigate', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  collapsedSections: {
    type: Array as () => string[],
    required: true,
  }
});

defineEmits(['toggle', 'navigate']);

const hasChildren = computed(() => props.item.children && props.item.children.length > 0);
const isCollapsed = computed(() => props.collapsedSections.includes(props.item.id));
</script>

<script lang="ts">
// This is required for a recursive component to be able to refer to itself.
export default {
  name: 'OutlineNode'
}
</script>

<style scoped>
.outline-node .mr-2 {
  font-size: 10px;
  transition: transform 0.2s;
  flex-shrink: 0; /* Prevent icons from shrinking */
}
</style> 