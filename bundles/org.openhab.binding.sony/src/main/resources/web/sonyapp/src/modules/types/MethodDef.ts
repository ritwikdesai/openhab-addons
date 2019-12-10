import Method from '@/modules/types/Method';
import { MethodType } from '@/modules/types/MethodType';

export default class MethodDef {
  public baseUrl: string;
  public modelName: string;
  public serviceName: string;
  public serviceVersion: string;
  public transport: string;
  public method: Method;
  public methodType: string;
  constructor(baseUrl: string, modelName: string, serviceName: string, serviceVersion: string, transport: string, method: Method, methodType: string) {
    this.baseUrl = baseUrl;
    this.modelName = modelName;
    this.serviceName = serviceName;
    this.serviceVersion = serviceVersion;
    this.transport = transport;
    this.method = method;
    this.methodType = methodType;
  }

  public isDuplicateKey(mth: MethodDef): boolean {
    return this.serviceName === mth.serviceName && this.method.command === mth.method.command
      && this.method.version === mth.method.version && this.method.variation === mth.method.variation;
  }

  public isSameDef(mth: MethodDef): boolean {
    return this.serviceName === mth.serviceName
      && this.method.command === mth.method.command
      && this.method.version === mth.method.version
      && this.method.variation === mth.method.variation
      && this.arrEquals(this.method.parms, mth.method.parms)
      && this.arrEquals(this.method.retVals, mth.method.retVals)
      && this.methodType === mth.methodType;
  }

  private arrEquals(array1: string[], array2: string[]): boolean {
    const arr1 = array1.sort();
    const arr2 = array2.sort();
    return arr1.length === arr2.length && arr1.every((value, index) => value === arr2[index]);
  }
}
