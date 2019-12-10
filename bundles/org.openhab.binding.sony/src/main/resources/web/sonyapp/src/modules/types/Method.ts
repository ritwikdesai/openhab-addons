export default class Method {
  public baseUrl: string;
  public service: string;
  public transport: string;
  public command: string;
  public version: string;
  public variation: number;
  public parms: string[];
  public retVals: string[];

  constructor(baseUrl?: string, service?: string, transport?: string, command?: string, version?: string, variation?: number, parms?: string[], retVals?: string[]) {
    this.baseUrl = baseUrl === undefined ? 'http://192.168.1.167/sony' : baseUrl;
    this.service = service === undefined ? 'service' : service;
    this.transport = transport === undefined ? 'auto' : transport;
    this.command = command === undefined ? 'getPowerStatus' : command;
    this.version = version === undefined ? '1.1' : version;
    this.variation = variation === undefined ? 0 : variation;

    const myParms = parms === undefined ? undefined : parms.filter((e) => e !== undefined && e !== null && e !== '');
    this.parms = myParms === undefined ? [] : myParms;

    const myVals = retVals === undefined ? undefined : retVals.filter((e) => e !== undefined && e !== null && e !== '');
    this.retVals = myVals === undefined ? [] : myVals;
  }
}
