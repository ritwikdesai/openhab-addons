<template>
  <div class='actions'>
    <FileRead id="loadFile" icon="upload" title="Load File" callback="type/loadFile"/>
    <label v-bind="fileName" class='title'> {{ fileName }}</label>
    <button id="execute" uib-tooltip="Save" v-on:click="saveFile()"><font-awesome-icon icon="save" />Save</button>
  </div>
</template>

<script lang="ts">
import { mapState, mapActions } from "vuex";
import FileRead from "@/components/Common/FileRead.vue";
import Vue from "vue";

export default Vue.extend({
  components: {
    FileRead
  },
  methods: {
    ...mapActions("type", ["saveFile"])
  },
  computed: {
    ...mapState("type", ["fileName","typeDefinition"])
  }
});
</script>

<style scoped>
.actions {
  margin-bottom: .5em;
}
label {
  display: inline-block;
  margin: 0em 1em;
}
</style>

