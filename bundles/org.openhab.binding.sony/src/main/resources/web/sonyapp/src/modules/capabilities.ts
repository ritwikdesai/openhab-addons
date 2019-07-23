import axios from 'axios';
import swal from 'sweetalert2';
import Method from '@/modules/types/Method';
import FileInfo from '@/modules/types/FileInfo';
import MethodDef from '@/modules/types/MethodDef';
import { MethodType } from '@/modules/types/MethodType';

class DefinitionState {
  public currMethod: Method;
  public results: string;
  public file: FileInfo;
  constructor() {
    this.currMethod = new Method();
    this.results = '';
    this.file = new FileInfo();
  }
}

const getMethods = (jsonData: any): MethodDef[] => {
  const defs: MethodDef[] = [];
  jsonData.services.forEach((srv: any) => {
    srv.methods.forEach((mthd: any) => defs.push(new MethodDef(jsonData.baseURL, jsonData.modelName, srv.serviceName, srv.transport,
      new Method(mthd.baseUrl, mthd.service, mthd.transport, mthd.methodName, mthd.version, mthd.parms, mthd.retVals), MethodType.Method)));
    srv.notifications.forEach((mthd: any) => defs.push(new MethodDef(jsonData.baseURL, jsonData.modelName, srv.serviceName, srv.transport,
      new Method(mthd.baseUrl, mthd.service, mthd.transport, mthd.methodName, mthd.version, mthd.parms, mthd.retVals), MethodType.Notification)));
  });
  return defs;
};

export default class DefinitionModule {
  public namespaced: boolean = true;
  public state: DefinitionState = new DefinitionState();
  public mutations: any = {
    showResults(state: DefinitionState, res: string) {
      state.results = res;
    },
    selectMethod(state: DefinitionState, idx: number) {
      state.file.selectedIdx = idx;

      if (idx < state.file.methods.length) {
        const mthd = state.file.methods[idx];
        if (mthd.methodType === MethodType.Method) {
          state.currMethod.baseUrl = mthd.baseUrl;
          state.currMethod.service = mthd.serviceName;
          state.currMethod.transport = mthd.transport;
          state.currMethod.command = mthd.method.command;
          state.currMethod.version = mthd.method.version;
          state.currMethod.parms = mthd.method.parms;
        }
      }
    },
    loadFile(state: DefinitionState, payload: any) {
      const jsonData = JSON.parse(payload.result);
      const defs = getMethods(jsonData);
      state.file.selectedIdx = -1;
      state.file.loadedFile = jsonData.modelName;
      state.file.setMethods(defs);
      swal.fire('Info', `Loaded ${defs.length} methods`);
    },
    mergeFile(state: DefinitionState, payload: any) {
      const jsonData = JSON.parse(payload.result);
      if (!state.file.loadedFile.includes(jsonData.modelName)) {
        const defs = getMethods(jsonData);
        state.file.selectedIdx = -1;
        state.file.mergeMethods(defs);

        if (state.file.loadedFile === '') {
          state.file.loadedFile = jsonData.modelName;
        } else {
          state.file.loadedFile = state.file.loadedFile + ',' + jsonData.modelName;
        }
        swal.fire('Info', `Loaded ${defs.length} methods`);
      } else {
        swal.fire('Info', `Already loaded ${jsonData.modelName} methods`);
      }
    },
  };

  public actions: any = {
    runCommand(context: any) {
      context.commit('showResults', 'waiting...');
      axios.post('/sony/app/execute', {
        baseUrl: context.state.currMethod.baseUrl,
        serviceName: context.state.currMethod.service,
        transport: context.state.currMethod.transport,
        command: context.state.currMethod.command,
        version: context.state.currMethod.version,
        parms: context.state.currMethod.parms,
      }).then((res: any) => {
        if (res.data.success === true) {
          context.commit('showResults', res.data.results);
        } else {
          context.commit('showResults', res.data.message);
        }
      }, (res: any) => {
        const msg: string = res.response.status + ' ' + res.response.statusText;
        swal.fire('Error', msg, 'error');
        context.commit('showResults', msg);
      });
    },
  };
}
