class TypeState {
  public file?: File = undefined;
  public fileName: string = '';
  public typeDefinition: TypeDefinition = new TypeDefinition();
}

import fs from 'file-saver';
import TypeDefinition from '@/modules/types/TypeDefinition';

export default class TypeModule {
  public namespaced: boolean = true;
  public state: TypeState = new TypeState();
  public actions: any = {

    saveFile(context: any) {
      const blb = new Blob([JSON.stringify(context.state.typeDefinition.export())], { type: 'application/json' });
      fs.saveAs(blb, context.state.fileName);
    },
  };
  public mutations: any = {
    loadFile(state: TypeState, payload: any) {
      const jsonData: any = JSON.parse(payload.result);
      state.file = payload.file;
      state.fileName = payload.file.name;

      if (Array.isArray(jsonData)) {
        state.typeDefinition = TypeDefinition.parse(jsonData[0]);
      } else {
        state.typeDefinition = TypeDefinition.parse(jsonData);
      }
    },

    saveFile(state: TypeState) {
      const blb = new Blob([JSON.stringify(state.typeDefinition.export())], { type: 'application/json' });
      fs.saveAs(blb, state.fileName);
    },
  };
}
