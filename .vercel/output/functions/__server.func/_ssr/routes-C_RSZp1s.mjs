import { i as __toESM } from "../_runtime.mjs";
import { L as require_react, v as require_jsx_runtime } from "../_libs/@tanstack/react-router+[...].mjs";
import { a as Trash2, c as Plus, d as Gamepad2, f as Download, h as ArrowLeft, l as Play, m as Blocks, n as Wrench, o as Share2, p as Check, r as User, s as Settings, t as X, u as House } from "../_libs/lucide-react.mjs";
import { t as clsx } from "../_libs/clsx.mjs";
import { t as twMerge } from "../_libs/tailwind-merge.mjs";
import { t as create } from "../_libs/zustand.mjs";
//#region node_modules/.nitro/vite/services/ssr/assets/routes-C_RSZp1s.js
var import_react = /* @__PURE__ */ __toESM(require_react());
var import_jsx_runtime = require_jsx_runtime();
function cn(...inputs) {
	return twMerge(clsx(inputs));
}
var SEED_VERSIONS = [
	{
		id: "1.21.4",
		name: "1.21.4",
		mc: "1.21.4",
		loader: "Vanilla",
		lastPlayed: "Today",
		installed: true
	},
	{
		id: "1.21.1-fabric",
		name: "1.21.1-Fabric",
		mc: "1.21.1",
		loader: "Fabric",
		loaderVer: "0.16.9",
		lastPlayed: "Yesterday",
		installed: true
	},
	{
		id: "1.20.1-forge",
		name: "1.20.1-Forge",
		mc: "1.20.1",
		loader: "Forge",
		loaderVer: "47.3.0",
		lastPlayed: "3 days ago",
		installed: true
	},
	{
		id: "1.16.5-forge",
		name: "1.16.5-Forge",
		mc: "1.16.5",
		loader: "Forge",
		loaderVer: "36.2.39",
		installed: true
	},
	{
		id: "1.12.2-forge",
		name: "1.12.2-Forge",
		mc: "1.12.2",
		loader: "Forge",
		loaderVer: "14.23.5.2860",
		installed: true
	}
];
var SEED_ACCOUNTS = [{
	id: "orbit",
	name: "Orbit",
	type: "Offline"
}];
var SEED_LAYOUTS = [
	{
		id: "default",
		name: "OrbitX Default",
		author: "OrbitX",
		buttons: 18
	},
	{
		id: "survival",
		name: "Survival Compact",
		author: "VenZ9",
		buttons: 12
	},
	{
		id: "pvp",
		name: "PvP Split",
		author: "community",
		buttons: 24
	}
];
var BOOT_LINES = [
	"OrbitX Launcher 1.3.3.3",
	"applicationId = com.orbitx.launcher",
	"Resolving Java runtime…",
	"Using JRE 21  (/OrbitX/runtime/java/jre21)",
	"Renderer: Zink (OpenGL 4.6)",
	"Unpacking natives (arm64-v8a)",
	"authlib-injector skipped (offline)",
	"Game directory: /OrbitX/.minecraft",
	"Launching net.minecraft.client.main.Main"
];
var CATALOG = [
	{
		id: "1.21.8",
		mc: "1.21.8",
		type: "release",
		date: "2025.07.17"
	},
	{
		id: "1.21.4",
		mc: "1.21.4",
		type: "release",
		date: "2024.12.03"
	},
	{
		id: "1.21.1",
		mc: "1.21.1",
		type: "release",
		date: "2024.08.08"
	},
	{
		id: "1.20.1",
		mc: "1.20.1",
		type: "release",
		date: "2023.06.07"
	},
	{
		id: "1.19.2",
		mc: "1.19.2",
		type: "release",
		date: "2022.08.05"
	},
	{
		id: "1.18.2",
		mc: "1.18.2",
		type: "release",
		date: "2022.02.28"
	},
	{
		id: "1.16.5",
		mc: "1.16.5",
		type: "release",
		date: "2021.01.15"
	},
	{
		id: "1.12.2",
		mc: "1.12.2",
		type: "release",
		date: "2017.09.18"
	},
	{
		id: "1.8.9",
		mc: "1.8.9",
		type: "release",
		date: "2015.12.09"
	},
	{
		id: "25w31a",
		mc: "25w31a",
		type: "snapshot",
		date: "2025.07.30"
	}
];
var useLauncher = create()((set, get) => ({
	nav: "home",
	setNav: (nav) => set({ nav }),
	versions: SEED_VERSIONS,
	selectedVersionId: "1.21.4",
	selectVersion: (id) => set({
		selectedVersionId: id,
		nav: "home"
	}),
	installVersion: (v) => {
		if (get().versions.some((x) => x.id === v.id)) {
			set({
				selectedVersionId: v.id,
				nav: "home"
			});
			return;
		}
		set({
			versions: [v, ...get().versions],
			selectedVersionId: v.id,
			nav: "home"
		});
	},
	removeVersion: (id) => {
		const next = get().versions.filter((v) => v.id !== id);
		set({
			versions: next,
			selectedVersionId: get().selectedVersionId === id ? next[0]?.id ?? "" : get().selectedVersionId
		});
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
			accounts: [...get().accounts, {
				id,
				name: trimmed,
				type: "Offline"
			}],
			selectedAccountId: id
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
		set({
			launching: true,
			launchLog: [],
			launchPhase: "boot"
		});
		BOOT_LINES.forEach((line, i) => {
			window.setTimeout(() => {
				set({ launchLog: [...get().launchLog, line] });
				if (i === BOOT_LINES.length - 1) window.setTimeout(() => set({ launchPhase: "blocked" }), 700);
			}, 280 * (i + 1));
		});
	},
	clearLaunch: () => set({
		launching: false,
		launchLog: [],
		launchPhase: "idle"
	}),
	ramMb: 2048,
	setRamMb: (n) => set({ ramMb: n }),
	renderer: "Zink (OpenGL 4.6)",
	setRenderer: (r) => set({ renderer: r }),
	launcherName: "OrbitX Launcher",
	setLauncherName: (n) => set({ launcherName: n || "OrbitX Launcher" })
}));
function selectedVersion(s) {
	return s.versions.find((v) => v.id === s.selectedVersionId) ?? s.versions[0];
}
function selectedAccount(s) {
	return s.accounts.find((a) => a.id === s.selectedAccountId) ?? s.accounts[0];
}
function OrbitMark({ className, title = "OrbitX" }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("img", {
		src: "/orbitx-icon.png",
		alt: title,
		className: cn("rounded-[22%] object-cover", className)
	});
}
function PlayerPreview({ name }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "relative mx-auto flex h-full min-h-64 w-full max-w-sm items-end justify-center",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { className: "pointer-events-none absolute inset-x-8 bottom-8 h-16 rounded-[100%] bg-bg/70 blur-xl" }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "player-bob relative flex flex-col items-center",
			children: [
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					className: "orbit-ring absolute top-[38%] h-44 w-44 rounded-full border-[14px] border-accent/90",
					style: { transform: "rotateX(68deg)" },
					"aria-hidden": true
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)(VoxelPlayer, {}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
					className: "relative z-10 mt-3 font-display text-xl font-semibold tracking-wide text-fg drop-shadow",
					children: name
				})
			]
		})]
	});
}
function VoxelPlayer() {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "relative z-10 flex flex-col items-center",
		"aria-hidden": true,
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Box, {
				className: "h-11 w-11 bg-skin shadow-inner",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { className: "absolute top-[42%] left-[22%] h-2 w-2 bg-bg" }),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { className: "absolute top-[42%] right-[22%] h-2 w-2 bg-bg" }),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { className: "absolute bottom-[18%] left-1/2 h-1 w-4 -translate-x-1/2 bg-accent-dark/80" }),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { className: "absolute inset-x-0 top-0 h-3 bg-hair" })
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Box, {
				className: "mt-0.5 h-16 w-12 bg-accent",
				children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { className: "absolute inset-x-2 top-3 h-8 bg-accent-dark/40" })
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "flex gap-1",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Box, { className: "h-14 w-5 bg-pants" }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Box, { className: "h-14 w-5 bg-pants-2" })]
			})
		]
	});
}
function Box({ className, children }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: cn("relative overflow-hidden rounded-[2px] shadow-[4px_6px_0_rgba(0,0,0,0.35)]", className),
		children
	});
}
var NAV = [
	{
		id: "home",
		label: "Home",
		icon: House
	},
	{
		id: "manage",
		label: "Manage",
		icon: Wrench
	},
	{
		id: "download",
		label: "Download",
		icon: Download
	},
	{
		id: "controller",
		label: "Controller",
		icon: Gamepad2
	},
	{
		id: "multiplayer",
		label: "Multiplayer",
		icon: Share2
	},
	{
		id: "settings",
		label: "Settings",
		icon: Settings
	}
];
function LauncherShell() {
	const nav = useLauncher((s) => s.nav);
	const launching = useLauncher((s) => s.launching);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "relative min-h-dvh overflow-hidden bg-bg text-fg",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "pointer-events-none absolute inset-0 bg-cover bg-center",
				style: { backgroundImage: "url(/launcher-bg.jpg)" }
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { className: "pointer-events-none absolute inset-0 bg-linear-to-br from-bg/55 via-bg/25 to-accent-dark/45" }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "relative z-10 mx-auto flex min-h-dvh max-w-[1400px] flex-col md:flex-row",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(NavRail, {}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("main", {
						className: "flex min-h-0 min-w-0 flex-1 flex-col px-3 py-3 md:px-4 md:py-4",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(HeaderBar, {}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
							className: "mt-3 min-h-0 flex-1 overflow-y-auto",
							children: [
								nav === "home" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(HomeView, {}),
								nav === "manage" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ManageView, {}),
								nav === "download" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(DownloadView, {}),
								nav === "controller" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(ControllerView, {}),
								nav === "multiplayer" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(MultiplayerView, {}),
								nav === "settings" && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(SettingsView, {})
							]
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(RightDock, {})
				]
			}),
			launching && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(LaunchOverlay, {})
		]
	});
}
function NavRail() {
	const nav = useLauncher((s) => s.nav);
	const setNav = useLauncher((s) => s.setNav);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("nav", {
		className: "flex shrink-0 items-center gap-1 overflow-x-auto border-border/60 bg-surface/80 px-2 py-2 backdrop-blur-md md:w-[72px] md:flex-col md:overflow-visible md:border-r md:py-5",
		"aria-label": "Launcher",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)(OrbitMark, { className: "hidden size-11 md:mb-4 md:block" }),
			NAV.map((item) => {
				const Icon = item.icon;
				const active = nav === item.id;
				return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
					type: "button",
					onClick: () => setNav(item.id),
					title: item.label,
					className: cn("flex size-11 shrink-0 items-center justify-center rounded-md transition-colors duration-150", active ? "bg-accent text-fg" : "text-muted hover:bg-surface-2 hover:text-fg"),
					"aria-current": active ? "page" : void 0,
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Icon, {
						className: "size-5",
						strokeWidth: 1.75
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
						className: "sr-only",
						children: item.label
					})]
				}, item.id);
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
				type: "button",
				onClick: () => setNav("home"),
				className: "ml-auto flex size-11 items-center justify-center rounded-md text-muted hover:bg-surface-2 hover:text-fg md:mt-auto md:ml-0",
				title: "Back",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(ArrowLeft, {
					className: "size-5",
					strokeWidth: 1.75
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
					className: "sr-only",
					children: "Back"
				})]
			})
		]
	});
}
function HeaderBar() {
	const name = useLauncher((s) => s.launcherName);
	const nav = useLauncher((s) => s.nav);
	const label = NAV.find((n) => n.id === nav)?.label ?? "Home";
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "flex items-center gap-3",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(OrbitMark, { className: "size-9 md:hidden" }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "min-w-0",
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
				className: "font-display text-[1.65rem] leading-none font-semibold tracking-wide text-balance",
				children: [
					name,
					" ",
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
						className: "text-muted",
						children: "1.3.3.3"
					})
				]
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "mt-1 text-xs tracking-wide text-subtle uppercase",
				children: label
			})]
		})]
	});
}
function Panel({ children, className }) {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: cn("rounded-xl border border-border/80 bg-surface/78 p-4 shadow-[0_12px_40px_rgba(0,0,0,0.28)] backdrop-blur-md", className),
		children
	});
}
function HomeView() {
	const account = useLauncher(selectedAccount);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "grid h-full gap-4 lg:grid-cols-[minmax(0,0.42fr)_minmax(0,0.58fr)]",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Panel, {
			className: "flex flex-col",
			children: [
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
					className: "text-center font-display text-lg font-semibold tracking-wide",
					children: "Notice"
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { className: "my-3 h-px bg-border" }),
				/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "min-h-0 flex-1 space-y-3 overflow-y-auto text-pretty text-sm leading-relaxed text-muted",
					children: [
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", { children: "OrbitX Launcher is a rebrand of Fold Craft Launcher — same Java Edition stack on Android, new name, crimson orbit mark, and a darker UI." }),
						/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", { children: [
							"Full versions, Forge / Fabric / Quilt / NeoForge, custom controls, shaders, and LAN via Terracotta are still here. Game files now live in ",
							/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
								className: "font-mono text-fg",
								children: "/OrbitX/.minecraft"
							}),
							"."
						] }),
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", { children: "This preview is the launcher chrome. Build the APK from the OrbitX-Launcher repo to run the game on a device." })
					]
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", { className: "my-3 h-px bg-border" }),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
					className: "text-center text-xs text-subtle",
					children: "2026.09.19"
				})
			]
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
			className: "hidden min-h-72 lg:block",
			children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(PlayerPreview, { name: account?.name ?? "Orbit" })
		})]
	});
}
function ManageView() {
	const versions = useLauncher((s) => s.versions);
	const selectedId = useLauncher((s) => s.selectedVersionId);
	const selectVersion = useLauncher((s) => s.selectVersion);
	const removeVersion = useLauncher((s) => s.removeVersion);
	const setNav = useLauncher((s) => s.setNav);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "space-y-3",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "flex items-end justify-between gap-3",
			children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
				className: "font-display text-2xl font-semibold tracking-wide",
				children: "Versions"
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "text-sm text-muted",
				children: "Installed profiles on this device"
			})] }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
				type: "button",
				onClick: () => setNav("download"),
				className: "inline-flex h-11 items-center gap-2 rounded-md bg-accent px-4 text-sm font-medium text-fg",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Plus, { className: "size-4" }), "Install"]
			})]
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
			className: "space-y-2",
			children: versions.map((v) => {
				const active = v.id === selectedId;
				return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: cn("flex items-center gap-3 rounded-lg border px-3 py-3 transition-colors", active ? "border-accent bg-accent/15" : "border-border bg-surface/70 hover:border-accent/50"),
					children: [
						/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
							type: "button",
							onClick: () => selectVersion(v.id),
							className: "flex min-w-0 flex-1 items-center gap-3 text-left",
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
								className: "flex size-10 items-center justify-center rounded-sm bg-surface-2 text-accent",
								children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Blocks, { className: "size-5" })
							}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
								className: "min-w-0",
								children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
									className: "block truncate font-medium",
									children: v.name
								}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
									className: "block text-xs text-subtle",
									children: [
										v.mc,
										v.loaderVer ? ` · ${v.loader} ${v.loaderVer}` : ` · ${v.loader}`,
										v.lastPlayed ? ` · ${v.lastPlayed}` : ""
									]
								})]
							})]
						}),
						active && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Check, { className: "size-4 shrink-0 text-accent-soft" }),
						/* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
							type: "button",
							onClick: () => removeVersion(v.id),
							className: "flex size-10 items-center justify-center rounded-md text-subtle hover:bg-surface-2 hover:text-fg",
							title: "Remove",
							children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(Trash2, { className: "size-4" })
						})
					]
				}) }, v.id);
			})
		})]
	});
}
function DownloadView() {
	const [loader, setLoader] = (0, import_react.useState)("Vanilla");
	const [busy, setBusy] = (0, import_react.useState)(null);
	const installVersion = useLauncher((s) => s.installVersion);
	const versions = useLauncher((s) => s.versions);
	const loaders = [
		"Vanilla",
		"Fabric",
		"Forge",
		"Quilt",
		"NeoForge"
	];
	function install(mc, type) {
		const id = loader === "Vanilla" ? mc : `${mc}-${loader.toLowerCase()}`;
		setBusy(id);
		window.setTimeout(() => {
			installVersion({
				id,
				name: loader === "Vanilla" ? mc : `${mc}-${loader}`,
				mc,
				loader,
				loaderVer: loader === "Vanilla" ? void 0 : "latest",
				installed: true,
				lastPlayed: type === "snapshot" ? "Snapshot" : "Just now"
			});
			setBusy(null);
		}, 900);
	}
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "space-y-4",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
				className: "font-display text-2xl font-semibold tracking-wide",
				children: "Download"
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "text-sm text-muted",
				children: "Install a Minecraft version. Loaders apply at install time."
			})] }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "flex flex-wrap gap-2",
				children: loaders.map((l) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
					type: "button",
					onClick: () => setLoader(l),
					className: cn("h-10 rounded-md px-3 text-sm font-medium", loader === l ? "bg-accent text-fg" : "bg-surface-2 text-muted hover:text-fg"),
					children: l
				}, l))
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
				className: "divide-y divide-border overflow-hidden rounded-lg border border-border bg-surface/70",
				children: CATALOG.map((item) => {
					const id = loader === "Vanilla" ? item.mc : `${item.mc}-${loader.toLowerCase()}`;
					const have = versions.some((v) => v.id === id);
					return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("li", {
						className: "flex items-center gap-3 px-3 py-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
							className: "min-w-0 flex-1",
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
								className: "font-medium",
								children: item.mc
							}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
								className: "text-xs text-subtle",
								children: [
									item.type,
									" · ",
									item.date
								]
							})]
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
							type: "button",
							disabled: have || busy === id,
							onClick: () => install(item.mc, item.type),
							className: cn("h-10 min-w-24 rounded-md px-3 text-sm font-medium", have ? "bg-surface-2 text-subtle" : "bg-accent text-fg disabled:opacity-60"),
							children: have ? "Installed" : busy === id ? "Installing…" : "Install"
						})]
					}, item.id + loader);
				})
			})
		]
	});
}
function ControllerView() {
	const layouts = useLauncher((s) => s.layouts);
	const selected = useLauncher((s) => s.selectedLayoutId);
	const selectLayout = useLauncher((s) => s.selectLayout);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "space-y-4",
		children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
			className: "font-display text-2xl font-semibold tracking-wide",
			children: "Control layouts"
		}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
			className: "text-sm text-muted",
			children: "Touch mappings used in-game. Compatible with ZalithLauncher2 import."
		})] }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
			className: "grid gap-3 sm:grid-cols-2",
			children: layouts.map((l) => {
				const active = l.id === selected;
				return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
					type: "button",
					onClick: () => selectLayout(l.id),
					className: cn("flex h-full w-full flex-col items-start rounded-lg border p-4 text-left", active ? "border-accent bg-accent/15" : "border-border bg-surface/70 hover:border-accent/50"),
					children: [
						/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
							className: "flex items-center gap-2 font-medium",
							children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Gamepad2, { className: "size-4 text-accent-soft" }), l.name]
						}),
						/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
							className: "mt-2 text-sm text-muted",
							children: [
								l.author,
								" · ",
								l.buttons,
								" buttons"
							]
						}),
						active && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
							className: "mt-3 text-xs tracking-wide text-accent-soft uppercase",
							children: "Active"
						})
					]
				}) }, l.id);
			})
		})]
	});
}
function MultiplayerView() {
	const [mode, setMode] = (0, import_react.useState)("host");
	const [code] = (0, import_react.useState)("ORBIT-7K2Q");
	const [join, setJoin] = (0, import_react.useState)("");
	const [status, setStatus] = (0, import_react.useState)("");
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "mx-auto max-w-xl space-y-4",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
				className: "font-display text-2xl font-semibold tracking-wide",
				children: "Multiplayer"
			}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "text-sm text-muted",
				children: "Terracotta mesh — play LAN across the internet, including offline accounts."
			})] }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
				className: "flex gap-2",
				children: ["host", "guest"].map((m) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
					type: "button",
					onClick: () => {
						setMode(m);
						setStatus("");
					},
					className: cn("h-11 flex-1 rounded-md text-sm font-medium capitalize", mode === m ? "bg-accent text-fg" : "bg-surface-2 text-muted"),
					children: m
				}, m))
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Panel, { children: [mode === "host" ? /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "space-y-3",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "text-sm text-muted",
						children: "Open to LAN in-game, then share this invite code."
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "font-mono text-2xl tracking-[0.2em] text-fg",
						children: code
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
						type: "button",
						onClick: () => {
							navigator.clipboard?.writeText(code);
							setStatus("Invite code copied");
						},
						className: "h-11 w-full rounded-md bg-accent text-sm font-medium",
						children: "Copy invite code"
					})
				]
			}) : /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("form", {
				className: "space-y-3",
				onSubmit: (e) => {
					e.preventDefault();
					setStatus(join.trim() ? `Joining ${join.trim().toUpperCase()}… grant VPN permission on device.` : "Enter a host invite code");
				},
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("label", {
					className: "block text-sm text-muted",
					children: ["Invite code", /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
						value: join,
						onChange: (e) => setJoin(e.target.value),
						className: "mt-1 h-11 w-full rounded-md border border-border bg-bg px-3 text-fg outline-none focus:border-accent",
						placeholder: "ORBIT-XXXX"
					})]
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
					type: "submit",
					className: "h-11 w-full rounded-md bg-accent text-sm font-medium",
					children: "Join session"
				})]
			}), status && /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
				className: "mt-3 text-sm text-accent-soft",
				children: status
			})] })
		]
	});
}
function SettingsView() {
	const ram = useLauncher((s) => s.ramMb);
	const setRam = useLauncher((s) => s.setRamMb);
	const renderer = useLauncher((s) => s.renderer);
	const setRenderer = useLauncher((s) => s.setRenderer);
	const launcherName = useLauncher((s) => s.launcherName);
	const setLauncherName = useLauncher((s) => s.setLauncherName);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "mx-auto max-w-2xl space-y-4",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("h2", {
				className: "font-display text-2xl font-semibold tracking-wide",
				children: "Launcher settings"
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Panel, {
				className: "space-y-5",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("label", {
						className: "block",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
							className: "text-sm text-muted",
							children: "Custom launcher name"
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
							value: launcherName,
							onChange: (e) => setLauncherName(e.target.value),
							className: "mt-1 h-11 w-full rounded-md border border-border bg-bg px-3 outline-none focus:border-accent"
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "flex justify-between text-sm",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
							className: "text-muted",
							children: "Memory"
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
							className: "font-mono tabular-nums",
							children: [ram, " MB"]
						})]
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
						type: "range",
						min: 1024,
						max: 8192,
						step: 256,
						value: ram,
						onChange: (e) => setRam(Number(e.target.value)),
						className: "mt-2 w-full accent-accent"
					})] }),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("label", {
						className: "block",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
							className: "text-sm text-muted",
							children: "Renderer"
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("select", {
							value: renderer,
							onChange: (e) => setRenderer(e.target.value),
							className: "mt-1 h-11 w-full rounded-md border border-border bg-bg px-3 outline-none focus:border-accent",
							children: [
								"Zink (OpenGL 4.6)",
								"VirGL (OpenGL 4.3)",
								"MobileGlues",
								"GL4ES"
							].map((r) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("option", { children: r }, r))
						})]
					})
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)(Panel, {
				className: "space-y-2 text-sm leading-relaxed text-muted",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "font-display text-lg font-semibold tracking-wide text-fg",
						children: "About"
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", { children: "OrbitX Launcher is a GPL-3.0 fork of Fold Craft Launcher (FCL-Team). Maintained by VenZ9. Core from HMCL; runtime from Amethyst-Android / PojavLauncher and Boat." }),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "font-mono text-xs text-subtle",
						children: "com.orbitx.launcher · theme #C0392B · /OrbitX/.minecraft"
					})
				]
			})
		]
	});
}
function RightDock() {
	const account = useLauncher(selectedAccount);
	const version = useLauncher(selectedVersion);
	const setNav = useLauncher((s) => s.setNav);
	const startLaunch = useLauncher((s) => s.startLaunch);
	const [accountOpen, setAccountOpen] = (0, import_react.useState)(false);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("aside", {
		className: "relative flex w-full shrink-0 flex-col border-t border-border/60 bg-surface/80 px-4 py-4 backdrop-blur-md md:w-[260px] md:border-t-0 md:border-l",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
				type: "button",
				onClick: () => setAccountOpen((v) => !v),
				className: "mx-auto flex flex-col items-center gap-1 py-2",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
						className: "flex size-14 items-center justify-center rounded-full bg-accent/20 text-accent-soft ring-2 ring-accent/40",
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(User, { className: "size-7" })
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
						className: "font-display text-lg font-semibold tracking-wide",
						children: account?.name ?? "No account"
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
						className: "text-xs text-subtle",
						children: account ? `${account.type} · tap to switch` : "Add account"
					})
				]
			}),
			accountOpen && /* @__PURE__ */ (0, import_jsx_runtime.jsx)(AccountMenu, { onClose: () => setAccountOpen(false) }),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
				type: "button",
				onClick: () => setNav("manage"),
				className: "mt-auto flex items-center gap-3 rounded-lg border border-border bg-surface-2/80 px-3 py-3 text-left",
				children: [
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Blocks, { className: "size-7 shrink-0 text-accent-soft" }),
					/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("span", {
						className: "min-w-0 flex-1",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
							className: "block truncate text-sm font-semibold",
							children: version?.name ?? "No version"
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
							className: "block truncate text-xs text-subtle",
							children: version ? `${version.mc}${version.loaderVer ? ` · ${version.loader}` : ""}` : "Install a version"
						})]
					}),
					/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Settings, { className: "size-4 text-subtle" })
				]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
				type: "button",
				onClick: startLaunch,
				disabled: !version,
				className: "mt-3 flex h-12 items-center justify-center gap-2 rounded-lg bg-accent text-base font-semibold tracking-wide text-fg shadow-[0_8px_24px_rgba(192,57,43,0.35)] transition-transform duration-150 active:scale-[0.98] disabled:opacity-50",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(Play, { className: "size-5 fill-current" }), "Launch"]
			})
		]
	});
}
function AccountMenu({ onClose }) {
	const accounts = useLauncher((s) => s.accounts);
	const selected = useLauncher((s) => s.selectedAccountId);
	const selectAccount = useLauncher((s) => s.selectAccount);
	const addOffline = useLauncher((s) => s.addOfflineAccount);
	const [name, setName] = (0, import_react.useState)("");
	return /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
		className: "absolute inset-x-3 top-36 z-20 rounded-lg border border-border bg-surface p-3 shadow-xl",
		children: [
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
				className: "mb-2 flex items-center justify-between",
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
					className: "text-sm font-medium",
					children: "Accounts"
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
					type: "button",
					onClick: onClose,
					className: "size-8 text-subtle",
					children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(X, { className: "mx-auto size-4" })
				})]
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsx)("ul", {
				className: "space-y-1",
				children: accounts.map((a) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("li", { children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("button", {
					type: "button",
					onClick: () => {
						selectAccount(a.id);
						onClose();
					},
					className: cn("flex h-10 w-full items-center justify-between rounded-md px-2 text-sm", selected === a.id ? "bg-accent/20" : "hover:bg-surface-2"),
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", { children: a.name }), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("span", {
						className: "text-xs text-subtle",
						children: a.type
					})]
				}) }, a.id))
			}),
			/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("form", {
				className: "mt-3 flex gap-2",
				onSubmit: (e) => {
					e.preventDefault();
					addOffline(name);
					setName("");
				},
				children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("input", {
					value: name,
					onChange: (e) => setName(e.target.value),
					placeholder: "Offline name",
					className: "h-10 min-w-0 flex-1 rounded-md border border-border bg-bg px-2 text-sm outline-none focus:border-accent"
				}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
					type: "submit",
					className: "h-10 rounded-md bg-accent px-3 text-sm font-medium",
					children: "Add"
				})]
			})
		]
	});
}
function LaunchOverlay() {
	const log = useLauncher((s) => s.launchLog);
	const phase = useLauncher((s) => s.launchPhase);
	const clear = useLauncher((s) => s.clearLaunch);
	const version = useLauncher(selectedVersion);
	const name = useLauncher((s) => s.launcherName);
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
		className: "fixed inset-0 z-50 flex items-center justify-center bg-bg/80 p-4 backdrop-blur-sm",
		children: /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
			className: "flex max-h-[min(640px,90dvh)] w-full max-w-2xl flex-col overflow-hidden rounded-xl border border-border bg-surface shadow-2xl",
			children: [
				/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "flex items-center justify-between border-b border-border px-4 py-3",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
						className: "flex items-center gap-3",
						children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)(OrbitMark, { className: "size-9" }), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
							className: "font-display text-lg font-semibold tracking-wide",
							children: name
						}), /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("p", {
							className: "text-xs text-subtle",
							children: ["Starting ", version?.name ?? "game"]
						})] })]
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
						type: "button",
						onClick: clear,
						className: "flex size-10 items-center justify-center rounded-md hover:bg-surface-2",
						children: /* @__PURE__ */ (0, import_jsx_runtime.jsx)(X, { className: "size-5" })
					})]
				}),
				/* @__PURE__ */ (0, import_jsx_runtime.jsx)("div", {
					className: "min-h-0 flex-1 overflow-y-auto bg-bg px-4 py-3 font-mono text-xs leading-6 text-ok",
					children: log.map((line, i) => /* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "log-line",
						children: line
					}, i))
				}),
				phase === "blocked" && /* @__PURE__ */ (0, import_jsx_runtime.jsxs)("div", {
					className: "space-y-3 border-t border-border px-4 py-4",
					children: [/* @__PURE__ */ (0, import_jsx_runtime.jsx)("p", {
						className: "text-sm text-pretty text-muted",
						children: "JVM handoff is native. Install the OrbitX APK on Android to actually enter the world — this screen is the new branded chrome."
					}), /* @__PURE__ */ (0, import_jsx_runtime.jsx)("button", {
						type: "button",
						onClick: clear,
						className: "h-11 w-full rounded-md bg-accent text-sm font-medium",
						children: "Return to launcher"
					})]
				})
			]
		})
	});
}
function Home() {
	return /* @__PURE__ */ (0, import_jsx_runtime.jsx)(LauncherShell, {});
}
//#endregion
export { Home as component };
