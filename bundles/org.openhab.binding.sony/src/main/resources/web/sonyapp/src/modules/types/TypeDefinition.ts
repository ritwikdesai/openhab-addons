import NameValue from '@/modules/types/NameValue';
import Channel from '@/modules/types/Channel';

export default class TypeDefinition {
  public static parse(data: any): TypeDefinition {
    const service: string = data.service;
    const configUri: string = data.configUri;
    const modelName: string = data.modelName;
    const label: string = data.label;
    const description: string = data.description;
    const channelGroups: NameValue[] = [];
    const channels: Channel[] = [];

    for (const key of Object.keys(data.channelGroups)) {
      channelGroups.push(new NameValue(key, data.channelGroups[key]));
    }

    for (const chl of data.channels) {
      const channelId: string = chl.channelId;
      const mappedChannelId: string = chl.mappedChannelId === chl.channelId ? undefined : chl.mappedChannelId;
      const channelType: string = chl.channelType;
      const properties: NameValue[] = [];
      const state: NameValue[] = [];

      for (const key of Object.keys(chl.properties)) {
        properties.push(new NameValue(key, chl.properties[key]));
      }

      for (const key of Object.keys(chl.state)) {
        state.push(new NameValue(key, chl.state[key]));
      }

      channels.push(new Channel(channelId, mappedChannelId, channelType, properties, state));
    }
    return new TypeDefinition(service, configUri, modelName, label, description, channelGroups, channels);
  }

  public service: string;
  public configUri: string;
  public modelName: string;
  public label: string;
  public description: string;
  public channelGroups: NameValue[];
  public channels: Channel[];

  constructor(service?: string, configUri?: string, modelName?: string, label?: string, description?: string, channelGroups?: NameValue[], channels?: Channel[]) {
    this.service = service === undefined ? '' : service;
    this.configUri = configUri === undefined ? '' : configUri;
    this.modelName = modelName === undefined ? '' : modelName;
    this.label = label === undefined ? '' : label;
    this.description = description === undefined ? '' : description;
    this.channelGroups = (channelGroups === undefined ? [] : channelGroups).sort((a, b) => a.name.localeCompare(b.name));
    this.channels = (channels === undefined ? [] : channels).sort((a, b) => a.channelWithGroup.localeCompare(b.channelWithGroup));
  }


  public groupInUse(grpName: string): boolean {
    for (const chl of this.channels) {
      if (chl.mappedChannelGroup === grpName) {
        return true;
      }
    }
    return false;
  }

  public export(): any {
    const cGroups: any = {};
    for (const nv of this.channelGroups) {
      cGroups[nv.name] = nv.value;
    }

    const chls: any[] = [];
    for (const chl of this.channels) {
      const state: any = {};
      for (const nv of chl.state) {
        state[nv.name] = nv.value;
      }

      const properties: any = {};
      for (const nv of chl.properties) {
        properties[nv.name] = nv.value;
      }

      chls.push({
        channelId: chl.channelWithGroup,
        mappedChannelId: chl.mappedChannelWithGroup,
        channelType: chl.channelType,
        properties,
        state,
      });
    }

    return {
      service: this.service,
      configUri: this.configUri,
      modelName: this.modelName,
      label: this.label,
      description: this.description,
      channelGroups: cGroups,
      channels: chls,
    };
  }
}
