<template>
  <div class="typeEditor">
    <Actions/>
    <Groups/>
    <Mappings/>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import Actions from '@/components/TypeEditor/Actions.vue';
import Groups from '@/components/TypeEditor/Groups.vue';
import Mappings from '@/components/TypeEditor/Mappings.vue';

@Component({
  components: {
    Actions,Groups,Mappings
  },
})
export default class Definitions extends Vue {}
</script>
