// decky-globals:decky-frontend-lib
var React = window.SP_REACT || window.React;
var DFL = window.DFL || window.DeckyPluginLoader || {};
var definePlugin = (fn) => fn;
var PanelSection = DFL.PanelSection || ((props) => React.createElement("div", { style: { margin: "12px 0" } }, props.title ? React.createElement("h3", { style: { fontSize: "14px", fontWeight: "bold" } }, props.title) : null, props.children));
var PanelSectionRow = DFL.PanelSectionRow || ((props) => React.createElement("div", { style: { margin: "8px 0" } }, props.children));
var ButtonItem = DFL.ButtonItem || ((props) => React.createElement("button", { onClick: props.onClick, disabled: props.disabled, style: { width: "100%", padding: "8px 12px", background: "#3d4450", color: "#fff", border: "none", borderRadius: "4px" } }, props.children));
var TextField = DFL.TextField || ((props) => React.createElement("div", { style: { margin: "6px 0" } }, props.label ? React.createElement("label", { style: { display: "block", fontSize: "12px", marginBottom: "4px" } }, props.label) : null, React.createElement("input", { value: props.value, onChange: props.onChange, style: { width: "100%", padding: "6px", background: "#1a1f26", color: "#fff", border: "1px solid #3d4450", borderRadius: "4px" } })));
var Field = DFL.Field || ((props) => React.createElement("div", { style: { display: "flex", justifyContent: "space-between", alignItems: "center", margin: "6px 0" } }, React.createElement("span", { style: { display: "flex", alignItems: "center", gap: "6px" } }, props.icon, props.label), React.createElement("span", null, props.children)));
var ToggleField = DFL.ToggleField || ((props) => React.createElement("div", { style: { display: "flex", justifyContent: "space-between", alignItems: "center", margin: "6px 0" } }, React.createElement("span", null, props.label), React.createElement("input", { type: "checkbox", checked: props.checked, onChange: (e) => props.onChange && props.onChange(e.target.checked) })));
var staticClasses = DFL.staticClasses || { Title: "quickaccessmenu_Title_3v1i_" };
var Navigation = DFL.Navigation || window.Navigation || {};

// decky-globals:react
var React2 = window.SP_REACT || window.React;
var react_default = React2;
var {
  useState,
  useEffect,
  useMemo,
  useCallback,
  useRef,
  useContext,
  createContext,
  Fragment,
  createElement,
  Component,
  PureComponent,
  memo,
  forwardRef
} = React2;

// node_modules/react-icons/lib/iconContext.mjs
var DefaultContext = {
  color: void 0,
  size: void 0,
  className: void 0,
  style: void 0,
  attr: void 0
};
var IconContext = react_default.createContext && /* @__PURE__ */ react_default.createContext(DefaultContext);

// node_modules/react-icons/lib/iconBase.mjs
var _excluded = ["attr", "size", "title"];
function _objectWithoutProperties(e, t) {
  if (null == e) return {};
  var o, r, i = _objectWithoutPropertiesLoose(e, t);
  if (Object.getOwnPropertySymbols) {
    var n = Object.getOwnPropertySymbols(e);
    for (r = 0; r < n.length; r++) o = n[r], -1 === t.indexOf(o) && {}.propertyIsEnumerable.call(e, o) && (i[o] = e[o]);
  }
  return i;
}
function _objectWithoutPropertiesLoose(r, e) {
  if (null == r) return {};
  var t = {};
  for (var n in r) if ({}.hasOwnProperty.call(r, n)) {
    if (-1 !== e.indexOf(n)) continue;
    t[n] = r[n];
  }
  return t;
}
function _extends() {
  return _extends = Object.assign ? Object.assign.bind() : function(n) {
    for (var e = 1; e < arguments.length; e++) {
      var t = arguments[e];
      for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]);
    }
    return n;
  }, _extends.apply(null, arguments);
}
function ownKeys(e, r) {
  var t = Object.keys(e);
  if (Object.getOwnPropertySymbols) {
    var o = Object.getOwnPropertySymbols(e);
    r && (o = o.filter(function(r2) {
      return Object.getOwnPropertyDescriptor(e, r2).enumerable;
    })), t.push.apply(t, o);
  }
  return t;
}
function _objectSpread(e) {
  for (var r = 1; r < arguments.length; r++) {
    var t = null != arguments[r] ? arguments[r] : {};
    r % 2 ? ownKeys(Object(t), true).forEach(function(r2) {
      _defineProperty(e, r2, t[r2]);
    }) : Object.getOwnPropertyDescriptors ? Object.defineProperties(e, Object.getOwnPropertyDescriptors(t)) : ownKeys(Object(t)).forEach(function(r2) {
      Object.defineProperty(e, r2, Object.getOwnPropertyDescriptor(t, r2));
    });
  }
  return e;
}
function _defineProperty(e, r, t) {
  return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: true, configurable: true, writable: true }) : e[r] = t, e;
}
function _toPropertyKey(t) {
  var i = _toPrimitive(t, "string");
  return "symbol" == typeof i ? i : i + "";
}
function _toPrimitive(t, r) {
  if ("object" != typeof t || !t) return t;
  var e = t[Symbol.toPrimitive];
  if (void 0 !== e) {
    var i = e.call(t, r || "default");
    if ("object" != typeof i) return i;
    throw new TypeError("@@toPrimitive must return a primitive value.");
  }
  return ("string" === r ? String : Number)(t);
}
function Tree2Element(tree) {
  return tree && tree.map((node, i) => /* @__PURE__ */ react_default.createElement(node.tag, _objectSpread({
    key: i
  }, node.attr), Tree2Element(node.child)));
}
function GenIcon(data) {
  return (props) => /* @__PURE__ */ react_default.createElement(IconBase, _extends({
    attr: _objectSpread({}, data.attr)
  }, props), Tree2Element(data.child));
}
function IconBase(props) {
  var elem = (conf) => {
    var attr = props.attr, size = props.size, title = props.title, svgProps = _objectWithoutProperties(props, _excluded);
    var computedSize = size || conf.size || "1em";
    var className;
    if (conf.className) className = conf.className;
    if (props.className) className = (className ? className + " " : "") + props.className;
    return /* @__PURE__ */ react_default.createElement("svg", _extends({
      stroke: "currentColor",
      fill: "currentColor",
      strokeWidth: "0"
    }, conf.attr, attr, svgProps, {
      className,
      style: _objectSpread(_objectSpread({
        color: props.color || conf.color
      }, conf.style), props.style),
      height: computedSize,
      width: computedSize,
      xmlns: "http://www.w3.org/2000/svg"
    }), title && /* @__PURE__ */ react_default.createElement("title", null, title), props.children);
  };
  return IconContext !== void 0 ? /* @__PURE__ */ react_default.createElement(IconContext.Consumer, null, (conf) => elem(conf)) : elem(DefaultContext);
}

// node_modules/react-icons/fa/index.mjs
function FaUnlink(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M304.083 405.907c4.686 4.686 4.686 12.284 0 16.971l-44.674 44.674c-59.263 59.262-155.693 59.266-214.961 0-59.264-59.265-59.264-155.696 0-214.96l44.675-44.675c4.686-4.686 12.284-4.686 16.971 0l39.598 39.598c4.686 4.686 4.686 12.284 0 16.971l-44.675 44.674c-28.072 28.073-28.072 73.75 0 101.823 28.072 28.072 73.75 28.073 101.824 0l44.674-44.674c4.686-4.686 12.284-4.686 16.971 0l39.597 39.598zm-56.568-260.216c4.686 4.686 12.284 4.686 16.971 0l44.674-44.674c28.072-28.075 73.75-28.073 101.824 0 28.072 28.073 28.072 73.75 0 101.823l-44.675 44.674c-4.686 4.686-4.686 12.284 0 16.971l39.598 39.598c4.686 4.686 12.284 4.686 16.971 0l44.675-44.675c59.265-59.265 59.265-155.695 0-214.96-59.266-59.264-155.695-59.264-214.961 0l-44.674 44.674c-4.686 4.686-4.686 12.284 0 16.971l39.597 39.598zm234.828 359.28l22.627-22.627c9.373-9.373 9.373-24.569 0-33.941L63.598 7.029c-9.373-9.373-24.569-9.373-33.941 0L7.029 29.657c-9.373 9.373-9.373 24.569 0 33.941l441.373 441.373c9.373 9.372 24.569 9.372 33.941 0z" }, "child": [] }] })(props);
}
function FaTimesCircle(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M256 8C119 8 8 119 8 256s111 248 248 248 248-111 248-248S393 8 256 8zm121.6 313.1c4.7 4.7 4.7 12.3 0 17L338 377.6c-4.7 4.7-12.3 4.7-17 0L256 312l-65.1 65.6c-4.7 4.7-12.3 4.7-17 0L134.4 338c-4.7-4.7-4.7-12.3 0-17l65.6-65-65.6-65.1c-4.7-4.7-4.7-12.3 0-17l39.6-39.6c4.7-4.7 12.3-4.7 17 0l65 65.7 65.1-65.6c4.7-4.7 12.3-4.7 17 0l39.6 39.6c4.7 4.7 4.7 12.3 0 17L312 256l65.6 65.1z" }, "child": [] }] })(props);
}
function FaStethoscope(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M447.1 112c-34.2.5-62.3 28.4-63 62.6-.5 24.3 12.5 45.6 32 56.8V344c0 57.3-50.2 104-112 104-60 0-109.2-44.1-111.9-99.2C265 333.8 320 269.2 320 192V36.6c0-11.4-8.1-21.3-19.3-23.5L237.8.5c-13-2.6-25.6 5.8-28.2 18.8L206.4 35c-2.6 13 5.8 25.6 18.8 28.2l30.7 6.1v121.4c0 52.9-42.2 96.7-95.1 97.2-53.4.5-96.9-42.7-96.9-96V69.4l30.7-6.1c13-2.6 21.4-15.2 18.8-28.2l-3.1-15.7C107.7 6.4 95.1-2 82.1.6L19.3 13C8.1 15.3 0 25.1 0 36.6V192c0 77.3 55.1 142 128.1 156.8C130.7 439.2 208.6 512 304 512c97 0 176-75.4 176-168V231.4c19.1-11.1 32-31.7 32-55.4 0-35.7-29.2-64.5-64.9-64zm.9 80c-8.8 0-16-7.2-16-16s7.2-16 16-16 16 7.2 16 16-7.2 16-16 16z" }, "child": [] }] })(props);
}
function FaSearch(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M505 442.7L405.3 343c-4.5-4.5-10.6-7-17-7H372c27.6-35.3 44-79.7 44-128C416 93.1 322.9 0 208 0S0 93.1 0 208s93.1 208 208 208c48.3 0 92.7-16.4 128-44v16.3c0 6.4 2.5 12.5 7 17l99.7 99.7c9.4 9.4 24.6 9.4 33.9 0l28.3-28.3c9.4-9.4 9.4-24.6.1-34zM208 336c-70.7 0-128-57.2-128-128 0-70.7 57.2-128 128-128 70.7 0 128 57.2 128 128 0 70.7-57.2 128-128 128z" }, "child": [] }] })(props);
}
function FaMobileAlt(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 320 512" }, "child": [{ "tag": "path", "attr": { "d": "M272 0H48C21.5 0 0 21.5 0 48v416c0 26.5 21.5 48 48 48h224c26.5 0 48-21.5 48-48V48c0-26.5-21.5-48-48-48zM160 480c-17.7 0-32-14.3-32-32s14.3-32 32-32 32 14.3 32 32-14.3 32-32 32zm112-108c0 6.6-5.4 12-12 12H60c-6.6 0-12-5.4-12-12V60c0-6.6 5.4-12 12-12h200c6.6 0 12 5.4 12 12v312z" }, "child": [] }] })(props);
}
function FaKeyboard(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 576 512" }, "child": [{ "tag": "path", "attr": { "d": "M528 448H48c-26.51 0-48-21.49-48-48V112c0-26.51 21.49-48 48-48h480c26.51 0 48 21.49 48 48v288c0 26.51-21.49 48-48 48zM128 180v-40c0-6.627-5.373-12-12-12H76c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm96 0v-40c0-6.627-5.373-12-12-12h-40c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm96 0v-40c0-6.627-5.373-12-12-12h-40c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm96 0v-40c0-6.627-5.373-12-12-12h-40c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm96 0v-40c0-6.627-5.373-12-12-12h-40c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm-336 96v-40c0-6.627-5.373-12-12-12h-40c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm96 0v-40c0-6.627-5.373-12-12-12h-40c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm96 0v-40c0-6.627-5.373-12-12-12h-40c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm96 0v-40c0-6.627-5.373-12-12-12h-40c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm-336 96v-40c0-6.627-5.373-12-12-12H76c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12zm288 0v-40c0-6.627-5.373-12-12-12H172c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h232c6.627 0 12-5.373 12-12zm96 0v-40c0-6.627-5.373-12-12-12h-40c-6.627 0-12 5.373-12 12v40c0 6.627 5.373 12 12 12h40c6.627 0 12-5.373 12-12z" }, "child": [] }] })(props);
}
function FaChevronUp(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 448 512" }, "child": [{ "tag": "path", "attr": { "d": "M240.971 130.524l194.343 194.343c9.373 9.373 9.373 24.569 0 33.941l-22.667 22.667c-9.357 9.357-24.522 9.375-33.901.04L224 227.495 69.255 381.516c-9.379 9.335-24.544 9.317-33.901-.04l-22.667-22.667c-9.373-9.373-9.373-24.569 0-33.941L207.03 130.525c9.372-9.373 24.568-9.373 33.941-.001z" }, "child": [] }] })(props);
}
function FaChevronDown(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 448 512" }, "child": [{ "tag": "path", "attr": { "d": "M207.029 381.476L12.686 187.132c-9.373-9.373-9.373-24.569 0-33.941l22.667-22.667c9.357-9.357 24.522-9.375 33.901-.04L224 284.505l154.745-154.021c9.379-9.335 24.544-9.317 33.901.04l22.667 22.667c9.373 9.373 9.373 24.569 0 33.941L240.971 381.476c-9.373 9.372-24.569 9.372-33.942 0z" }, "child": [] }] })(props);
}
function FaCheckCircle(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M504 256c0 136.967-111.033 248-248 248S8 392.967 8 256 119.033 8 256 8s248 111.033 248 248zM227.314 387.314l184-184c6.248-6.248 6.248-16.379 0-22.627l-22.627-22.627c-6.248-6.249-16.379-6.249-22.628 0L216 308.118l-70.059-70.059c-6.248-6.248-16.379-6.248-22.628 0l-22.627 22.627c-6.248 6.248-6.248 16.379 0 22.627l104 104c6.249 6.249 16.379 6.249 22.628.001z" }, "child": [] }] })(props);
}
function FaCamera(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M512 144v288c0 26.5-21.5 48-48 48H48c-26.5 0-48-21.5-48-48V144c0-26.5 21.5-48 48-48h88l12.3-32.9c7-18.7 24.9-31.1 44.9-31.1h125.5c20 0 37.9 12.4 44.9 31.1L376 96h88c26.5 0 48 21.5 48 48zM376 288c0-66.2-53.8-120-120-120s-120 53.8-120 120 53.8 120 120 120 120-53.8 120-120zm-32 0c0 48.5-39.5 88-88 88s-88-39.5-88-88 39.5-88 88-88 88 39.5 88 88z" }, "child": [] }] })(props);
}

// src/index.tsx
async function callBackend(methodName, args = {}, serverAPI) {
  if (serverAPI && typeof serverAPI.callPluginMethod === "function") {
    try {
      const res = await serverAPI.callPluginMethod(methodName, args);
      if (res && res.success !== void 0) {
        return {
          success: res.success,
          result: res.result !== void 0 ? res.result : res,
          message: res.result?.message || res.message
        };
      }
      return { success: true, result: res };
    } catch (e) {
      console.warn(`serverAPI.callPluginMethod(${methodName}) error:`, e);
    }
  }
  const dfl = window.DeckyPluginLoader || window.DFL;
  if (dfl && typeof dfl.callPluginMethod === "function") {
    try {
      const res = await dfl.callPluginMethod(methodName, args);
      if (res && res.success !== void 0) {
        return {
          success: res.success,
          result: res.result !== void 0 ? res.result : res,
          message: res.result?.message || res.message
        };
      }
      return { success: true, result: res };
    } catch (e) {
      console.warn(`DFL.callPluginMethod(${methodName}) error:`, e);
    }
  }
  const internals = window.__DECKY_SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED_deckyLoaderAPIInit;
  if (internals && typeof internals.connect === "function") {
    try {
      const api = internals.connect(2, "YomiDeck") || internals.connect(1, "YomiDeck");
      if (api && typeof api.call === "function") {
        const hasArgs = args && Object.keys(args).length > 0;
        const res = hasArgs ? await api.call(methodName, args) : await api.call(methodName);
        if (res && typeof res === "object" && "success" in res && typeof res.success === "boolean") {
          return {
            success: res.success,
            result: res.result !== void 0 ? res.result : res,
            message: res.message || res.result?.message
          };
        }
        return { success: true, result: res };
      }
    } catch (e) {
      console.warn(`deckyLoaderAPIInit.call(${methodName}) error:`, e);
    }
  }
  if (typeof window.callPluginMethod === "function") {
    try {
      const res = await window.callPluginMethod("YomiDeck", methodName, args);
      return { success: true, result: res };
    } catch (e) {
      console.warn(`global callPluginMethod(${methodName}) error:`, e);
    }
  }
  return { success: false, message: "Decky API not accessible" };
}
var Content = ({ serverAPI }) => {
  const [phoneIp, setPhoneIp] = useState("");
  const [phonePort, setPhonePort] = useState("8765");
  const [pin, setPin] = useState("");
  const [status, setStatus] = useState("Disconnected");
  const [isPaired, setIsPaired] = useState(false);
  const [autoUpload, setAutoUpload] = useState(true);
  const [loading, setLoading] = useState(false);
  const [diagLoading, setDiagLoading] = useState(false);
  const [diagLogs, setDiagLogs] = useState([]);
  const [showDiag, setShowDiag] = useState(false);
  const [hotkey, setHotkey] = useState("Steam + R1 / F12");
  const sanitizeAndSetIp = (val) => {
    let clean = val.trim();
    if (clean.startsWith("http://")) clean = clean.substring(7);
    if (clean.startsWith("https://")) clean = clean.substring(8);
    clean = clean.split("/")[0];
    if (clean.includes(":")) {
      const parts = clean.split(":");
      setPhoneIp(parts[0]);
      if (parts[1]) setPhonePort(parts[1]);
    } else {
      setPhoneIp(clean);
    }
  };
  const fetchStatus = async () => {
    try {
      const resp = await callBackend("get_status", {}, serverAPI);
      if (resp.success && resp.result) {
        if (resp.result.phone_ip) {
          setPhoneIp(resp.result.phone_ip);
        }
        if (resp.result.phone_port) {
          setPhonePort(String(resp.result.phone_port));
        }
        setIsPaired(Boolean(resp.result.is_paired));
        if (resp.result.auto_upload_enabled !== void 0) {
          setAutoUpload(Boolean(resp.result.auto_upload_enabled));
        }
        setStatus(resp.result.last_sync_status || (resp.result.is_paired ? "Connected" : "Disconnected"));
        if (resp.result.hotkey_name) {
          setHotkey(resp.result.hotkey_name);
        }
      }
    } catch (e) {
      console.error("Failed to fetch YomiDeck status:", e);
    }
  };
  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 3e3);
    return () => clearInterval(interval);
  }, []);
  const handlePair = async () => {
    if (!phoneIp || !pin) return;
    setLoading(true);
    setStatus("Pairing...");
    try {
      const resp = await callBackend(
        "pair_device",
        {
          ip: phoneIp.trim(),
          port: parseInt(phonePort, 10) || 8765,
          pin: pin.trim()
        },
        serverAPI
      );
      if (resp.success && resp.result?.success) {
        setIsPaired(true);
        setStatus("Connected");
        setPin("");
      } else {
        setStatus(`Pair failed: ${resp.result?.message || resp.message || "Invalid PIN"}`);
      }
    } catch (e) {
      setStatus(`Pair error: ${String(e)}`);
    } finally {
      setLoading(false);
      fetchStatus();
    }
  };
  const handleUnpair = async () => {
    setLoading(true);
    try {
      await callBackend("unpair_device", {}, serverAPI);
      setIsPaired(false);
      setStatus("Disconnected");
    } catch (e) {
      console.error("Unpair failed:", e);
    } finally {
      setLoading(false);
      fetchStatus();
    }
  };
  const handleToggleAutoUpload = async (enabled) => {
    setAutoUpload(enabled);
    try {
      await callBackend("set_auto_upload", { enabled }, serverAPI);
    } catch (e) {
      console.error("Failed to toggle auto upload:", e);
    }
  };
  const handleScan = async () => {
    setLoading(true);
    setStatus("Scanning LAN subnet & mDNS...");
    try {
      const resp = await callBackend(
        "discover_phone",
        {},
        serverAPI
      );
      if (resp.success && resp.result) {
        setPhoneIp(resp.result.ip);
        setPhonePort(String(resp.result.port));
        setStatus(`Found phone at ${resp.result.ip}`);
      } else {
        setStatus("No phone found on LAN");
      }
    } catch (e) {
      setStatus("Scan failed");
    } finally {
      setLoading(false);
    }
  };
  const handleRunDiagnostics = async () => {
    setDiagLoading(true);
    setDiagLogs(["Testing connection to drop service..."]);
    try {
      const resp = await callBackend(
        "run_diagnostics",
        {
          ip: phoneIp.trim(),
          port: parseInt(phonePort, 10) || 8765
        },
        serverAPI
      );
      if (resp.success && resp.result?.logs) {
        setDiagLogs(resp.result.logs);
      } else {
        setDiagLogs([`Diagnostics returned no logs: ${resp.message || "Unknown error"}`]);
      }
    } catch (e) {
      setDiagLogs([`Diagnostics error: ${String(e)}`]);
    } finally {
      setDiagLoading(false);
    }
  };
  const closeSideMenu = () => {
    try {
      if (Navigation && typeof Navigation.CloseSideMenus === "function") {
        Navigation.CloseSideMenus();
      }
    } catch (e) {
    }
    try {
      if (typeof window.DeckyPluginLoader?.closeSideMenus === "function") {
        window.DeckyPluginLoader.closeSideMenus();
      } else if (typeof window.SteamClient?.Navigation?.CloseSideMenus === "function") {
        window.SteamClient.Navigation.CloseSideMenus();
      } else if (typeof window.Navigation?.CloseSideMenus === "function") {
        window.Navigation.CloseSideMenus();
      }
    } catch (e) {
      console.warn("Failed to close side menu:", e);
    }
  };
  const handleLiveCapture = async () => {
    setLoading(true);
    setStatus("Capturing screen...");
    const capturePromise = callBackend(
      "trigger_live_capture",
      {},
      serverAPI
    );
    closeSideMenu();
    try {
      const resp = await capturePromise;
      if (resp.success && resp.result?.success) {
        setStatus("Screen captured & sent");
      } else {
        setStatus(`Capture: ${resp.result?.message || resp.message || "Failed"}`);
      }
    } catch (e) {
      setStatus(`Capture error: ${String(e)}`);
    } finally {
      setLoading(false);
      setTimeout(fetchStatus, 2e3);
    }
  };
  return /* @__PURE__ */ window.SP_REACT.createElement(PanelSection, { title: "YomiDeck Drop" }, /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
    Field,
    {
      label: "Connection State",
      icon: isPaired ? /* @__PURE__ */ window.SP_REACT.createElement(FaCheckCircle, { color: "#4CAF50" }) : /* @__PURE__ */ window.SP_REACT.createElement(FaTimesCircle, { color: "#F44336" })
    },
    status
  )), !isPaired ? /* @__PURE__ */ window.SP_REACT.createElement(window.SP_REACT.Fragment, null, /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(ButtonItem, { layout: "below", onClick: handleScan, disabled: loading }, /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaSearch, null), " Auto-Discover Phone (LAN / mDNS)"))), /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
    TextField,
    {
      label: "Phone IP Address (e.g. 192.168.1.18)",
      value: phoneIp,
      onChange: (e) => sanitizeAndSetIp(e.target.value)
    }
  )), /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
    TextField,
    {
      label: "Port",
      value: phonePort,
      onChange: (e) => setPhonePort(e.target.value.trim())
    }
  )), /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
    TextField,
    {
      label: "4-Digit PIN (From Phone Settings)",
      value: pin,
      onChange: (e) => setPin(e.target.value.trim())
    }
  )), /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(ButtonItem, { layout: "below", onClick: handlePair, disabled: loading || !phoneIp || pin.length !== 4 }, "Pair & Connect"))) : (
    /* 3. Paired Connected State */
    /* @__PURE__ */ window.SP_REACT.createElement(window.SP_REACT.Fragment, null, /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      Field,
      {
        label: "Target Device",
        icon: /* @__PURE__ */ window.SP_REACT.createElement(FaMobileAlt, { color: "#3b82f6" })
      },
      phoneIp,
      ":",
      phonePort
    )), /* @__PURE__ */ window.SP_REACT.createElement(PanelSection, { title: "Sync Settings" }, /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      ToggleField,
      {
        label: "Auto-Upload Screenshots",
        description: "Automatically push in-game screenshots to YomiLLM in real time (Steam + R1 / Back Grip).",
        checked: autoUpload,
        onChange: handleToggleAutoUpload
      }
    )), /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      Field,
      {
        label: "In-Game Shortcut",
        icon: /* @__PURE__ */ window.SP_REACT.createElement(FaKeyboard, null)
      },
      "Steam + R1 / L4 / R4"
    ))), /* @__PURE__ */ window.SP_REACT.createElement(PanelSection, { title: "Actions" }, /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(ButtonItem, { layout: "below", onClick: handleLiveCapture, disabled: loading }, /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaCamera, null), " Capture & Sync Current Screen"))), /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(ButtonItem, { layout: "below", onClick: handleUnpair, disabled: loading }, /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaUnlink, null), " Disconnect & Unpair")))))
  ), /* @__PURE__ */ window.SP_REACT.createElement(PanelSection, { title: "Troubleshooting" }, /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(ButtonItem, { layout: "below", onClick: () => setShowDiag(!showDiag) }, /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaStethoscope, null), " ", showDiag ? "Hide Diagnostics" : "Network Diagnostics", showDiag ? /* @__PURE__ */ window.SP_REACT.createElement(FaChevronUp, { size: 12 }) : /* @__PURE__ */ window.SP_REACT.createElement(FaChevronDown, { size: 12 })))), showDiag && /* @__PURE__ */ window.SP_REACT.createElement(window.SP_REACT.Fragment, null, /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(ButtonItem, { layout: "below", onClick: handleRunDiagnostics, disabled: diagLoading || !phoneIp }, diagLoading ? "Diagnosing Network..." : "Run Network Test")), diagLogs.length > 0 && /* @__PURE__ */ window.SP_REACT.createElement(PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement("div", { style: {
    background: "#161920",
    padding: "8px 10px",
    borderRadius: "6px",
    fontSize: "11px",
    lineHeight: "1.45",
    fontFamily: "monospace",
    maxHeight: "160px",
    overflowY: "auto",
    border: "1px solid #2d3340"
  } }, diagLogs.map((log, i) => /* @__PURE__ */ window.SP_REACT.createElement(
    "div",
    {
      key: i,
      style: {
        margin: "2px 0",
        color: log.includes("ERROR") || log.includes("Failed") ? "#ff6b6b" : log.includes("OK") ? "#51cf66" : log.includes("WARN") ? "#fcc419" : "#ced4da"
      }
    },
    log
  )))))));
};
var index_default = definePlugin((serverAPI) => {
  return {
    title: /* @__PURE__ */ window.SP_REACT.createElement("div", { className: staticClasses.Title }, "YomiDeck"),
    content: /* @__PURE__ */ window.SP_REACT.createElement(Content, { serverAPI }),
    icon: /* @__PURE__ */ window.SP_REACT.createElement(FaMobileAlt, null),
    onDismount() {
    }
  };
});
export {
  index_default as default
};
