import Method from '@/modules/types/Method';

export default class MethodDef {
  public baseUrl: string;
  public modelName: string;
  public serviceName: string;
  public transport: string;
  public method: Method;
  public methodType: string;
  constructor(baseUrl: string, modelName: string, serviceName: string, transport: string, method: Method, methodType: string) {
    this.baseUrl = baseUrl;
    this.modelName = modelName;
    this.serviceName = serviceName;
    this.transport = transport;
    this.method = method;
    this.methodType = methodType;
  }

  public isDuplicateKey(mth: MethodDef): boolean {
    return this.serviceName === mth.serviceName && this.method.command === mth.method.command && this.method.version === mth.method.version;
  }

  public isSameDef(mth: MethodDef): boolean {
    return this.serviceName === mth.serviceName
      && this.method.command === mth.method.command
      && this.method.version === mth.method.version
      && this.method.parms === mth.method.parms
      && this.method.retVals === mth.method.retVals
      && this.methodType === mth.methodType;
  }
}
