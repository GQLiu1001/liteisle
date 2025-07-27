<template>
  <div class="code-block-wrapper relative" spellcheck="false">
    <div class="code-block-header absolute top-2 right-2 text-xs text-gray-500">
      <div class="language-display px-2 py-1 rounded bg-gray-100/50">{{ language || 'text' }}</div>
    </div>
    <div :class="['language-selector absolute bottom-2 right-2 z-[1] transition-opacity', language ? 'opacity-50 hover:opacity-100' : 'opacity-0 focus-within:opacity-100 group-hover:opacity-100']">
      <input
        type="text"
        v-model="language"
        placeholder="language"
        :style="{ width: `${inputSize}ch`, textAlign: 'center' }"
        class="bg-transparent text-gray-500 text-xs rounded-md py-1 px-2 border border-transparent hover:border-gray-300 focus:outline-none focus:ring-1 focus:ring-blue-400 hover:bg-[rgba(255,255,255,0.1)] focus:bg-[rgba(255,255,255,0.1)] transition-all duration-200"
        @focus="($event.target as HTMLInputElement)?.select()"
      />
    </div>
    <pre><code class="group"><slot></slot></code></pre>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

const props = defineProps<{
  initialLanguage?: string;
}>();

const language = ref(props.initialLanguage || '');

const inputSize = computed(() => {
  const langLength = language.value.length;
  const placeholderLength = 'language'.length;
  // Use the length of the content or placeholder, whichever is longer, and add a small buffer.
  return Math.max(langLength, placeholderLength) + 2;
});
</script>

<style>
.code-block-wrapper {
  position: relative;
  margin: 1em 0;
}

.language-selector {
  position: absolute;
  bottom: 0.5rem;
  right: 0.5rem;
  z-index: 1;
}

.language-selector input {
  background: transparent;
  border: none;
  font-family: 'Fira Code', monospace;
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
  transition: all 0.2s ease;
  border-radius: 4px;
}

.language-selector input:hover,
.language-selector input:focus {
  background: transparent;
  outline: none;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.2);
}

.language-selector input::placeholder {
  color: #9ca3af;
  opacity: 0.7;
}

.code-block-header {
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.code-block-wrapper:hover .code-block-header {
  opacity: 1;
}
</style> 