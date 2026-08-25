import * as esbuild from "esbuild";
import fs from "fs";

fs.mkdirSync("dist", { recursive: true });

const deckyGlobalsPlugin = {
  name: "decky-globals",
  setup(build) {
    build.onResolve({ filter: /^(react|react-dom|decky-frontend-lib|@decky\/ui|@decky\/api)$/ }, args => ({
      path: args.path,
      namespace: "decky-globals"
    }));

    build.onLoad({ filter: /.*/, namespace: "decky-globals" }, args => {
      if (args.path === "react") {
        return {
          contents: `
            const React = window.SP_REACT || window.React;
            export default React;
            export const {
              useState, useEffect, useMemo, useCallback, useRef, useContext, createContext,
              Fragment, createElement, Component, PureComponent, memo, forwardRef
            } = React;
          `,
          loader: "js"
        };
      }
      if (args.path === "react-dom") {
        return {
          contents: `
            const ReactDOM = window.SP_REACTDOM || window.ReactDOM;
            export default ReactDOM;
            export const { render, unmountComponentAtNode, createPortal } = ReactDOM;
          `,
          loader: "js"
        };
      }
      if (args.path === "decky-frontend-lib" || args.path === "@decky/ui" || args.path === "@decky/api") {
        return {
          contents: `
            const React = window.SP_REACT || window.React;
            const DFL = window.DFL || window.DeckyPluginLoader || {};
            export default DFL;
            export const definePlugin = (fn) => fn;
            export const PanelSection = DFL.PanelSection || ((props) => React.createElement('div', {style:{margin:'12px 0'}}, props.title ? React.createElement('h3', {style:{fontSize:'14px',fontWeight:'bold'}}, props.title) : null, props.children));
            export const PanelSectionRow = DFL.PanelSectionRow || ((props) => React.createElement('div', {style:{margin:'8px 0'}}, props.children));
            export const ButtonItem = DFL.ButtonItem || ((props) => React.createElement('button', {onClick: props.onClick, disabled: props.disabled, style:{width:'100%', padding:'8px 12px', background:'#3d4450', color:'#fff', border:'none', borderRadius:'4px'}}, props.children));
            export const TextField = DFL.TextField || ((props) => React.createElement('div', {style:{margin:'6px 0'}}, props.label ? React.createElement('label', {style:{display:'block',fontSize:'12px',marginBottom:'4px'}}, props.label) : null, React.createElement('input', {value: props.value, onChange: props.onChange, style:{width:'100%', padding:'6px', background:'#1a1f26', color:'#fff', border:'1px solid #3d4450', borderRadius:'4px'}})));
            export const Field = DFL.Field || ((props) => React.createElement('div', {style:{display:'flex', justifyContent:'space-between', alignItems:'center', margin:'6px 0'}}, React.createElement('span', {style:{display:'flex', alignItems:'center', gap:'6px'}}, props.icon, props.label), React.createElement('span', null, props.children)));
            export const ToggleField = DFL.ToggleField || ((props) => React.createElement('div', {style:{display:'flex', justifyContent:'space-between', alignItems:'center', margin:'6px 0'}}, React.createElement('span', null, props.label), React.createElement('input', {type:'checkbox', checked:props.checked, onChange:(e) => props.onChange && props.onChange(e.target.checked)})));
            export const staticClasses = DFL.staticClasses || { Title: 'quickaccessmenu_Title_3v1i_' };
            export const Navigation = DFL.Navigation || window.Navigation || {};
          `,
          loader: "js"
        };
      }
    });
  }
};

await esbuild.build({
  entryPoints: ["src/index.tsx"],
  bundle: true,
  outfile: "dist/index.js",
  format: "esm",
  target: "es2020",
  jsx: "transform",
  jsxFactory: "window.SP_REACT.createElement",
  jsxFragment: "window.SP_REACT.Fragment",
  plugins: [deckyGlobalsPlugin],
  define: {
    "process.env.NODE_ENV": '"production"'
  }
});

console.log("Decky frontend bundle built successfully with classic JSX transform!");
