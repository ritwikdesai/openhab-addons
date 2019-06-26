<template>
  <div>
    <label id="resultsLabel" for="results">Results:</label>
    <textarea id="results" v-model="results" readonly></textarea>
  </div>
</template>

<script lang="ts">
import { mapState } from 'vuex';
import Vue from 'vue';
export default Vue.extend({
  computed: {
    ...mapState('definition', ['results']),
  },
});
</script>

<style scoped>
#results {
  clear: both;
  display: block;
  width: 100em;
  height: 20em;
}
</style>
