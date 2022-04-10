import { Plugin, EditorState } from 'prosemirror-state';
import { EditorView, NodeView } from 'prosemirror-view';
import { Node } from 'static/toast-editor/editor/types/prosemirror-model';

import { CustomParserMap } from 'static/toast-editor/editor/types/toastmark';
import { CustomHTMLRenderer } from 'static/toast-editor/editor/types/editor';
import { Emitter } from 'static/toast-editor/editor/types/event';
import { ToMdConvertorMap } from 'static/toast-editor/editor/types/convertor';
import { Dispatch, Payload, DefaultPayload } from 'static/toast-editor/editor/types/spec';
import { ToolbarItemOptions } from 'static/toast-editor/editor/types/ui';

export type PluginProp = (eventEmitter?: Emitter) => Plugin;

export type PluginNodeViews = (
  node: Node,
  view: EditorView,
  getPos: () => number,
  eventEmitter: Emitter
) => NodeView;

type NodeViewPropMap = Record<string, PluginNodeViews>;

export type CommandFn<T = DefaultPayload> = (
  payload: Payload<T>,
  state: EditorState,
  dispatch: Dispatch,
  view: EditorView
) => boolean;
export type PluginCommandMap = Record<string, CommandFn>;

interface PluginToolbarItem {
  groupIndex: number;
  itemIndex: number;
  item: string | ToolbarItemOptions;
}

export interface PluginInfo {
  toHTMLRenderers?: CustomHTMLRenderer;
  toMarkdownRenderers?: ToMdConvertorMap;
  markdownPlugins?: PluginProp[];
  wysiwygPlugins?: PluginProp[];
  wysiwygNodeViews?: NodeViewPropMap;
  markdownCommands?: PluginCommandMap;
  wysiwygCommands?: PluginCommandMap;
  toolbarItems?: PluginToolbarItem[];
  markdownParsers?: CustomParserMap;
}

export interface PluginInfoResult {
  toHTMLRenderers: CustomHTMLRenderer;
  toMarkdownRenderers: ToMdConvertorMap;
  mdPlugins: PluginProp[];
  wwPlugins: PluginProp[];
  wwNodeViews: NodeViewPropMap;
  mdCommands: PluginCommandMap;
  wwCommands: PluginCommandMap;
  toolbarItems: PluginToolbarItem[];
  markdownParsers: CustomParserMap;
}
