<script lang="ts">
    // @ts-ignore
    import Flex from "svelte-flex";
    import {
        modalStore,
        toastStore,
        ProgressRadial,
        RadioGroup,
        RadioItem,
    } from "@skeletonlabs/skeleton";
    import Icon from "@iconify/svelte";
    import type { ModalSettings, ToastSettings } from "@skeletonlabs/skeleton";
    import { onDestroy } from "svelte";
    import { Command } from "@tauri-apps/api/shell";
    import type { PageData } from "./$types";
    import { invoke } from "@tauri-apps/api/tauri";
    import { Body, ResponseType, getClient } from "@tauri-apps/api/http";

    export let data: PageData;

    let downloadProgress: number = 0;
    let downloadSpeed: string = "";
    let status: string = "idle";
    let inst = false;
    let instFolder = "";
    let instSize = "0 Bytes";
    let isDownloading = false;
    let enableAdminSettings = false;
    let lastVersionUpdate: string | null = null;

    $: installed = inst;
    $: installFolder = instFolder;
    $: instSize = instSize;
    $: downloading = isDownloading;
    $: data.selectedVersion && loadAppInfo(data.app.name, data.selectedVersion);

    let downloadCommand: Command;

    interface ProgressUpdate {
        status: string;
        progress: number;
        speed: string;
    }

    function updateProgress() {
        const jString = sessionStorage.getItem(
            data.app.name + "-" + data.selectedVersion + "-progress"
        );
        if (jString != null) {
            const update: ProgressUpdate = JSON.parse(jString);
            downloadProgress = update.progress;
            downloadSpeed = update.speed;
            status = update.status;

            if (update.status == "done") {
                localStorage.removeItem(
                    data.app.name + "-" + data.selectedVersion + "-progress"
                );
            }
        } else {
            downloadProgress = 0;
            downloadSpeed = "";
            status = "idle";
        }
    }

    async function installApp() {
        const address = localStorage.getItem("saved-address");
        const password = localStorage.getItem("server-password");
        const installFolder = localStorage.getItem("install-folder");

        if (address != null && password != null && installFolder != null) {
            const url =
                "http://" +
                address +
                ":8765/" +
                data.app.name +
                "/" +
                data.selectedVersion +
                ".app";
            console.log(url);

            downloadCommand = Command.sidecar("../svr/build/gl-downloader", [
                url,
                installFolder +
                    "/" +
                    data.app.name +
                    "-" +
                    data.selectedVersion,
                password,
            ]);

            isDownloading = true;

            // Print error to debug console
            downloadCommand.on("error", (error) =>
                console.error(`command error: "${error}"`)
            );
            downloadCommand.stderr.on("data", (line) =>
                console.log(`command stderr: "${line}"`)
            );

            // On progress update
            downloadCommand.stdout.on("data", (line) => {
                sessionStorage.setItem(
                    data.app.name + "-" + data.selectedVersion + "-progress",
                    line
                );
            });

            await downloadCommand.execute();
            downloadCommand.removeAllListeners();
        }
    }

    function removeAppPrompt() {
        const modal: ModalSettings = {
            type: "confirm",
            title: "Please Confirm",
            body: "Are you sure you wish to remove the app?",
            // TRUE if confirm pressed, FALSE if cancel pressed
            response: (r: boolean) => {
                if (r) removeApp();
            },
        };
        modalStore.trigger(modal);
    }

    async function removeApp() {
        if (localStorage.getItem("install-folder") != null) {
            await invoke("remove_dir", {
                dir:
                    localStorage.getItem("install-folder") +
                    "/" +
                    data.app.name +
                    "-" +
                    data.selectedVersion,
            });

            isDownloading = false;
            downloadProgress = 0;
            downloadSpeed = "";

            const msg: ToastSettings = {
                message:
                    parseName(data.app.name) + " was deleted from harddrive",
                hideDismiss: false,
                timeout: 5000,
                background: "variant-filled-primary",
            };
            toastStore.trigger(msg);
        }
    }

    function parseName(inp: string): string {
        return inp.replaceAll("_", " ");
    }

    async function checkDirSize() {
        if (inst) {
            await invoke("get_dir_size", {
                dir: instFolder,
            }).then((val) => {
                instSize = formatBytes(val);
            });
        }
    }

    async function checkIfInstalled() {
        if (localStorage.getItem("install-folder") != null) {
            instFolder =
                localStorage.getItem("install-folder") +
                "/" +
                data.app.name +
                "-" +
                data.selectedVersion;
            await invoke("check_dir_exists", {
                dir: instFolder,
            }).then((val) => {
                inst = !!val;
            });
        } else inst = false;
    }

    async function openFolder() {
        instFolder =
            localStorage.getItem("install-folder") +
            "/" +
            data.app.name +
            "-" +
            data.selectedVersion +
            "/";
        console.log(instFolder);
        await invoke("open_path", {
            path: instFolder,
        });
    }

    function formatBytes(bytes: any, decimals = 2) {
        if (!+bytes) return "0 Bytes";

        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = [
            "Bytes",
            "KiB",
            "MiB",
            "GiB",
            "TiB",
            "PiB",
            "EiB",
            "ZiB",
            "YiB",
        ];

        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${
            sizes[i]
        }`;
    }

    async function loadAppInfo(appName: string, version: string) {
        if (lastVersionUpdate != version) {
            const address = localStorage.getItem("saved-address");
            const client = await getClient();
            const reqHeaders: Record<string, any> = {
                pw: localStorage.getItem("server-password"),
            };
            const res = await client.get<AppInfo>(
                "http://" +
                    address +
                    ":" +
                    8765 +
                    "/" +
                    appName +
                    "/" +
                    version +
                    ".app.json",
                {
                    responseType: ResponseType.JSON,
                    headers: reqHeaders,
                }
            );

            console.log(res);

            if (res.status == 200) data.info = res.data;
            else data.info = { exec: "" };
            lastVersionUpdate = version;
        }
    }

    function uploadInfo() {
        const passModal: ModalSettings = {
            type: "prompt",
            title: "Enter upload values",
            body: "Type in the upload password below",
            value: "",
            valueAttr: { type: "password", required: true },

            response: async (r: string) => {
                if (r != "") {
                    const info: AppInfo = data.info;
                    const reqHeaders = {
                        pw: r,
                        "app-name": data.app.name,
                        "app-version": data.selectedVersion,
                    };

                    const address = localStorage.getItem("saved-address");
                    const client = await getClient();
                    const res = await client.post(
                        "http://" + address + ":" + 8765 + "/upload-info",
                        Body.json(info),
                        {
                            headers: reqHeaders,
                            responseType: ResponseType.Text,
                        }
                    );

                    if (res.status == 200) {
                        const errMsg: ToastSettings = {
                            message: "Upload accepted",
                            timeout: 5000,
                            background: "variant-filled-primary",
                        };
                        toastStore.trigger(errMsg);
                    } else {
                        const errMsg: ToastSettings = {
                            message: "Upload failed",
                            timeout: 5000,
                            background: "variant-filled-error",
                        };
                        toastStore.trigger(errMsg);
                    }
                }
            },
        };

        modalStore.trigger(passModal);
    }

    const progressUpdate = setInterval(async function () {
        updateProgress();
        await checkIfInstalled();
    }, 100);

    const dirSizeUpdate = setInterval(async function () {
        await checkDirSize();
    }, 10000);

    onDestroy(() => {
        clearInterval(progressUpdate);
        clearInterval(dirSizeUpdate);
    });
</script>

<h1 class="h1 mb-12">{parseName(data.app.name)}</h1>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5">version</h5>
        <select class="select w-2/3" bind:value={data.selectedVersion}>
            {#each data.app.versions as version}
                <option value={version}>{version}</option>
            {/each}
        </select>
    </Flex>
</div>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5">path</h5>
        <p class="text-xs text-slate-400">{installFolder}</p>
    </Flex>
</div>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5">size on disk</h5>
        <p class="text-xs text-slate-400">{instSize}</p>
    </Flex>
</div>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5">admin settings</h5>
        <RadioGroup>
            <RadioItem
                bind:group={enableAdminSettings}
                name="justify"
                value={false}
                ><Icon icon="radix-icons:cross-1" width="14" />
            </RadioItem>
            <RadioItem
                bind:group={enableAdminSettings}
                name="justify"
                value={true}
                ><Icon icon="carbon:checkmark" width="14" /></RadioItem
            >
        </RadioGroup>
    </Flex>
</div>

{#if enableAdminSettings}
    <div class="my-2">
        <Flex justify="between">
            <h5 class="h5">executable location</h5>
            <input
                class="input w-2/3 text-right"
                type="text"
                placeholder="executable location"
                bind:value={data.info.exec}
            />
        </Flex>
    </div>
    <div class="my-2">
        <Flex justify="between">
            <div />
            <button
                type="button"
                class="btn variant-filled-primary"
                on:click={uploadInfo}>Upload changes</button
            >
        </Flex>
    </div>
{/if}

<div class="my-2 pt-8">
    <Flex justify="evenly">
        {#if installed}
            <button
                on:click={openFolder}
                type="button"
                class="relative inset-y-0 left-0 btn variant-filled"
            >
                Open folder
            </button>
            <button
                on:click={removeAppPrompt}
                type="button"
                class="relative inset-y-0 left-0 btn variant-filled-error"
            >
                Remove app
            </button>
        {:else if downloading}
            <button
                type="button"
                class="relative inset-y-0 left-0 btn variant-filled-surface"
            >
                {status}
            </button>
        {:else}
            <button
                on:click={installApp}
                type="button"
                class="relative inset-y-0 left-0 btn variant-filled-primary"
            >
                Install app
            </button>
        {/if}
        {#if !installed}
            <div class="w-1/2">
                <Flex justify="between">
                    <p class="text-xs text-slate-400">
                        Download speed: {downloadSpeed}
                    </p>
                    <ProgressRadial value={downloadProgress} width="w-20"
                        >{downloadProgress.toFixed(2)}%</ProgressRadial
                    >
                </Flex>
            </div>
        {/if}
    </Flex>
</div>
