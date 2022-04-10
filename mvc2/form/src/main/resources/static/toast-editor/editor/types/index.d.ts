// Type definitions for TOAST UI Editor v3.1.3
// TypeScript Version: 4.2.3
import {
  EditorCore,
  Editor,
  Viewer,
  EditorOptions,
  ViewerOptions,
  ExtendedAutolinks,
  LinkAttributes,
  Sanitizer,
  EditorType,
  PreviewStyle,
  EventMap,
  HookMap,
  WidgetStyle,
  WidgetRuleMap,
  WidgetRule,
  PluginContext,
  I18n,
  CustomHTMLRenderer,
  HTMLMdNodeConvertor,
  HTMLMdNodeConvertorMap,
} from 'static/toast-editor/editor/types/editor';
import './toastui-editor-viewer';

// @ts-ignore
export {
  MdNode,
  MdNodeType,
  ListMdNode,
  ListItemMdNode,
  TableMdNode,
  TableCellMdNode,
  CodeBlockMdNode,
  LinkMdNode,
  ListData,
  HeadingMdNode,
  CodeMdNode,
  HTMLConvertorMap,
} from '/static/toast-editor/editor/types/toastmark.ts';
export { ToMdConvertorMap } from 'static/toast-editor/editor/types/convertor';
export { Emitter, Handler } from 'static/toast-editor/editor/types/event';
export {
  EditorOptions,
  ViewerOptions,
  ExtendedAutolinks,
  LinkAttributes,
  Sanitizer,
  EditorType,
  PreviewStyle,
  EventMap,
  HookMap,
  WidgetStyle,
  WidgetRuleMap,
  WidgetRule,
  PluginContext,
  I18n,
  CustomHTMLRenderer,
  HTMLMdNodeConvertor,
  HTMLMdNodeConvertorMap,
};
export { Dispatch } from 'static/toast-editor/editor/types/spec';
export { PluginInfo, PluginNodeViews, CommandFn, PluginCommandMap } from 'static/toast-editor/editor/types/plugin';
export { MdLikeNode, HTMLMdNode } from 'static/toast-editor/editor/types/markdown';
export { Editor, EditorCore, Viewer };
export default Editor;

export declare namespace toastui {
  export { Editor };
}
