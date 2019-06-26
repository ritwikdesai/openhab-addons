<template>
  <div class='parent'>
          <div class="title">
        <span>Channel Mappings</span>
      </div>

    <table class="mappings">
      <thead>
        <tr>
          <td>Channel ID</td>
          <td>Mapped Group</td>
          <td>Mapped ID</td>
          <td colspan="3">Actions</td>
        </tr>
      </thead>
      <tbody>
        <tr v-bind:key="index" v-for="(chl, index) in typeDefinition.channels" v-bind:class="{ 'even' : index%2===0, 'diff': chl.isMapped() }">
          <td>{{ chl.channelWithGroup }}</td>
          <td>
            <select v-model="chl.mappedChannelGroup">
              <option
                v-bind:key="index"
                v-for="(grp, index) in typeDefinition.channelGroups"
                v-bind:value="grp.name"
              >{{grp.value}}</option>
            </select>
          </td>
          <td>
            <input v-model="chl.mappedChannelId" :placeholder="chl.channelId">
          </td>
          <td>
            <button id="props" uib-tooltip="Properties" v-on:click="showProperties(chl)">
              <font-awesome-icon icon="tasks"/>
            </button>
          </td>
          <td>
            <button id="state" uib-tooltip="State" v-on:click="showState(chl)">
              <font-awesome-icon icon="eye"/>
            </button>
          </td>
          <td>
            <button id="info" uib-tooltip="Info" v-on:click="showInfo(chl)">
              <font-awesome-icon icon="info"/>
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <modal name="modal-info" @closed="modalClosed" :height="300">
      <div
        class="title"
        v-if="selectedChannel !== undefined"
      >Manage Properties for {{ selectedChannel.channelId }}</div>
      <div class="modal">
        <table v-if="selectedChannel">
          <tbody>
            <tr>
              <td class="label">Channel Type</td>
              <td class="value">{{ selectedChannel.channelType}}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </modal>

    <modal name="modal-props" @closed="modalClosed" :height="300">
      <div class="title" v-if="selectedChannel !== undefined">
        Manage Properties for {{ selectedChannel.channelId }}
        <div class="actions">
          <button uib-tooltip="Add" v-on:click="addProperty(selectedChannel)">
            <font-awesome-icon icon="plus"/>Add
          </button>
        </div>
      </div>
      <div class="modal">
        <div
          v-if="selectedChannel !== undefined && selectedChannel.properties.length === 0"
        >Press &quot;Add&quot; above to add properties</div>

        <div class="content">
          <table v-if="selectedChannel !== undefined && selectedChannel.properties.length > 0">
            <tbody>
              <tr v-bind:key="index" v-for="(prop, index) in selectedChannel.properties">
                <td class="label">
                  <input v-model="prop.name">
                </td>
                <td class="value">
                  <input v-model="prop.value">
                </td>
                <td>
                  <button
                    uib-tooltip="Trash"
                    v-on:click="deleteProperty(selectedChannel, prop, index)"
                  >
                    <font-awesome-icon icon="trash"/>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </modal>

    <modal name="modal-state" @closed="modalClosed" :height="300">
      <div class="title" v-if="selectedChannel !== undefined">
        Manage State for {{ selectedChannel.channelId }}
        <div class="actions">
          <button uib-tooltip="Add" v-on:click="addState(selectedChannel)">
            <font-awesome-icon icon="plus"/>Add
          </button>
        </div>
      </div>
      <div class="modal">
        <div
          v-if="selectedChannel !== undefined && selectedChannel.state.length === 0"
        >Press &quot;Add&quot; above to add state</div>

        <div class="content">
          <table v-if="selectedChannel !== undefined && selectedChannel.state.length > 0">
            <tbody>
              <tr v-bind:key="index" v-for="(state, index) in selectedChannel.state">
                <td class="label">
                  <input v-model="state.name">
                </td>
                <td class="value">
                  <input v-model="state.value">
                </td>
                <td>
                  <button
                    uib-tooltip="Trash"
                    v-on:click="deleteState(selectedChannel, state, index)"
                  >
                    <font-awesome-icon icon="trash"/>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </modal>
  </div>
</template>

<script lang="ts">
import { mapState } from "vuex";
import Vue from "vue";

import modal from "vue-js-modal";
import NameValue from "../../modules/types/NameValue";
import Channel from "../../modules/types/Channel";


Vue.use(modal);

class MyData {
  selectedChannel?: Channel;
  constructor() {
    this.selectedChannel = undefined;
  }
}

const myData = new MyData();

export default Vue.extend({
  data: function() {
    return myData;
  },
  methods: {
    modalClosed() {
      this.selectedChannel = undefined;
    },
    addState(chl: Channel, state: NameValue, idx: number) {
      chl.state.push(new NameValue("", ""));
    },
    deleteState(chl: Channel, state: NameValue, idx: number) {
      chl.state.splice(idx, 1);
    },
    addProperty(chl: Channel, state: NameValue, idx: number) {
      chl.properties.push(new NameValue("", ""));
    },
    deleteProperty(chl: Channel, state: NameValue, idx: number) {
      chl.properties.splice(idx, 1);
    },
    showProperties(chl: Channel) {
      this.selectedChannel = chl;
      this.$modal.show("modal-props");
    },
    showState(chl: Channel) {
      this.selectedChannel = chl;
      this.$modal.show("modal-state");
    },
    showInfo(chl: Channel) {
      this.selectedChannel = chl;
      this.$modal.show("modal-info");
    }
  },
  computed: {
    ...mapState("type", ["typeDefinition"])
  }
});
</script>

<style scoped>
.modal {
  margin: 20px;
}

.parent>.title {
  margin: 1em 0em;
}

.content {
  height: 190px;
  overflow: auto;
}

table thead td {
  font-weight: bold;
}

.actions {
  margin-bottom: 20px;
}
</style>

