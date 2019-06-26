<template>
  <div id="rightside">
    <div>
      <FileRead id='loadFile' icon='upload' title='Load File' callback='definition/loadFile'/>
      <FileRead id='mergeFile' icon='object-ungroup' title='Merge File' callback='definition/mergeFile'/>
      <span class='title'>{{ file.loadedFile }}</span>

    </div>
    <div id="methods">
      <table>
        <tbody>
          <tr
            v-for="(mthd, index) in file.methods"
            v-bind:key="index"
            v-bind:class="{ 'even' : index%2===0, 'selected': file.selectedIdx === index, 'diff': index>0 && file.methods[index].isDuplicateKey(file.methods[index-1])}"
            v-on:click="selectMethod(index)"
          >
            <td>{{ mthd.serviceName }}</td>
            <td>{{ mthd.method.command }}</td>
            <td>{{ mthd.method.version }}</td>
            <td>{{ mthd.method.parms }}</td>
            <td>{{ mthd.method.retVals }}</td>
            <td>{{ mthd.methodType}}</td>
            <td>{{ mthd.modelName }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import { mapState } from 'vuex';
import FileRead from '@/components/Common/FileRead.vue';

export default Vue.extend({
  components: {
    FileRead,
  },
  computed: {
    ...mapState('definition', ['file']),
  },
  methods: {
    selectMethod(idx: number) {
      this.$store.commit('definition/selectMethod', idx);
    },
  },
});
</script>

<style scoped>
#methods {
  border: solid 1px black;
  padding: 5px;
  margin-top: 5px;
  overflow: auto;
  height: 20em;
}

#methods td {
  white-space: nowrap;
  padding: 0px 2px;
  border: solid 1px lightgray;
}

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

.title {
  margin-left: .5em
}
</style>
