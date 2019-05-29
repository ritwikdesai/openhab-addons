var underscore = angular.module('underscore', []);
underscore.factory('_', [ '$window', function($window) {
    return $window._; // assumes underscore has already been loaded on the page
} ]);

angular.module('sony', [ 'ui.bootstrap', 'underscore', 'ngSanitize', 'ui.select' ]).controller('SonyCtrl', sonyController);

sonyController.$inject = [ '$scope', '$http', '$timeout', '$window', 'orderByFilter', '$uibModal', '_' ];
function sonyController($scope, $http, $timeout, $window, orderBy, $uibModal, _) {
    var ctrl = this;

    ctrl.baseUrl = "http://192.168.1.167/sony";
    ctrl.service = "system";
    ctrl.transport = "auto";
    ctrl.command = "getPowerStatus";
    ctrl.version = "1.1";
    ctrl.parms = "";
    ctrl.results = "";
    ctrl.loadedFile = undefined;
    ctrl.methods = [];
    ctrl.selectedIdx = -1;

    ctrl.runCommand = function() {
        ctrl.results = "waiting...";
        $http.post("app/execute", {
            baseUrl : ctrl.baseUrl,
            serviceName : ctrl.service,
            transport : ctrl.transport,
            command : ctrl.command,
            version : ctrl.version,
            parms : ctrl.parms
        }).then(function(response) {
            if (response.data.success === true) {
                ctrl.results = response.data.results;
            } else {
                ctrl.results = response.data.message;
            }
        }, function(response) {
            $.jGrowl(response.status + " " + response.statusText, {
                theme : 'jgrowl-error'
            });
        })
    }

    var sortMethods = function() {
        ctrl.methods.sort(function (a, b) {
            if (a.serviceName < b.serviceName) {
                return -1;
            }
            if (a.serviceName > b.serviceName) {
                return 1;
            }
            if (a.methodType < b.methodType) {
                return -1;
            }
            if (a.methodType > b.methodType) {
                return 1;
            }
            if (a.method.methodName < b.method.methodName) {
                return -1;
            }
            if (a.method.methodName > b.method.methodName) {
                return 1;
            }
            var an = parseFloat(a.method.version);
            var bn = parseFloat(b.method.version);
            return an-bn;            
        });
    }
    
    var addMethods = function(data) {
        var jsonData = JSON.parse(data);
        if (ctrl.loadedFile == undefined) {
            ctrl.loadedFile = jsonData.modelName;
        } else {
            ctrl.loadedFile = ctrl.loadedFile + "," + jsonData.modelName;    
        }
        
        var ct = 0;
        for (var i = 0; i < jsonData.services.length; i++) {
            var srv = jsonData.services[i];
            
            for (var j = 0; j < srv.methods.length; j++) {
                var found = false;
                for(var k=0;k < ctrl.methods.length; k++) {
                    if (ctrl.methods[k].serviceName == srv.serviceName
                       && ctrl.methods[k].method.methodName == srv.methods[j].methodName
                       && ctrl.methods[k].method.version == srv.methods[j].version
                       && ctrl.methods[k].method.parms.join() == srv.methods[j].parms.join()
                       && ctrl.methods[k].method.retVals.join() == srv.methods[j].retVals.join()
                       && ctrl.methods[k].methodType == "M"
                    ) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ct++;
                    ctrl.methods.push({
                        baseUrl: jsonData.baseURL,
                        modelName: jsonData.modelName,
                        serviceName : srv.serviceName,
                        transport: srv.transport,
                        method : srv.methods[j],
                        methodType: "M"
                    });
                }
            }
            for (var j = 0; j < srv.notifications.length; j++) {
                var found = false;
                for(var k=0;k < ctrl.methods.length; k++) {
                    if (ctrl.methods[k].serviceName == srv.serviceName
                       && ctrl.methods[k].method.methodName == srv.notifications[j].methodName
                       && ctrl.methods[k].method.version == srv.notifications[j].version
                       && ctrl.methods[k].method.parms.join() == srv.notifications[j].parms.join()
                       && ctrl.methods[k].method.retVals.join() == srv.notifications[j].retVals.join()
                       && ctrl.methods[k].methodType == "N"
                    ) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ct++;
                    ctrl.methods.push({
                        baseUrl: jsonData.baseURL,
                        modelName: jsonData.modelName,
                        serviceName : srv.serviceName,
                        transport: srv.transport,
                        method : srv.notifications[j],
                        methodType: "N"
                    });
                }
            }
        }        
        $.jGrowl("Added " + ct + " methods");
    }
    ctrl.loadFile = function(element) {
        var itemsFile = element.files[0];
        var reader = new FileReader();

        reader.onload = function(e) {
            $scope.$apply(function() {
                var data = e.target.result;
                ctrl.loadedFile = undefined;
                ctrl.methods = [];
                addMethods(data);
                sortMethods();
                element.value = "";
            });
        };
        reader.readAsText(itemsFile);
    };

    ctrl.mergeFile = function(element) {
        var itemsFile = element.files[0];
        var reader = new FileReader();

        reader.onload = function(e) {
            $scope.$apply(function() {
                var data = e.target.result;
                addMethods(data);
                sortMethods();
                element.value = "";
            });
        };
        reader.readAsText(itemsFile);
    };

    ctrl.selectMethod = function(idx) {
        ctrl.selectedIdx = idx;
        var mthd = ctrl.methods[idx];
        if (mthd.methodType == "M") {
            ctrl.baseUrl = mthd.baseUrl;
            ctrl.service = mthd.serviceName;
            ctrl.transport = mthd.transport;
            ctrl.command = mthd.method.methodName;
            ctrl.version = mthd.method.version;
            ctrl.parms = mthd.method.parms.join();
        }
    }
};
