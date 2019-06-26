<template>
  <div class="definitions">
    <Method/>
    <Methods/>
    <Result/>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import Method from '@/components/Definitions/Method.vue';
import Methods from '@/components/Definitions/Methods.vue';
import Result from '@/components/Definitions/Result.vue';

@Component({
  components: {
    Method, Methods, Result,
  },
})
export default class Definitions extends Vue {}
</script>
