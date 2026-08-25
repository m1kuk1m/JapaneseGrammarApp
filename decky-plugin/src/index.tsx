import {
  definePlugin,
  PanelSection,
  PanelSectionRow,
  ButtonItem,
  TextField,
  ToggleField,
  ServerAPI,
  staticClasses,
  Field,
  Navigation
} from "decky-frontend-lib";
import { VFC, useState, useEffect } from "react";
import {
  FaMobileAlt,
  FaCheckCircle,
  FaTimesCircle,
  FaCamera,
  FaKeyboard,
  FaSearch,
  FaStethoscope,
  FaUnlink,
  FaChevronDown,
  FaChevronUp
} from "react-icons/fa";

interface PluginState {
  phone_ip: string;
  phone_port: number;
  is_paired: boolean;
  auto_upload_enabled?: boolean;
  last_sync_status: string;
  hotkey_name: string;
}

// Universal bridge supporting Decky v1, v2 (serverAPI / DFL) and v3 (@decky/api / loaderAPIInit)
async function callBackend<T = any>(
  methodName: string,
  args: Record<string, any> = {},
  serverAPI?: any
): Promise<{ success: boolean; result?: T; message?: string }> {
  // 1. Try serverAPI prop
  if (serverAPI && typeof serverAPI.callPluginMethod === "function") {
    try {
      const res = await serverAPI.callPluginMethod(methodName, args);
      if (res && res.success !== undefined) {
        return {
          success: res.success,
          result: res.result !== undefined ? res.result : (res as any),
          message: res.result?.message || res.message
        };
      }
      return { success: true, result: res as any };
    } catch (e) {
      console.warn(`serverAPI.callPluginMethod(${methodName}) error:`, e);
    }
  }

  // 2. Try window.DeckyPluginLoader or window.DFL
  const dfl = (window as any).DeckyPluginLoader || (window as any).DFL;
  if (dfl && typeof dfl.callPluginMethod === "function") {
    try {
      const res = await dfl.callPluginMethod(methodName, args);
      if (res && res.success !== undefined) {
        return {
          success: res.success,
          result: res.result !== undefined ? res.result : (res as any),
          message: res.result?.message || res.message
        };
      }
      return { success: true, result: res as any };
    } catch (e) {
      console.warn(`DFL.callPluginMethod(${methodName}) error:`, e);
    }
  }

  // 3. Try Decky 3.x internal API connection (@decky/api)
  const internals = (window as any).__DECKY_SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED_deckyLoaderAPIInit;
  if (internals && typeof internals.connect === "function") {
    try {
      const api = internals.connect(2, "YomiDeck") || internals.connect(1, "YomiDeck");
      if (api && typeof api.call === "function") {
        const hasArgs = args && Object.keys(args).length > 0;
        const res = hasArgs ? await api.call(methodName, args) : await api.call(methodName);
        if (res && typeof res === "object" && "success" in res && typeof res.success === "boolean") {
          return {
            success: res.success,
            result: res.result !== undefined ? res.result : res,
            message: res.message || res.result?.message
          };
        }
        return { success: true, result: res };
      }
    } catch (e) {
      console.warn(`deckyLoaderAPIInit.call(${methodName}) error:`, e);
    }
  }

  // 4. Try global callable
  if (typeof (window as any).callPluginMethod === "function") {
    try {
      const res = await (window as any).callPluginMethod("YomiDeck", methodName, args);
      return { success: true, result: res };
    } catch (e) {
      console.warn(`global callPluginMethod(${methodName}) error:`, e);
    }
  }

  return { success: false, message: "Decky API not accessible" };
}

const Content: VFC<{ serverAPI?: ServerAPI }> = ({ serverAPI }) => {
  const [phoneIp, setPhoneIp] = useState<string>("");
  const [phonePort, setPhonePort] = useState<string>("8765");
  const [pin, setPin] = useState<string>("");
  const [status, setStatus] = useState<string>("Disconnected");
  const [isPaired, setIsPaired] = useState<boolean>(false);
  const [autoUpload, setAutoUpload] = useState<boolean>(true);
  const [loading, setLoading] = useState<boolean>(false);
  const [diagLoading, setDiagLoading] = useState<boolean>(false);
  const [diagLogs, setDiagLogs] = useState<string[]>([]);
  const [showDiag, setShowDiag] = useState<boolean>(false);
  const [hotkey, setHotkey] = useState<string>("Steam + R1 / F12");

  const sanitizeAndSetIp = (val: string) => {
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
      const resp = await callBackend<PluginState>("get_status", {}, serverAPI);
      if (resp.success && resp.result) {
        if (resp.result.phone_ip) {
          setPhoneIp(resp.result.phone_ip);
        }
        if (resp.result.phone_port) {
          setPhonePort(String(resp.result.phone_port));
        }
        setIsPaired(Boolean(resp.result.is_paired));
        if (resp.result.auto_upload_enabled !== undefined) {
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
    const interval = setInterval(fetchStatus, 3000);
    return () => clearInterval(interval);
  }, []);

  const handlePair = async () => {
    if (!phoneIp || !pin) return;
    setLoading(true);
    setStatus("Pairing...");
    try {
      const resp = await callBackend<{ success: boolean; message?: string }>(
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

  const handleToggleAutoUpload = async (enabled: boolean) => {
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
      const resp = await callBackend<{ ip: string; port: number } | null>(
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
      const resp = await callBackend<{ success: boolean; logs: string[] }>(
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
    } catch (e) {}
    try {
      if (typeof (window as any).DeckyPluginLoader?.closeSideMenus === "function") {
        (window as any).DeckyPluginLoader.closeSideMenus();
      } else if (typeof (window as any).SteamClient?.Navigation?.CloseSideMenus === "function") {
        (window as any).SteamClient.Navigation.CloseSideMenus();
      } else if (typeof (window as any).Navigation?.CloseSideMenus === "function") {
        (window as any).Navigation.CloseSideMenus();
      }
    } catch (e) {
      console.warn("Failed to close side menu:", e);
    }
  };

  const handleLiveCapture = async () => {
    setLoading(true);
    setStatus("Capturing screen...");
    const capturePromise = callBackend<{ success: boolean; message?: string }>(
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
      setTimeout(fetchStatus, 2000);
    }
  };

  return (
    <PanelSection title="YomiDeck Drop">
      {/* 1. Connection Status Card */}
      <PanelSectionRow>
        <Field
          label="Connection State"
          icon={isPaired ? <FaCheckCircle color="#4CAF50" /> : <FaTimesCircle color="#F44336" />}
        >
          {status}
        </Field>
      </PanelSectionRow>

      {/* 2. Unpaired Setup Flow */}
      {!isPaired ? (
        <>
          <PanelSectionRow>
            <ButtonItem layout="below" onClick={handleScan} disabled={loading}>
              <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
                <FaSearch /> Auto-Discover Phone (LAN / mDNS)
              </span>
            </ButtonItem>
          </PanelSectionRow>

          <PanelSectionRow>
            <TextField
              label="Phone IP Address (e.g. 192.168.1.18)"
              value={phoneIp}
              onChange={(e) => sanitizeAndSetIp(e.target.value)}
            />
          </PanelSectionRow>

          <PanelSectionRow>
            <TextField
              label="Port"
              value={phonePort}
              onChange={(e) => setPhonePort(e.target.value.trim())}
            />
          </PanelSectionRow>

          <PanelSectionRow>
            <TextField
              label="4-Digit PIN (From Phone Settings)"
              value={pin}
              onChange={(e) => setPin(e.target.value.trim())}
            />
          </PanelSectionRow>

          <PanelSectionRow>
            <ButtonItem layout="below" onClick={handlePair} disabled={loading || !phoneIp || pin.length !== 4}>
              Pair & Connect
            </ButtonItem>
          </PanelSectionRow>
        </>
      ) : (
        /* 3. Paired Connected State */
        <>
          <PanelSectionRow>
            <Field
              label="Target Device"
              icon={<FaMobileAlt color="#3b82f6" />}
            >
              {phoneIp}:{phonePort}
            </Field>
          </PanelSectionRow>

          {/* Sync Settings */}
          <PanelSection title="Sync Settings">
            <PanelSectionRow>
              <ToggleField
                label="Auto-Upload Screenshots"
                description="Automatically push in-game screenshots to YomiLLM in real time (Steam + R1 / Back Grip)."
                checked={autoUpload}
                onChange={handleToggleAutoUpload}
              />
            </PanelSectionRow>

            <PanelSectionRow>
              <Field
                label="In-Game Shortcut"
                icon={<FaKeyboard />}
              >
                Steam + R1 / L4 / R4
              </Field>
            </PanelSectionRow>
          </PanelSection>

          {/* Actions */}
          <PanelSection title="Actions">
            <PanelSectionRow>
              <ButtonItem layout="below" onClick={handleLiveCapture} disabled={loading}>
                <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
                  <FaCamera /> Capture & Sync Current Screen
                </span>
              </ButtonItem>
            </PanelSectionRow>

            <PanelSectionRow>
              <ButtonItem layout="below" onClick={handleUnpair} disabled={loading}>
                <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
                  <FaUnlink /> Disconnect & Unpair
                </span>
              </ButtonItem>
            </PanelSectionRow>
          </PanelSection>
        </>
      )}

      {/* 4. Troubleshooting & Diagnostics (Collapsible) */}
      <PanelSection title="Troubleshooting">
        <PanelSectionRow>
          <ButtonItem layout="below" onClick={() => setShowDiag(!showDiag)}>
            <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
              <FaStethoscope /> {showDiag ? "Hide Diagnostics" : "Network Diagnostics"}
              {showDiag ? <FaChevronUp size={12} /> : <FaChevronDown size={12} />}
            </span>
          </ButtonItem>
        </PanelSectionRow>

        {showDiag && (
          <>
            <PanelSectionRow>
              <ButtonItem layout="below" onClick={handleRunDiagnostics} disabled={diagLoading || !phoneIp}>
                {diagLoading ? "Diagnosing Network..." : "Run Network Test"}
              </ButtonItem>
            </PanelSectionRow>

            {diagLogs.length > 0 && (
              <PanelSectionRow>
                <div style={{
                  background: "#161920",
                  padding: "8px 10px",
                  borderRadius: "6px",
                  fontSize: "11px",
                  lineHeight: "1.45",
                  fontFamily: "monospace",
                  maxHeight: "160px",
                  overflowY: "auto",
                  border: "1px solid #2d3340"
                }}>
                  {diagLogs.map((log, i) => (
                    <div
                      key={i}
                      style={{
                        margin: "2px 0",
                        color: log.includes("ERROR") || log.includes("Failed")
                          ? "#ff6b6b"
                          : log.includes("OK")
                          ? "#51cf66"
                          : log.includes("WARN")
                          ? "#fcc419"
                          : "#ced4da"
                      }}
                    >
                      {log}
                    </div>
                  ))}
                </div>
              </PanelSectionRow>
            )}
          </>
        )}
      </PanelSection>
    </PanelSection>
  );
};

export default definePlugin((serverAPI?: ServerAPI) => {
  return {
    title: <div className={staticClasses.Title}>YomiDeck</div>,
    content: <Content serverAPI={serverAPI} />,
    icon: <FaMobileAlt />,
    onDismount() {
      // Cleanup when plugin is unmounted
    }
  };
});

