import Vue from 'vue';
import Vuex from 'vuex';
import DefinitionModule from '@/modules/capabilities';
import TypeModule from '@/modules/editorTypes';

Vue.use(Vuex);

export default new Vuex.Store({
  modules: {
    definition: new DefinitionModule(),
    type: new TypeModule(),
  },
});
