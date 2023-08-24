<script lang="ts">
    // @ts-ignore
    import Flex from "svelte-flex";
    import { ProgressRadial } from "@skeletonlabs/skeleton";
    import { onDestroy } from "svelte";
    import { Command } from "@tauri-apps/api/shell";
    import type { PageData } from "./$types";

    export let data: PageData;

    let downloadProgress: number = 0;
    let status: string = "idle";

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
                localStorage.setItem(
                    data.app.name + "-installed-version",
                    data.selectedVersion
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
                installFolder,
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

    async function removeApp() {
        //TODO
    }

    function parseName(inp: string): string {
        return inp.replaceAll("_", " ");
    }

    function checkIfInstalled(): boolean {
        if (
            localStorage.getItem(data.app.name + "-installed-version") != null
        ) {
            return (
                localStorage.getItem(data.app.name + "-installed-version") ==
                data.selectedVersion
            );
        }

        return false;
    }

    const progressUpdate = setInterval(function () {
        updateProgress();
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
        {#if checkIfInstalled()}
            <button
                on:click={removeApp}
                type="button"
                class="relative inset-y-0 left-0 btn variant-filled-primary"
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
