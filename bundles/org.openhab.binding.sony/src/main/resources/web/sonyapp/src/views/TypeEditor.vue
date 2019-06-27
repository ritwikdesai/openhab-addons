<template>
  <div class="typeEditor">
    <Actions/>
    <Overview/>
    <Groups/>
    <Mappings/>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import Overview from '@/components/TypeEditor/Overview.vue';
import Actions from '@/components/TypeEditor/Actions.vue';
import Groups from '@/components/TypeEditor/Groups.vue';
import Mappings from '@/components/TypeEditor/Mappings.vue';

@Component({
  components: {
    Overview, Actions, Groups, Mappings,
  },
})
export default class Definitions extends Vue {}
</script>