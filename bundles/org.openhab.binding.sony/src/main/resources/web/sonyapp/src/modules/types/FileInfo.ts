import MethodDef from '@/modules/types/MethodDef';

export default class FileInfo {
  public loadedFile: string;
  public methods: MethodDef[];
  public selectedIdx: number;

  constructor() {
    this.loadedFile = '';
    this.methods = [];
    this.selectedIdx = -1;
  }

  public setMethods(defs: MethodDef[]) {
    this.methods = defs;
    this.sortMethods();
  }
  public mergeMethods(defs: MethodDef[]) {
    defs.forEach((def) => {
      this.addMethod(def);
    });
    this.sortMethods();
  }
  private addMethod(def: MethodDef) {
    if (!this.methods.find((mthd) => def.isSameDef(mthd))) {
      this.methods.push(def);
    }
  }

  private sortMethods() {
    this.methods.sort((a, b) => {
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
      if (a.method.command < b.method.command) {
        return -1;
      }
      if (a.method.command > b.method.command) {
        return 1;
      }
      const an = parseFloat(a.method.version);
      const bn = parseFloat(b.method.version);
      return an - bn;
    });
  }
}