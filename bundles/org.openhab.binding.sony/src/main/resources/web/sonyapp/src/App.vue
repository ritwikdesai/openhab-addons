<template>
  <div id="app">
    <div id="header">
      <h1>Sony</h1>
    <div id="nav">
      <router-link to="/">Definitions</router-link> |
      <router-link to="/typeeditor">Type Editor</router-link>
    </div>
    <div id="content">
    <router-view/>
    </div>
    </div>
  </div>
</template>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
	margin: 5px;
}

#app h1 {
  padding:0 15px 15px 0;
  margin:0;
  display:table-cell;
}

#nav {
  padding:0;
  margin:0;
  vertical-align: center;
  display: table-cell;
}
#nav a {
  text-decoration: none;
}

#content {
  clear: both;
}
#nav a {
  font-weight: bold;
  color: #2c3e50;
}

#nav a.router-link-exact-active {
  color: #42b983;
}

.even {
	background-color: lavender;
}

tr.diff {
	background-color: orange; 
}


table tr.selected {
    background-color: aquamarine; 
}

.title {
  font-weight: bold;
  background-color: aqua;
  padding: 5px;
  height: 20px;
}
.title .actions {
  float: right;
  margin-right: .25em;
  height: 20px;
}
</style>
