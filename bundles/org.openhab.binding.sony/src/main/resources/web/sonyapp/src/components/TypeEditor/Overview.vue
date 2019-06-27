<template>
    <div class="overview">
        <div class="title">Overview</div>
        <table>
            <tbody>
                <tr>
                    <td class="label">Service</td>
                    <td><input v-model="typeDefinition.service"/></td>
                </tr>
                <tr>
                    <td class="label">Config Url</td>
                    <td><input v-model="typeDefinition.configUri"/></td>
                </tr>
                <tr>
                    <td class="label">Model Name</td>
                    <td><input v-model="typeDefinition.modelName"/></td>
                </tr>
                <tr>
                    <td></td>
                    <td><span>Use X (case sensitive) for wildcard</span></td>
                </tr>
                <tr>
                    <td class="label">Label</td>
                    <td><input v-model="typeDefinition.label"/></td>
                </tr>
                <tr>
                    <td class="label">Description</td>
                    <td><input v-model="typeDefinition.description"/></td>
                </tr>
            </tbody>
        </table>
    </div>
    
</template>

<script lang="ts">
import { mapState } from 'vuex';
import Vue from 'vue'

export default Vue.extend({
      computed: {
    ...mapState('type', ['typeDefinition']),
  },
})
</script>

<style scoped>
.overview {
    float: left;
    width: 30em;
    margin-right: 2em;
}
    table .label {
        font-weight: bold;
    }
    table span {
        display: block;
        font-size: small;
        height: 2em;
        margin-left: .25em;
    }
</style>
