import NameValue from '@/modules/types/NameValue';

export default class Channel {
  public channelId: string;
  public channelGroup: string;
  public mappedChannelId: string;
  public mappedChannelGroup: string;
  public channelType: string;
  public properties: NameValue[];
  public state: NameValue[];

  constructor(channelId: string, mappedChannelId: string, channelType: string, properties: NameValue[], state: NameValue[]) {
    let idx: number = channelId.indexOf('#');
    if (idx >= 0) {
      this.channelId = channelId.substr(idx + 1);
      this.channelGroup = channelId.substr(0, idx);
    } else {
      this.channelId = channelId;
      this.channelGroup = '';
    }

    if (mappedChannelId === undefined) {
      this.mappedChannelId = this.channelId;
      this.mappedChannelGroup = this.channelGroup;
    } else {
      idx = mappedChannelId.indexOf('#');
      if (idx >= 0) {
        this.mappedChannelId = mappedChannelId.substr(idx + 1);
        this.mappedChannelGroup = mappedChannelId.substr(0, idx);
      } else {
        this.mappedChannelId = mappedChannelId;
        this.mappedChannelGroup = this.channelGroup;
      }
    }

    this.channelType = channelType;
    this.properties = properties.sort((a, b) => a.name.localeCompare(b.name));
    this.state = state.sort((a, b) => a.name.localeCompare(b.name));
  }

  public get channelWithGroup(): string {
    return this.channelGroup + '#' + this.channelId;
  }

  public get mappedChannelWithGroup(): string | undefined {
    return this.isMapped() ? (this.mappedChannelGroup + '#' + this.mappedChannelId) : undefined;
  }

  public isMapped(): boolean {
    return this.channelId !== this.mappedChannelId || this.channelGroup !== this.mappedChannelGroup;
  }
}
