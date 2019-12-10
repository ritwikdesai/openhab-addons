import Method from '@/modules/types/Method';

export default class RestApiMethod {
    public methodName: string;
    public version: string;
    public variation: number;
    public parms: string[];
    public retVals: string[];

    constructor(mthd: Method) {
        this.methodName = mthd.command;
        this.version = mthd.version;
        this.variation = mthd.variation;

        this.parms = mthd.parms;
        this.retVals = mthd.retVals;
    }
}
