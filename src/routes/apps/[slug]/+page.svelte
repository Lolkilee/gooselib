<script lang="ts">
    // @ts-ignore
    import Flex from "svelte-flex";
    import {
        modalStore,
        toastStore,
        ProgressRadial,
    } from "@skeletonlabs/skeleton";
    import type { ModalSettings, ToastSettings } from "@skeletonlabs/skeleton";
    import { onDestroy } from "svelte";
    import { Command } from "@tauri-apps/api/shell";
    import type { PageData } from "./$types";
    import { invoke } from "@tauri-apps/api/tauri";

    export let data: PageData;

    let downloadProgress: number = 0;
    let status: string = "idle";
    let inst = false;

    $: installed = inst;

    interface ProgressUpdate {
        status: string;
        progress: number;
    }

    function updateProgress() {
        const jString = sessionStorage.getItem(
            data.app.name + "-" + data.selectedVersion + "-progress"
        );
        if (jString != null) {
            const update: ProgressUpdate = JSON.parse(jString);
            downloadProgress = update.progress;
            status = update.status;

            if (update.status == "done") {
                localStorage.removeItem(
                    data.app.name + "-" + data.selectedVersion + "-progress"
                );
            }
        } else {
            downloadProgress = 0;
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

            const command = Command.sidecar("../svr/build/gl-downloader", [
                url,
                installFolder +
                    "/" +
                    data.app.name +
                    "-" +
                    data.selectedVersion,
                password,
            ]);

            // Print error to debug console
            command.on("error", (error) =>
                console.error(`command error: "${error}"`)
            );
            command.stderr.on("data", (line) =>
                console.log(`command stderr: "${line}"`)
            );

            // On progress update
            command.stdout.on("data", (line) => {
                sessionStorage.setItem(
                    data.app.name + "-" + data.selectedVersion + "-progress",
                    line
                );
            });

            await command.execute();
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

    async function checkIfInstalled() {
        if (localStorage.getItem("install-folder") != null) {
            await invoke("check_dir_exists", {
                dir:
                    localStorage.getItem("install-folder") +
                    "/" +
                    data.app.name +
                    "-" +
                    data.selectedVersion,
            }).then((val) => {
                inst = !!val;
            });
        } else inst = false;
    }

    const progressUpdate = setInterval(async function () {
        updateProgress();
        await checkIfInstalled();
    }, 100);

    onDestroy(() => {
        clearInterval(progressUpdate);
    });
</script>

<h1 class="h1 mb-12">{parseName(data.app.name)}</h1>

<div class="my-2 pb-16">
    <Flex justify="between">
        <h5 class="h5">App version</h5>
        <select class="select w-2/3" bind:value={data.selectedVersion}>
            {#each data.app.versions as version}
                <option value={version}>{version}</option>
            {/each}
        </select>
    </Flex>
</div>

<div class="my-2">
    <Flex justify="evenly">
        {#if installed}
            <button
                on:click={removeAppPrompt}
                type="button"
                class="relative inset-y-0 left-0 btn variant-filled-error"
            >
                Remove app
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
        <div class="w-1/4">
            <ProgressRadial value={downloadProgress} width="w-24"
                >{downloadProgress.toFixed(2)}%</ProgressRadial
            >
        </div>
    </Flex>
</div>
