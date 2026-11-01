<template>
  <PageShell title="创意工坊">
    <MenuItem size="large" title="剧本市集" icon="◇">
      <div class="flex flex-col items-center justify-center gap-2 py-20">
        <p class="text-sm font-bold text-white/70">创意工坊尚未接入后端</p>
        <p class="text-xs text-white/40">剧本市集与安装功能将在接入后端后开放，当前可直接用剧本编辑器自建剧本。</p>
      </div>
    </MenuItem>
  </PageShell>
</template>

<script setup lang="ts">
import PageShell from "../components/ui/PageShell.vue";
import MenuItem from "../components/settings/MenuItem.vue";
</script>
