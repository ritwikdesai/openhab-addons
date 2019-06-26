import Vue from 'vue';
import Router from 'vue-router';
import Definitions from './views/Definitions.vue';
import TypeEditor from './views/TypeEditor.vue';

Vue.use(Router);

export default new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/',
      name: 'definitions',
      component: Definitions,
    },
    {
      path: '/typeeditor',
      name: 'typeeditor',
      component: TypeEditor,
    },
  ],
});
