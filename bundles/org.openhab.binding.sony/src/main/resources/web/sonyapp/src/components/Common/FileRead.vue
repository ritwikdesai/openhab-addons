<template>
  <span class="fileRead">
      <label v-bind:for="id" class="btn">
        <font-awesome-icon v-bind:icon="icon"/>
        <p>{{ title }}</p>
      </label>
      <input v-bind:id="id" type="file" accept=".json" v-on:change="loadFile">
  </span>
</template>

<script lang="ts">
import Vue from 'vue';
export default Vue.extend({
  props: {
    id: String,
    icon: String,
    title: String,
    callback: String,
  },
  methods: {
    loadFile(event: any) {
      const files = event.target.files;
      if (files.length === 1) {
        const reader = new FileReader();
        const store = this.$store;

        reader.onload = (e) => {
          store.commit(this.$props.callback, { file: files[0], result: reader.result});
        };
        reader.readAsText(files[0]);
      }
    },
  },
});
</script>

<style scoped>
input[type="file"] {
  display: none;
}

.btn {
  margin: 0 0 3px 5px;
  display: inline-block;
  border: solid 1px black;
  padding: 3px;
}

.btn p {
  display: inline-block;
  margin: 0 0 0 5px;
}
</style>
