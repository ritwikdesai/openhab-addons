import Vue from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';

import { library } from '@fortawesome/fontawesome-svg-core';
import { faUpload, faObjectUngroup, faRunning, faSave, faEye, faTasks, faInfo, faTrash, faPlus } from '@fortawesome/free-solid-svg-icons';
library.add(faUpload, faObjectUngroup, faRunning, faSave, faEye, faTasks, faInfo, faTrash, faPlus);

import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
Vue.component('font-awesome-icon', FontAwesomeIcon);

Vue.config.productionTip = false;

new Vue({
  router,
  store,
  render: (h) => h(App),
}).$mount('#app');
