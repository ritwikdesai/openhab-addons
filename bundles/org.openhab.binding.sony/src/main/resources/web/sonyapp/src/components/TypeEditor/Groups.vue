<template>
  <div>
    <div class="title">
      <span>Groups</span>
      <div class="actions">
        <button uib-tooltip="Add" v-on:click="addGroup(typeDefinition)">
          <font-awesome-icon icon="plus"/>Add
        </button>
      </div>
    </div>
    <div class="content">
      <table>
        <tbody>
          <tr v-bind:key="index" v-for="(grp, index) in typeDefinition.channelGroups">
            <td>
              <input v-model="grp.name" :disabled="typeDefinition.groupInUse(grp.name)">
            </td>
            <td>
              <input v-model="grp.value">
            </td>
            <td>
              <button
                uib-tooltip="Trash"
                :disabled="typeDefinition.groupInUse(grp.name)"
                v-on:click="deleteGroup(typeDefinition, grp, index)"
              >
                <font-awesome-icon icon="trash"/>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script lang="ts">
import { mapState } from 'vuex';
import Vue from 'vue';
import TypeDefinition from '../../modules/types/TypeDefinition';
import NameValue from '../../modules/types/NameValue';

export default Vue.extend({
  computed: {
    ...mapState('type', ['typeDefinition']),
  },
  methods: {
    addGroup(typeDefinition: TypeDefinition) {
      typeDefinition.channelGroups.unshift(new NameValue('', ''));
    },
    deleteGroup(typeDefinition: TypeDefinition, group: NameValue, idx: number) {
      typeDefinition.channelGroups.splice(idx, 1);
    },
  },
});
</script>

<style scoped>
.title,
.content {
  width: 40em;
}
.content {
  height: 10em;
  overflow: auto;
}

.content table {
  width: 100%;
}

.content table td input {
  width: 100%;
  box-sizing: border-box;
  -webkit-box-sizing: border-box;
  -moz-box-sizing: border-box;
}
</style>

