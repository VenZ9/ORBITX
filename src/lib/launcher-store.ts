import { create } from "zustand";

export type NavId =
  | "home"
  | "manage"
  | "download"
  | "controller"
  | "multiplayer"
  | "settings";

export type Loader = "Vanilla" | "Fabric" | "Forge" | "Quilt" | "NeoForge";

export type Version = {
  id: string;
  name: string;
  mc: string;
  loader: Loader;
  loaderVer?: string;
  lastPlayed?: string;
  installed: boolean;
};

export type Account = {
  id: string;
  name: string;
  type: "Offline" | "Microsoft" | "External";
};

export type ControlLayout = {
  id: string;
  name: string;
  author: string;
  buttons: number;
};

export type DownloadTarget = {
  id: string;
  mc: string;
  type: "release" | "snapshot";
  date: string;
};

type LauncherState = {
  nav: NavId;
  setNav: (nav: NavId) => void;
  versions: Version[];
  selectedVersionId: string;
  selectVersion: (id: string) => void;
  installVersion: (v: Version) => void;
  removeVersion: (id: string) => void;
  accounts: Account[];
  selectedAccountId: string;
  selectAccount: (id: string) => void;
  addOfflineAccount: (name: string) => void;
  layouts: ControlLayout[];
  selectedLayoutId: string;
  selectLayout: (id: string) => void;
  launching: boolean;
  launchLog: string[];
  launchPhase: "idle" | "boot" | "ready" | "blocked";
  startLaunch: () => void;
  clearLaunch: () => void;
  ramMb: number;
  setRamMb: (n: number) => void;
  renderer: string;
  setRenderer: (r: string) => void;
  launcherName: string;
  setLauncherName: (n: string) => void;
};

const SEED_VERSIONS: Version[] = [
  {
    id: "1.21.4",
    name: "1.21.4",
    mc: "1.21.4",
    loader: "Vanilla",
    lastPlayed: "Today",
    installed: true,
  },
  {
    id: "1.21.1-fabric",
    name: "1.21.1-Fabric",
    mc: "1.21.1",
    loader: "Fabric",
    loaderVer: "0.16.9",
    lastPlayed: "Yesterday",
    installed: true,
  },
  {
    id: "1.20.1-forge",
    name: "1.20.1-Forge",
    mc: "1.20.1",
    loader: "Forge",
    loaderVer: "47.3.0",
    lastPlayed: "3 days ago",
    installed: true,
  },
  {
    id: "1.16.5-forge",
    name: "1.16.5-Forge",
    mc: "1.16.5",
    loader: "Forge",
    loaderVer: "36.2.39",
    installed: true,
  },
  {
    id: "1.12.2-forge",
    name: "1.12.2-Forge",
    mc: "1.12.2",
    loader: "Forge",
    loaderVer: "14.23.5.2860",
    installed: true,
  },
];

const SEED_ACCOUNTS: Account[] = [
  { id: "orbit", name: "Orbit", type: "Offline" },
];

const SEED_LAYOUTS: ControlLayout[] = [
  { id: "default", name: "OrbitX Default", author: "OrbitX", buttons: 18 },
  { id: "survival", name: "Survival Compact", author: "VenZ9", buttons: 12 },
  { id: "pvp", name: "PvP Split", author: "community", buttons: 24 },
];

const BOOT_LINES = [
  "OrbitX Launcher 1.3.3.3",
  "applicationId = com.orbitx.launcher",
  "Resolving Java runtime…",
  "Using JRE 21  (/OrbitX/runtime/java/jre21)",
  "Renderer: Zink (OpenGL 4.6)",
  "Unpacking natives (arm64-v8a)",
  "authlib-injector skipped (offline)",
  "Game directory: /OrbitX/.minecraft",
  "Launching net.minecraft.client.main.Main",
];

export const CATALOG: DownloadTarget[] = [
  { id: "1.21.8", mc: "1.21.8", type: "release", date: "2025.07.17" },
  { id: "1.21.4", mc: "1.21.4", type: "release", date: "2024.12.03" },
  { id: "1.21.1", mc: "1.21.1", type: "release", date: "2024.08.08" },
  { id: "1.20.1", mc: "1.20.1", type: "release", date: "2023.06.07" },
  { id: "1.19.2", mc: "1.19.2", type: "release", date: "2022.08.05" },
  { id: "1.18.2", mc: "1.18.2", type: "release", date: "2022.02.28" },
  { id: "1.16.5", mc: "1.16.5", type: "release", date: "2021.01.15" },
  { id: "1.12.2", mc: "1.12.2", type: "release", date: "2017.09.18" },
  { id: "1.8.9", mc: "1.8.9", type: "release", date: "2015.12.09" },
  { id: "25w31a", mc: "25w31a", type: "snapshot", date: "2025.07.30" },
];

export const useLauncher = create<LauncherState>()((set, get) => ({
  nav: "home",
  setNav: (nav) => set({ nav }),
  versions: SEED_VERSIONS,
  selectedVersionId: "1.21.4",
  selectVersion: (id) => set({ selectedVersionId: id, nav: "home" }),
  installVersion: (v) => {
    const exists = get().versions.some((x) => x.id === v.id);
    if (exists) {
      set({ selectedVersionId: v.id, nav: "home" });
      return;
    }
    set({
      versions: [v, ...get().versions],
      selectedVersionId: v.id,
      nav: "home",
    });
  },
  removeVersion: (id) => {
    const next = get().versions.filter((v) => v.id !== id);
    const selected =
      get().selectedVersionId === id ? (next[0]?.id ?? "") : get().selectedVersionId;
    set({ versions: next, selectedVersionId: selected });
  },
  accounts: SEED_ACCOUNTS,
  selectedAccountId: "orbit",
  selectAccount: (id) => set({ selectedAccountId: id }),
  addOfflineAccount: (name) => {
    const trimmed = name.trim().slice(0, 16);
    if (!trimmed) return;
    const id = "off-" + trimmed.toLowerCase().replace(/\s+/g, "_");
    if (get().accounts.some((a) => a.id === id)) {
      set({ selectedAccountId: id });
      return;
    }
    set({
      accounts: [...get().accounts, { id, name: trimmed, type: "Offline" }],
      selectedAccountId: id,
    });
  },
  layouts: SEED_LAYOUTS,
  selectedLayoutId: "default",
  selectLayout: (id) => set({ selectedLayoutId: id }),
  launching: false,
  launchLog: [],
  launchPhase: "idle",
  startLaunch: () => {
    if (get().launching) return;
    set({ launching: true, launchLog: [], launchPhase: "boot" });
    BOOT_LINES.forEach((line, i) => {
      window.setTimeout(() => {
        set({ launchLog: [...get().launchLog, line] });
        if (i === BOOT_LINES.length - 1) {
          window.setTimeout(() => set({ launchPhase: "blocked" }), 700);
        }
      }, 280 * (i + 1));
    });
  },
  clearLaunch: () => set({ launching: false, launchLog: [], launchPhase: "idle" }),
  ramMb: 2048,
  setRamMb: (n) => set({ ramMb: n }),
  renderer: "Zink (OpenGL 4.6)",
  setRenderer: (r) => set({ renderer: r }),
  launcherName: "OrbitX Launcher",
  setLauncherName: (n) => set({ launcherName: n || "OrbitX Launcher" }),
}));

export function selectedVersion(s: LauncherState) {
  return s.versions.find((v) => v.id === s.selectedVersionId) ?? s.versions[0];
}

export function selectedAccount(s: LauncherState) {
  return s.accounts.find((a) => a.id === s.selectedAccountId) ?? s.accounts[0];
}
