import RestApiMethod from '@/modules/types/RestApiMethod';
import MethodDef from '@/modules/types/MethodDef';
import { MethodType } from '@/modules/types/MethodType';

export default class RestApi {
    public serviceName: string;
    public version: string;
    public methods: RestApiMethod[] = [];
    public notifications: RestApiMethod[] = [];

    constructor(methodDef: MethodDef) {
        this.serviceName = methodDef.serviceName;
        this.version = methodDef.serviceVersion;

        if (methodDef.method !== undefined) {
            if (methodDef.methodType === MethodType.Method) {
                this.methods.push(new RestApiMethod(methodDef.method));
            } else {
                this.notifications.push(new RestApiMethod(methodDef.method));
            }
        }
    }
}
