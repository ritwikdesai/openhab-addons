<template>
  <div id="leftside">
    <div>
      <label for="baseUrl">Base URL:</label>
      <input type="text" id="baseUrl" v-model="currMethod.baseUrl">
    </div>
    <div>
      <label for="transport">Transport:</label>
      <select id="transport" v-model="currMethod.transport">
        <option value="auto">Auto</option>
        <option value="websocket:jsonizer">Websocket</option>
        <option value="xhrpost:jsonizer">HTTP</option>
      </select>
    </div>
    <div>
      <label for="service">Service:</label>
      <input type="text" id="service" v-model="currMethod.service">
    </div>
    <div>
      <label for="command">Command:</label>
      <input type="text" id="command" v-model="currMethod.command">
    </div>
    <div>
      <label for="version">Version:</label>
      <input type="text" id="version" v-model="currMethod.version">
    </div>
    <div>
      <label for="parms">Parms:</label>
      <input type="text" id="parms" v-model="currMethod.parms">
    </div>
    <div>
      <button id="execute" uib-tooltip="Run Command" v-on:click="runCommand()"><font-awesome-icon icon="running" />Execute</button>
    </div>
  </div>
</template>

<script lang="ts">
import { mapState, mapActions } from 'vuex';
import Vue from 'vue';
export default Vue.extend({
  computed: {
    ...mapState('definition', [ 'currMethod' ]),
  },
  methods: {
    ...mapActions('definition', ['runCommand'] ),
  },
});
</script>

<style scoped>
#leftside {
  float: left;
  width: 50em;
}

#baseUrl {
  width: 20em;
}

#service {
  width: 7em;
}

#version {
  width: 4em;
}

#parms {
  width: 35em;
}
label {
  display: inline-block;
  width: 7em;
}

button svg {
  margin-right: .5em;
}
</style>
